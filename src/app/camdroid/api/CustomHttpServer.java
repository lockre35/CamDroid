package app.camdroid.api;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import android.util.Log;
import app.camdroid.server.TinyHttpServer;
import app.camdroid.streaming.Session;

/**
 * 
 * HTTP server of Spydroid.
 * 
 * Its document root is assets/www, it contains a little user-friendly website to control spydroid from a browser.
 * 
 * Some commands can be sent to it by sending POST request to "/request.json".
 * See {@link RequestHandler} to find out what kind of commands can be sent.
 * 
 * Streams can also be started/stopped by sending GET request to "/spydroid.sdp".
 * The HTTP server then responds with a proper Session Description (SDP).
 * All supported options are described in {@link UriParser}
 *
 */
public class CustomHttpServer extends TinyHttpServer {

	/** A stream failed to start. */
	public final static int ERROR_START_FAILED = 0xFE;

	/** Streaming started. */
	public final static int MESSAGE_STREAMING_STARTED = 0X00;

	/** Streaming stopped. */
	public final static int MESSAGE_STREAMING_STOPPED = 0X01;

	/** Maximal number of streams that you can start from the HTTP server. **/
	protected static final int MAX_STREAM_NUM = 2;

	private DescriptionRequestHandler mDescriptionRequestHandler;
	//private WeakHashMap<Session,Object> mSessions = new WeakHashMap<Session,Object>(2);
	private Session mSession;
	
	public CustomHttpServer() {

		// HTTP is used by default for now
		mHttpEnabled = true;

	}

	@Override
	public void onCreate() {
		super.onCreate();
		mDescriptionRequestHandler = new DescriptionRequestHandler();
		addRequestHandler("/spydroid.sdp*", mDescriptionRequestHandler);

	}

	@Override
	public void stop() {
		super.stop();
		// If user has started a session with the HTTP Server, we need to stop it
		if(mDescriptionRequestHandler.mAudio.session!=null){
			boolean streaming = isStreaming();
			mDescriptionRequestHandler.mAudio.session.stop();
			if(streaming&& !isStreaming()){
				postMessage(MESSAGE_STREAMING_STOPPED);
			}
			mDescriptionRequestHandler.mAudio.session.flush();
			mDescriptionRequestHandler.mAudio.session = null;
		}
		if(mDescriptionRequestHandler.mVideo.session!=null){
			boolean streaming = isStreaming();
			mDescriptionRequestHandler.mVideo.session.stop();
			if(streaming&& !isStreaming()){
				postMessage(MESSAGE_STREAMING_STOPPED);
			}
			mDescriptionRequestHandler.mVideo.session.flush();
			mDescriptionRequestHandler.mVideo.session = null;
		}

	}

	public boolean isStreaming() {
		    if ( mSession != null ) {
		    	if (mSession.isStreaming()) return true;
		    } 
		return false;
	}

	public long getBitrate() {
		long bitrate = 0;
		    if ( mSession != null ) {
		    	if (mSession.isStreaming()) bitrate += mSession.getBitrate();
		    } 
		return bitrate;
	}
	

	/** 
	 * Allows to start streams (a session contains one or more streams) from the HTTP server by requesting 
	 * this URL: http://ip/spydroid.sdp (the RTSP server is not needed here). 
	 **/
	class DescriptionRequestHandler implements HttpRequestHandler {

		
		private SessionInfo mVideo = new SessionInfo();
		private SessionInfo mAudio = new SessionInfo();

		private class SessionInfo {
			public Session session;
			public String uri;
			public String description;
		}


