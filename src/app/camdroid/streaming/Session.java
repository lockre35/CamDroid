package app.camdroid.streaming;

import java.io.IOException;
import java.net.InetAddress;

import android.content.Context;
import android.net.wifi.WifiManager;
import app.camdroid.streaming.audio.AudioStream;
import app.camdroid.streaming.video.VideoStream;

/**
 * This class makes use of all the streaming package.
 * It represents a streaming session between a client and the phone.
 * A stream is designated by the word "track" in this class.
 * To add tracks to the session you need to call addVideoTrack() or addAudioTrack().
 */
public class Session {

	public final static String TAG = "Session";
	
	// Prevents threads from modifying two sessions simultaneously
	private static Object sLock = new Object();

	private InetAddress mOrigin;
	private InetAddress mDestination;
	private long mTimestamp;
	private Context mContext = null;
	private WifiManager.MulticastLock mLock = null;
	
	private AudioStream mAudioStream = null;
	private VideoStream mVideoStream = null;

	/** 
	 * Creates a streaming session that can be customized by adding tracks.
	 */
	public Session() {
		this(null, null);
		try {
			mOrigin = InetAddress.getLocalHost();
		} catch (Exception ignore) {
			mOrigin = null;
		}
	}

	/** 
	 * Creates a streaming session that can be customized by adding tracks.
	 * @param destination The destination address of the streams
	 * @param origin The origin address of the streams (appears in the session description)
	 */
	public Session(InetAddress origin, InetAddress destination) {
		long uptime = System.currentTimeMillis();
		mDestination = destination;
		mOrigin = origin;
		mTimestamp = (uptime/1000)<<32 & (((uptime-((uptime/1000)*1000))>>32)/1000); // NTP timestamp
	}

	public void addAudioTrack(AudioStream track) {
		mAudioStream = track;
	}
	
	public void addVideoTrack(VideoStream track) {
		mVideoStream = track;
	}
	
	public void removeAudioTrack() {
		mAudioStream = null;
	}
	
	public void removeVideoTrack() {
		mVideoStream = null;
	}

	public AudioStream getAudioTrack() {
		return mAudioStream;
	}
	
	public VideoStream getVideoTrack() {
		return mVideoStream;
	}	
		
	/** 
	 * The origin address of the session.
	 * It appears in the session description.
	 * @param origin The origin address
	 */
	public void setOrigin(InetAddress origin) {
		mOrigin = origin;
	}	

	/** 
	 * The destination address for all the streams of the session.
	 * You must stop all tracks before calling this method.
	 * @param destination The destination address
	 */
	public void setDestination(InetAddress destination) throws IllegalStateException {
		mDestination =  destination;
	}


	/** 
	 * Returns a Session Description that can be stored in a file or sent to a client with RTSP.
	 * @return The Session Description
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public String getSessionDescription() throws IllegalStateException, IOException {
		if (mDestination==null) {
			throw new IllegalStateException("setDestination() has not been called !");
		}
		synchronized (sLock) {
			StringBuilder sessionDescription = new StringBuilder();
			sessionDescription.append("v=0\r\n");
			sessionDescription.append("o=- "+mTimestamp+" "+mTimestamp+" IN IP4 "+(mOrigin==null?"127.0.0.1":mOrigin.getHostAddress())+"\r\n");
			sessionDescription.append("s=Unnamed\r\n");
			sessionDescription.append("i=N/A\r\n");
			sessionDescription.append("c=IN IP4 "+mDestination.getHostAddress()+"\r\n");
			// t=0 0 means the session is permanent (we don't know when it will stop)
			sessionDescription.append("t=0 0\r\n");
			sessionDescription.append("a=recvonly\r\n");
			// Prevents two different sessions from using the same peripheral at the same time
			if (mAudioStream != null) {
				sessionDescription.append(mAudioStream.generateSessionDescription());
				sessionDescription.append("a=control:trackID="+0+"\r\n");
			}
			if (mVideoStream != null) {
				sessionDescription.append(mVideoStream.generateSessionDescription());
				sessionDescription.append("a=control:trackID="+1+"\r\n");
			}			
			return sessionDescription.toString();
		}
	}

	public InetAddress getDestination() {
		return mDestination;
	}

	public boolean trackExists(int id) {
		if (id==0) 
			return mAudioStream!=null;
		else
			return mVideoStream!=null;
	}
	
	public Stream getTrack(int id) {
		if (id==0)
			return mAudioStream;
		else
			return mVideoStream;
	}

	/**
	 * Returns an approximation of the bandwidth consumed by the session in bit per second. 
	 */
	public long getBitrate() {
		long sum = 0;
		if (mAudioStream != null) sum += mAudioStream.getBitrate();
		if (mVideoStream != null) sum += mVideoStream.getBitrate();
		return sum;
	}
	
	/** Indicates if a track is currently running. */
	public boolean isStreaming() {
		if ( (mAudioStream!=null && mAudioStream.isStreaming()) || (mVideoStream!=null && mVideoStream.isStreaming()) )
			return true;
		else 
			return false;
	}
	
	/** 
	 * Starts one stream.
	 * @param id The id of the stream to start
	 **/
	public void start(int id) throws IllegalStateException, IOException {
		synchronized (sLock) {
			Stream stream = id==0 ? mAudioStream : mVideoStream;
			if (stream!=null && !stream.isStreaming()) {
				stream.setDestinationAddress(mDestination);
				stream.start();
			}
		}
	}

	/** Starts all streams. */
	public void start() throws IllegalStateException, IOException {
		synchronized (sLock) {
			if (mDestination.isMulticastAddress()) {
				if (mContext != null) {
					// Aquire a MulticastLock to allow multicasted UDP packet
					WifiManager wifi = (WifiManager)mContext.getSystemService( Context.WIFI_SERVICE );
					if(wifi != null){
						mLock = wifi.createMulticastLock("net.majorkernelpanic.streaming");
						mLock.acquire();
					}
				}
			}
		}
		start(0);
		start(1);
	}

	/** 
	 * Stops one stream.
	 * @param id The id of the stream to stop
	 **/	
	public void stop(int id) {
		synchronized (sLock) {
			// Release the MulticastLock if one was previously acquired
			if (mLock != null) {
				if (mLock.isHeld()) {
					mLock.release();
				}
				mLock = null;
			}
			Stream stream = id==0 ? mAudioStream : mVideoStream;
			if (stream!=null) {
				stream.stop();
			}
		}
	}	
	
	/** Stops all existing streams. */
	public void stop() {
		stop(0);
		stop(1);
	}

	/** Deletes all existing tracks & release associated resources. */
	public void flush() {
		synchronized (sLock) {
			if (mVideoStream!=null) {
				mVideoStream.stop();
				mVideoStream = null;
			}
			if (mAudioStream!=null) {
				mAudioStream.stop();
				mAudioStream = null;
			}
		}
	}

}
