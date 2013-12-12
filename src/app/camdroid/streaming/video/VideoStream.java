package app.camdroid.streaming.video;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import app.camdroid.streaming.MediaStream;
import app.camdroid.streaming.mp4.MP4Config;
import app.camdroid.streaming.rtp.H264Packetizer;
import app.camdroid.streaming.rtp.MediaCodecInputStream;

/** 
 * Don't use this class directly.
 */
public class VideoStream extends MediaStream {

	protected final static String TAG = "VideoStream";

	protected VideoQuality mQuality = VideoQuality.DEFAULT_VIDEO_QUALITY.clone();
	protected SurfaceHolder.Callback mSurfaceHolderCallback = null;
	protected SurfaceHolder mSurfaceHolder = null;
	protected int mVideoEncoder, mCameraId = 0;
	protected Camera mCamera;
	protected boolean mCameraOpenedManually = true;
	protected boolean mFlashState = false;
	protected boolean mSurfaceReady = false;
	protected boolean mUnlocked = false;
	protected boolean mPreviewStarted = false;

	
	
	/**
	 * Constructs the H.264 stream.
	 * Uses CAMERA_FACING_BACK by default.
	 * @throws IOException
	 */
	public VideoStream() throws IOException {
		this(CameraInfo.CAMERA_FACING_BACK);
	}	

	/**
	 * Constructs the H.264 stream.
	 * @param cameraId Can be either CameraInfo.CAMERA_FACING_BACK or CameraInfo.CAMERA_FACING_FRONT
	 * @throws IOException
	 */
	public VideoStream(int cameraId) throws IOException {
		setCamera(cameraId);
		setMode(MODE_MEDIARECORDER_API);
		setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		mPacketizer = new H264Packetizer();
	}