		public synchronized void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException {
			Socket socket = ((TinyHttpServer.MHttpContext)context).getSocket();
			Log.d("Custom","Socket destination " + socket.getInetAddress());
			Log.d("Custom","Socket port origin " + socket.getLocalAddress());
			String uri = request.getRequestLine().getUri();
			int id = 0;
			boolean stop = false;

			try {

				// A stream id can be specified in the URI, this id is associated to a session
				List<NameValuePair> params = URLEncodedUtils.parse(URI.create(uri),"UTF-8");
				uri = "";
				if (params.size()>0) {
					for (Iterator<NameValuePair> it = params.iterator();it.hasNext();) {
						NameValuePair param = it.next();
						if (param.getName().equalsIgnoreCase("id")) {
							try {	
								id = Integer.parseInt(param.getValue());
							} catch (Exception ignore) {}
						}
						else if (param.getName().equalsIgnoreCase("stop")) {
							stop = true;
						}
					}	
				}

				params.remove("id");
				uri = "http://c?" + URLEncodedUtils.format(params, "UTF-8");

				if (!uri.equals(mAudio.uri)&&id==0) {

					mAudio.uri = uri;

					// Stops all streams if a Session already exists
					if (mAudio.session != null) {
						boolean streaming = isStreaming();
						mAudio.session.stop();
						if (streaming && !isStreaming()) {
							postMessage(MESSAGE_STREAMING_STOPPED);
						}
						mAudio.session.flush();
						mAudio.session = null;
					}

					if (!stop) {
						if (mAudio.session == null || 
								(mAudio.session != null)) 
						{
							// Parses URI and creates the Session
							mAudio.session = UriParser.parse(uri);
							mSession=mAudio.session;
						} 

						// Sets proper origin & dest
						mAudio.session.setOrigin(socket.getLocalAddress());
						if (mAudio.session.getDestination()==null) {
							mAudio.session.setDestination(socket.getInetAddress());
						}
						Log.v("Stream id: ", Integer.toString(id));
						mAudio.description = mAudio.session.getSessionDescription().replace("Unnamed", "Stream-"+id);
						
						// Starts all streams associated to the Session
						boolean streaming = isStreaming();
						mAudio.session.start();
						if (!streaming && isStreaming()) {
							postMessage(MESSAGE_STREAMING_STARTED);
						}
						
					}
				}else if (!uri.equals(mVideo.uri)&&id==1) {

					mVideo.uri = uri;

					// Stops all streams if a Session already exists
					if (mVideo.session != null) {
						boolean streaming = isStreaming();
						mVideo.session.stop();
						if (streaming && !isStreaming()) {
							postMessage(MESSAGE_STREAMING_STOPPED);
						}
						mVideo.session.flush();
						mVideo.session = null;
					}

					if (!stop) {
						if (mVideo.session == null || 
								(mVideo.session != null)) 
						{
							// Parses URI and creates the Session
							mVideo.session = UriParser.parse(uri);
							mSession=mVideo.session;
						} 

						// Sets proper origin & dest
						mVideo.session.setOrigin(socket.getLocalAddress());
						if (mVideo.session.getDestination()==null) {
							mVideo.session.setDestination(socket.getInetAddress());
						}
						Log.v("Stream id: ", Integer.toString(id));
						mVideo.description = mVideo.session.getSessionDescription().replace("Unnamed", "Stream-"+id);
						
						// Starts all streams associated to the Session
						boolean streaming = isStreaming();
						mVideo.session.start();
						if (!streaming && isStreaming()) {
							postMessage(MESSAGE_STREAMING_STARTED);
						}
						
					}
				}

				final int fid = id; final boolean fstop = stop;
				response.setStatusCode(HttpStatus.SC_OK);
				EntityTemplate body = new EntityTemplate(new ContentProducer() {
					public void writeTo(final OutputStream outstream) throws IOException {
						OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8");
						if (!fstop) {
							if(fid==1)writer.write(mVideo.description);
							else if(fid==0)writer.write(mAudio.description);
						} else {
							writer.write("STOPPED");
						}
						writer.flush();
					}
				});
				body.setContentType("application/sdp; charset=UTF-8");
				response.setEntity(body);

			} catch (Exception e) {
				mAudio.uri = "";
				mVideo.uri = "";
				response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				Log.e(TAG,e.getMessage()!=null?e.getMessage():"An unknown error occurred");
				e.printStackTrace();
				postError(e,ERROR_START_FAILED);
			}

		}

	}

}
