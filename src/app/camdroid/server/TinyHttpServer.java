package app.camdroid.server;


import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.LinkedList;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * 
 * A Service that contains a HTTP server and a HTTPS server.
 * 
 * Check out the github page of this project for further information.
 * 
 * You may add some logic to this server with {@link #addRequestHandler(String, HttpRequestHandler)}.
 * By default it serves files from /assets/www.
 * 
 */
public class TinyHttpServer extends Service {

	/** The tag used by the server. */
	public static final String TAG = "TinyHttpServer";

	/** Default port for HTTP. */
	public final static int DEFAULT_HTTP_PORT = 8080;
	
	/** Port already in use. */
	public final static int ERROR_HTTP_BIND_FAILED = 0x00;

	/** Key used in the SharedPreferences to store whether the HTTP server is enabled or not. */
	public final static String KEY_HTTP_ENABLED = "http_enabled";
	
	/** Key used in the SharedPreferences for the port used by the HTTP server. */
	public final static String KEY_HTTP_PORT = "http_port";
	
	/** 
	 * Common name that will appear in the root certificate. 
	 * You might want to extend {@link TinyHttpServer} to change that. 
	 **/
	protected String mCACommonName = "TinyHttpServer CA";

	protected String[] MODULES = new String[] {
		"ModAssetServer",
		};
	
	protected int mHttpPort = DEFAULT_HTTP_PORT;
	protected boolean mHttpEnabled = true;
	protected LinkedList<CallbackListener> mListeners = new LinkedList<CallbackListener>();
	
	private BasicHttpProcessor mHttpProcessor;
	private HttpParams mParams; 
	private HttpRequestListener mHttpRequestListener = null;
	private SharedPreferences mSharedPreferences;
	private boolean mHttpUpdate = false;

	Date mLastModified;
	HttpRequestHandlerRegistry mRegistry;
	public Context mContext;
	public Socket mSocket;
	
	/** Be careful: those callbacks won't necessarily be called from the ui thread ! */
	public interface CallbackListener {

		/** Called when an error occurs. */
		void onError(TinyHttpServer server, Exception e, int error);
		
		void onMessage(TinyHttpServer server, int message);		

	}




	
	/** 
	 * You may add some HttpRequestHandler to modify the default behavior of the server.
	 * @param pattern Patterns may have three formats: * or *<uri> or <uri>*
	 * @param handler A HttpRequestHandler
	 */ 
	protected void addRequestHandler(String pattern, HttpRequestHandler handler) {
		mRegistry.register(pattern, handler);
	}
	
	/**
	 * Sets the port for the HTTP server to use.
	 * @param port The port to use
	 */
	public void setHttpPort(int port) {
		Editor editor = mSharedPreferences.edit();
		editor.putString(KEY_HTTP_PORT, String.valueOf(port));
		editor.commit();
	}

	/** Enables the HTTP server. */
	public void setHttpEnabled(boolean enable) {
		Editor editor = mSharedPreferences.edit();
		editor.putBoolean(KEY_HTTP_ENABLED, enable);
		editor.commit();
	}

	/** Returns the port used by the HTTP server. */	
	public int getHttpPort() {
		return mHttpPort;
	}

	/** Indicates whether or not the HTTP server is enabled. */
	public boolean isHttpEnabled() {
		return mHttpEnabled;
	}

	/** Starts (or restart if needed) the HTTP server. */
	public void start() {

		// Stops the HTTP server if it has been disabled or if it needs to be restarted
		if ((!mHttpEnabled || mHttpUpdate) && mHttpRequestListener != null) {
			mHttpRequestListener.kill();
			mHttpRequestListener = null;
		}
		// Starts the HTTP server if needed
		if (mHttpEnabled && mHttpRequestListener == null) {
			try {
				mHttpRequestListener = new HttpRequestListener(mHttpPort);
			} catch (Exception e) {
				mHttpRequestListener = null;
			}
		}

		mHttpUpdate = false;

	}

	/** Stops the HTTP server and/or the HTTPS server but not the Android service. */
	public void stop() {
		if (mHttpRequestListener != null) {
			// Stops the HTTP server
			mHttpRequestListener.kill();
			mHttpRequestListener = null;
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();

		mContext = getApplicationContext();
		mRegistry = new HttpRequestHandlerRegistry();
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		mParams = new BasicHttpParams();
		mParams
		.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
		.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
		.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
		.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
		.setParameter(CoreProtocolPNames.ORIGIN_SERVER, "MajorKernelPanic HTTP Server");

		// Set up the HTTP protocol processor
		mHttpProcessor = new BasicHttpProcessor();
		mHttpProcessor.addInterceptor(new ResponseDate());
		mHttpProcessor.addInterceptor(new ResponseServer());
		mHttpProcessor.addInterceptor(new ResponseContent());
		mHttpProcessor.addInterceptor(new ResponseConnControl());

		// Will be used in the "Last-Modifed" entity-header field
		try {
			String packageName = mContext.getPackageName();
			mLastModified = new Date(mContext.getPackageManager().getPackageInfo(packageName, 0).lastUpdateTime);
		} catch (NameNotFoundException e) {
			mLastModified = new Date(0);
		}

		// Restores the state of the service 
		mHttpPort = Integer.parseInt(mSharedPreferences.getString(KEY_HTTP_PORT, String.valueOf(mHttpPort)));
		mHttpEnabled = mSharedPreferences.getBoolean(KEY_HTTP_ENABLED, mHttpEnabled);



		// Loads plugins available in the package net.majorkernelpanic.http
		for (int i=0; i<MODULES.length; i++) {
			try {
				Class<?> pluginClass = Class.forName(TinyHttpServer.class.getPackage().getName()+"."+MODULES[i]);
				Constructor<?> pluginConstructor = pluginClass.getConstructor(new Class[]{TinyHttpServer.class});
				addRequestHandler((String) pluginClass.getField("PATTERN").get(null), (HttpRequestHandler)pluginConstructor.newInstance(this));
			} catch (ClassNotFoundException ignore) {
				// Module disabled
			} catch (Exception e) {
				Log.e(TAG, "Bad module: "+MODULES[i]);
				e.printStackTrace();
			}
		}
		
		start();
		
	}

	@Override
	public void onDestroy() {
		stop();
	}
	
	@Override
	public int onStartCommand (Intent intent, int flags, int startId) {
		//Log.d(TAG,"TinyServerHttp started !");
		return START_STICKY;
	}
	


	/** The Binder you obtain when a connection with the Service is established. */
	public class LocalBinder extends Binder {
		public TinyHttpServer getService() {
			return TinyHttpServer.this;
		}
	}

	/** See {@link TinyHttpServer.LocalBinder}. */
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	private final IBinder mBinder = new LocalBinder();

	protected void postError(Exception exception, int id) {
		synchronized (mListeners) {
			if (mListeners.size() > 0) {
				for (CallbackListener cl : mListeners) {
					cl.onError(this, exception, id);
				}
			}			
		}
	}
	
	protected void postMessage(int id) {
		synchronized (mListeners) {
			if (mListeners.size() > 0) {
				for (CallbackListener cl : mListeners) {
					cl.onMessage(this, id);
				}
			}			
		}
	}	
	
	protected class HttpRequestListener extends RequestListener {

		public HttpRequestListener(final int port) throws Exception {
			try {
				ServerSocket serverSocket = new ServerSocket(port);
				construct(serverSocket);
				Log.i(TAG,"HTTP server listening on port " + serverSocket.getLocalPort());
			} catch (BindException e) {
				postError(e, ERROR_HTTP_BIND_FAILED);
				throw e;
			}
		}

		protected void kill() {
			super.kill();
			Log.i(TAG,"HTTP server stopped !");
		}

	}

	private class RequestListener extends Thread {

		private ServerSocket mServerSocket;
		private final org.apache.http.protocol.HttpService mHttpService;

		protected RequestListener() throws Exception {

			mHttpService = new org.apache.http.protocol.HttpService(
					mHttpProcessor, 
					new DefaultConnectionReuseStrategy(), 
					new DefaultHttpResponseFactory());
			mHttpService.setHandlerResolver(mRegistry);
			mHttpService.setParams(mParams);

		}

		protected void construct(ServerSocket serverSocket) {
			mServerSocket = serverSocket;
			start();
		}

		protected void kill() {
			try {
				mServerSocket.close();
			} catch (IOException ignore) {}
			try {
				this.join();
			} catch (InterruptedException ignore) {}
		}

		public void run() {
			while (!Thread.interrupted()) {
				try {
					// Set up HTTP connection
					Socket socket = this.mServerSocket.accept();
					DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
					Log.d(TAG,"Incoming connection from " + socket.getInetAddress());
					Log.d(TAG,"Socket port " + socket.getPort());
					conn.bind(socket, mParams);

					// Start worker thread
					Thread t = new WorkerThread(this.mHttpService, conn, socket);
					t.setDaemon(true);
					t.start();
				} catch (SocketException e) {
					break;
				} catch (InterruptedIOException ex) {
					Log.e(TAG,"Interrupted !");
					break;
				} catch (IOException e) {
					Log.d(TAG,"I/O error initialising connection thread: " + e.getMessage());
					break;
				}
			}
		}
	}

	static class WorkerThread extends Thread {

		private final org.apache.http.protocol.HttpService httpservice;
		private final HttpServerConnection conn;
		private final Socket socket;

		public WorkerThread(
				final org.apache.http.protocol.HttpService httpservice, 
				final HttpServerConnection conn,
				final Socket socket) {
			super();
			this.httpservice = httpservice;
			this.conn = conn;
			this.socket = socket;
		}

		public void run() {
			Log.d(TAG,"New connection thread");
			HttpContext context = new MHttpContext(socket);
			try {
				while (!Thread.interrupted() && this.conn.isOpen()) {
					try {
						this.httpservice.handleRequest(this.conn, context);
					} catch (UnsupportedOperationException e) {
						e.printStackTrace();
						// shutdownOutput is not implemented by SSLSocket, and it is called in the implementation
						// of org.apache.http.impl.SocketHttpServerConnection.close().
					}
				}
			} catch (ConnectionClosedException e) {
				Log.d(TAG,"Client closed connection");
				e.printStackTrace();
			} catch (SocketTimeoutException e) {
				Log.d(TAG,"Socket timeout");
			} catch (IOException e) {
				Log.e(TAG,"I/O error: " + e.getMessage());
			} catch (HttpException e) {
				Log.e(TAG,"Unrecoverable HTTP protocol violation: " + e.getMessage());
			} finally {
				try {
					OutputStream sockOutOStream = socket.getOutputStream();
					sockOutOStream.write(new byte[0]);
					sockOutOStream.flush();
					socket.close();
				} catch (IOException e) {
				}
				try {
					this.conn.shutdown();
				} catch (Exception ignore) {}
			}
		}
	}

	/** Little modification of BasicHttpContext to add access to the underlying tcp socket. */
	public static class MHttpContext extends BasicHttpContext {

		private Socket socket;

		public MHttpContext(Socket socket) {
			super(null);
			this.socket = socket;
		}

		/** Returns a reference to the underlying socket of the connection. */
		public Socket getSocket() {
			return socket;
		}

	}

}