	/**
	 * Sets the camera that will be used to capture video.
	 * You can call this method at any time and changes will take effect next time you start the stream.
	 * @param camera Can be either CameraInfo.CAMERA_FACING_BACK or CameraInfo.CAMERA_FACING_FRONT
	 */
	public void setCamera(int camera) {
		CameraInfo cameraInfo = new CameraInfo();
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i=0;i<numberOfCameras;i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == camera) {
				this.mCameraId = i;
				break;
			}
		}
	}

	public int getCamera() {
		return mCameraId;
	}

	/**
	 * Sets a Surface to show a preview of recorded media (video). 
	 * You can call this method at any time and changes will take effect next time you call {@link #start()}.
	 */
	public synchronized void setPreviewDisplay(SurfaceHolder surfaceHolder) {
		if (mSurfaceHolderCallback != null && mSurfaceHolder != null) {
			mSurfaceHolder.removeCallback(mSurfaceHolderCallback);
		}
		if (surfaceHolder != null) {
			mSurfaceHolderCallback = new Callback() {
				@Override
				public void surfaceDestroyed(SurfaceHolder holder) {
					mSurfaceReady = false;
					stopPreview();
					Log.d(TAG,"Surface destroyed !");
				}
				@Override
				public void surfaceCreated(SurfaceHolder holder) {
					mSurfaceReady = true;
				}
				@Override
				public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
					Log.d(TAG,"Surface Changed !");
				}
			};
			mSurfaceHolder = surfaceHolder;
			mSurfaceHolder.addCallback(mSurfaceHolderCallback);
			mSurfaceReady = true;
		}
	}

	

	/** 
	 * Modifies the resolution of the stream. You can call this method at any time 
	 * and changes will take effect next time you call {@link #start()}.
	 * {@link #setVideoQuality(VideoQuality)} may be more convenient.
	 * @param width Width of the stream
	 * @param height height of the stream
	 */
	public void setVideoSize(int width, int height) {
		if (mQuality.resX != width || mQuality.resY != height) {
			mQuality.resX = width;
			mQuality.resY = height;
		}
	}

	/** 
	 * Modifies the framerate of the stream. You can call this method at any time 
	 * and changes will take effect next time you call {@link #start()}.
	 * {@link #setVideoQuality(VideoQuality)} may be more convenient.
	 * @param rate Framerate of the stream
	 */	
	public void setVideoFramerate(int rate) {
		if (mQuality.framerate != rate) {
			mQuality.framerate = rate;
		}
	}

	/** 
	 * Modifies the bitrate of the stream. You can call this method at any time 
	 * and changes will take effect next time you call {@link #start()}.
	 * {@link #setVideoQuality(VideoQuality)} may be more convenient.
	 * @param bitrate Bitrate of the stream in bit per second
	 */	
	public void setVideoEncodingBitrate(int bitrate) {
		if (mQuality.bitrate != bitrate) {
			mQuality.bitrate = bitrate;
		}
	}

	/** 
	 * Modifies the quality of the stream. You can call this method at any time 
	 * and changes will take effect next time you call {@link #start()}.
	 * @param videoQuality Quality of the stream
	 */
	public void setVideoQuality(VideoQuality videoQuality) {
		if (!mQuality.equals(videoQuality)) {
			mQuality = videoQuality;
		}
	}

	/** 
	 * Returns the quality of the stream.  
	 */
	public VideoQuality getVideoQuality() {
		return mQuality;
	}	

	/** 
	 * Modifies the videoEncoder of the stream. You can call this method at any time 
	 * and changes will take effect next time you call {@link #start()}.
	 * @param videoEncoder Encoder of the stream
	 */
	protected void setVideoEncoder(int videoEncoder) {
		this.mVideoEncoder = videoEncoder;
	}	

	/** Stops the stream. */
	public synchronized void stop() {
		if (mCamera != null) {
			if (mMode == MODE_MEDIACODEC_API) {
				mCamera.setPreviewCallback(null);
			}
			super.stop();
			// We need to restart the preview
			if (!mCameraOpenedManually) {
				destroyCamera();
			} else {
				try {
					startPreview();
				} catch (RuntimeException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public synchronized void startPreview() throws RuntimeException, IOException {
		if (!mPreviewStarted) {
			createCamera();
			try {
				mCamera.startPreview();
				mPreviewStarted = true;
				mCameraOpenedManually = true;
			} catch (RuntimeException e) {
				destroyCamera();
				throw e;
			}
		}
	}

	/**
	 * Stops the preview.
	 */
	public synchronized void stopPreview() {
		mCameraOpenedManually = false;
		stop();
	}

	/**
	 * Encoding of the audio/video is done by a MediaRecorder.
	 */
	protected void encodeWithMediaRecorder() throws IOException {

		// We need a local socket to forward data output by the camera to the packetizer
		createSockets();

		// Opens the camera if needed
		createCamera();

		// Stops the preview if needed
		if (mPreviewStarted) {
			lockCamera();
			try {
				mCamera.stopPreview();
			} catch (Exception e) {}
			mPreviewStarted = false;
		}

		// Unlock the camera if needed
		unlockCamera();

		mMediaRecorder = new MediaRecorder();
		mMediaRecorder.setCamera(mCamera);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mMediaRecorder.setVideoEncoder(mVideoEncoder);
		mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
		mMediaRecorder.setVideoSize(mQuality.resX,mQuality.resY);
		mMediaRecorder.setVideoFrameRate(mQuality.framerate);
		mMediaRecorder.setVideoEncodingBitRate(mQuality.bitrate);

		// We write the ouput of the camera in a local socket instead of a file !			
		// This one little trick makes streaming feasible quiet simply: data from the camera
		// can then be manipulated at the other end of the socket
		mMediaRecorder.setOutputFile(mSender.getFileDescriptor());

		mMediaRecorder.prepare();
		mMediaRecorder.start();

		try {
			// mReceiver.getInputStream contains the data from the camera
			// the mPacketizer encapsulates this stream in an RTP stream and send it over the network
			mPacketizer.setDestination(mDestination, mRtpPort, mRtcpPort);
			mPacketizer.setInputStream(mReceiver.getInputStream());
			mPacketizer.start();
			mStreaming = true;
		} catch (IOException e) {
			stop();
			throw new IOException("Something happened with the local sockets :/ Start failed !");
		}

	}

	/**
	 * Encoding of the audio/video is done by a MediaCodec.
	 */
	@SuppressLint({ "InlinedApi", "NewApi" })
	protected void encodeWithMediaCodec() throws RuntimeException, IOException {

		// Opens the camera if needed
		createCamera();

		// Starts the preview if needed
		if (!mPreviewStarted) {
			try {
				mCamera.startPreview();
				mPreviewStarted = true;
			} catch (RuntimeException e) {
				destroyCamera();
				throw e;
			}
		}

		mMediaCodec = MediaCodec.createEncoderByType("video/avc");
		MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", mQuality.resX, mQuality.resY);
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mQuality.bitrate);
		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mQuality.framerate);	
		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 4);
		mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		mMediaCodec.start();

		final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();

		mCamera.setPreviewCallback(new Camera.PreviewCallback() {
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				long timeout = 1000000/mQuality.framerate;
				int bufferIndex = mMediaCodec.dequeueInputBuffer(timeout);

				if (bufferIndex>=0) {
					inputBuffers[bufferIndex].clear();
					inputBuffers[bufferIndex].put(data, 0, data.length);
					mMediaCodec.queueInputBuffer(bufferIndex, 0, data.length, System.nanoTime()/1000, 0);
				} else {
					Log.e(TAG,"No buffer available !");
				}

			}
		});

		try {
			// mReceiver.getInputStream contains the data from the camera
			// the mPacketizer encapsulates this stream in an RTP stream and send it over the network
			mPacketizer.setDestination(mDestination, mRtpPort, mRtcpPort);
			mPacketizer.setInputStream(new MediaCodecInputStream(mMediaCodec));
			mPacketizer.start();
			mStreaming = true;
		} catch (IOException e) {
			stop();
			throw new IOException("Something happened with the local sockets :/ Start failed !");
		}

	}

	@SuppressWarnings("deprecation")
	protected synchronized void createCamera() throws RuntimeException, IOException {
		if (mSurfaceHolder == null || mSurfaceHolder.getSurface() == null || !mSurfaceReady)
			throw new IllegalStateException("Invalid surface holder !");

		if (mCamera == null) {
			mCamera = Camera.open(mCameraId);
			mUnlocked = false;
			mCamera.setErrorCallback(new Camera.ErrorCallback() {
				@Override
				public void onError(int error, Camera camera) {
					// On some phones when trying to use the camera facing front the media server will die
					// Whether or not this callback may be called really depends on the phone
					if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
						// In this case the application must release the camera and instantiate a new one
						Log.e(TAG,"Media server died !");
						// We don't know in what thread we are so stop needs to be synchronized
						mCameraOpenedManually = false;
						stop();
					} else {
						Log.e(TAG,"Error unknown with the camera: "+error);
					}	
				}
			});

			Parameters parameters = mCamera.getParameters();

			if (mMode == MODE_MEDIACODEC_API) {
				parameters.setPreviewFormat(ImageFormat.YV12);
				parameters.setPreviewSize(mQuality.resX, mQuality.resY);
				parameters.setPreviewFrameRate(mQuality.framerate);
			}

			if (mFlashState) {
				if (parameters.getFlashMode()==null) {
					// The phone has no flash or the choosen camera can not toggle the flash
					throw new IllegalStateException("Can't turn the flash on !");
				} else {
					parameters.setFlashMode(mFlashState?Parameters.FLASH_MODE_TORCH:Parameters.FLASH_MODE_OFF);
				}
			}

			try {
				mCamera.setParameters(parameters);
				mCamera.setDisplayOrientation(mQuality.orientation);
				mCamera.setPreviewDisplay(mSurfaceHolder);
			} catch (RuntimeException e) {
				destroyCamera();
				throw e;
			} catch (IOException e) {
				destroyCamera();
				throw e;
			}
		}
	}

	protected synchronized void destroyCamera() {
		if (mCamera != null) {
			if (mStreaming) super.stop();
			lockCamera();
			mCamera.stopPreview();
			try {
				mCamera.release();
			} catch (Exception e) {
				Log.e(TAG,e.getMessage()!=null?e.getMessage():"unknown error");
			}
			mCamera = null;
			mUnlocked = false;
			mPreviewStarted = false;
		}	
	}	
	
	/** Verifies if streaming using the MediaCodec API is feasable. */
	@SuppressLint("NewApi")
	private void checkMediaCodecAPI() {
		for(int j = MediaCodecList.getCodecCount() - 1; j >= 0; j--){
			MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(j);
			if (codecInfo.isEncoder()) {
				MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType("video/avc");
				for (int i = 0; i < capabilities.colorFormats.length; i++) {
					int format = capabilities.colorFormats[i];
					Log.e(TAG, codecInfo.getName()+" with color format " + format);           
				}
				/*for (int i = 0; i < capabilities.profileLevels; i++) {
					int format = capabilities.colorFormats[i];
					Log.e(TAG, codecInfo.getName()+" with color format " + format);           
				}*/
			}
		}
	}

	protected void lockCamera() {
		if (mUnlocked) {
			Log.d(TAG,"Locking camera");
			try {
				mCamera.reconnect();
			} catch (Exception e) {
				Log.e(TAG,e.getMessage());
			}
			mUnlocked = false;
		}
	}

	protected void unlockCamera() {
		if (!mUnlocked) {
			Log.d(TAG,"Unlocking camera");
			try {	
				mCamera.unlock();
			} catch (Exception e) {
				Log.e(TAG,e.getMessage());
			}
			mUnlocked = true;
		}
	}
	
	
	

	private SharedPreferences mSettings = null;

	private Semaphore mLock = new Semaphore(0);


	/**
	 * Some data (SPS and PPS params) needs to be stored when {@link #generateSessionDescription()} is called 
	 * @param prefs The SharedPreferences that will be used to save SPS and PPS parameters
	 */
	public void setPreferences(SharedPreferences prefs) {
		mSettings = prefs;
	}

	/**
	 * Returns a description of the stream using SDP. It can then be included in an SDP file.
	 * Will fail if called when streaming.
	 */
	public synchronized  String generateSessionDescription() throws IllegalStateException, IOException {
		MP4Config config = testH264();

		return "m=video "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
		"a=rtpmap:96 H264/90000\r\n" +
		"a=fmtp:96 packetization-mode=1;profile-level-id="+config.getProfileLevel()+";sprop-parameter-sets="+config.getB64SPS()+","+config.getB64PPS()+";\r\n";
	}	

	/**
	 * Starts the stream.
	 * This will also open the camera and dispay the preview 
	 * if {@link #startPreview()} has not aready been called.
	 */
	public synchronized void start() throws IllegalStateException, IOException {
		MP4Config config = testH264();
		byte[] pps = Base64.decode(config.getB64PPS(), Base64.NO_WRAP);
		byte[] sps = Base64.decode(config.getB64SPS(), Base64.NO_WRAP);
		((H264Packetizer)mPacketizer).setStreamParameters(pps, sps);
		if (!mPreviewStarted) mCameraOpenedManually = false;
		super.start();
	}
	
	// Should not be called by the UI thread
	private MP4Config testH264() throws IllegalStateException, IOException {

		if (mSettings != null) {
			if (mSettings.contains("h264"+mQuality.framerate+","+mQuality.resX+","+mQuality.resY)) {
				String[] s = mSettings.getString("h264"+mQuality.framerate+","+mQuality.resX+","+mQuality.resY, "").split(",");
				return new MP4Config(s[0],s[1],s[2]);
			}
		}

		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			throw new IllegalStateException("No external storage or external storage not ready !");
		}

		final String TESTFILE = Environment.getExternalStorageDirectory().getPath()+"/spydroid-test.mp4";

		Log.i(TAG,"Testing H264 support... Test file saved at: "+TESTFILE);

		// Save flash state & set it to false so that led remains off while testing h264
		boolean savedFlashState = mFlashState;
		mFlashState = false;

		createCamera();
		
		// Stops the preview if needed
		if (mPreviewStarted) {
			lockCamera();
			try {
				mCamera.stopPreview();
			} catch (Exception e) {}
			mPreviewStarted = false;
		}
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		unlockCamera();

		mMediaRecorder = new MediaRecorder();
		mMediaRecorder.setCamera(mCamera);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mMediaRecorder.setMaxDuration(1000);
		mMediaRecorder.setVideoEncoder(mVideoEncoder);
		mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
		mMediaRecorder.setVideoSize(mQuality.resX,mQuality.resY);
		mMediaRecorder.setVideoFrameRate(mQuality.framerate);
		mMediaRecorder.setVideoEncodingBitRate(mQuality.bitrate);
		mMediaRecorder.setOutputFile(TESTFILE);

		// We wait a little and stop recording
		mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
			public void onInfo(MediaRecorder mr, int what, int extra) {
				Log.d(TAG,"MediaRecorder callback called !");
				if (what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
					Log.d(TAG,"MediaRecorder: MAX_DURATION_REACHED");
				} else if (what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
					Log.d(TAG,"MediaRecorder: MAX_FILESIZE_REACHED");
				} else if (what==MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN) {
					Log.d(TAG,"MediaRecorder: INFO_UNKNOWN");
				} else {
					Log.d(TAG,"WTF ?");
				}
				mLock.release();
			}
		});

		// Start recording
		mMediaRecorder.prepare();
		mMediaRecorder.start();

		try {
			if (mLock.tryAcquire(6,TimeUnit.SECONDS)) {
				Log.d(TAG,"MediaRecorder callback was called :)");
				Thread.sleep(400);
			} else {
				Log.d(TAG,"MediaRecorder callback was not called after 6 seconds... :(");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			try {
				mMediaRecorder.stop();
			} catch (Exception e) {}
			mMediaRecorder.release();
			mMediaRecorder = null;
			lockCamera();
		}

		// Retrieve SPS & PPS & ProfileId with MP4Config
		MP4Config config = new MP4Config(TESTFILE);

		// Delete dummy video
		File file = new File(TESTFILE);
		if (!file.delete()) Log.e(TAG,"Temp file could not be erased");

		// Restore flash state
		mFlashState = savedFlashState;

		Log.i(TAG,"H264 Test succeded...");

		// Save test result
		if (mSettings != null) {
			Editor editor = mSettings.edit();
			editor.putString("h264"+mQuality.framerate+","+mQuality.resX+","+mQuality.resY, config.getProfileLevel()+","+config.getB64SPS()+","+config.getB64PPS());
			editor.commit();
		}
		
		return config;

	}



}
