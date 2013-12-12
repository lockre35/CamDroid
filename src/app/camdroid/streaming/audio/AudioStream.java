package app.camdroid.streaming.audio;

import java.io.IOException;

import android.media.MediaRecorder;
import android.util.Log;
import app.camdroid.streaming.MediaStream;

/** 
 * Class which represents the Audio within streaming
 */
public abstract class AudioStream  extends MediaStream {

	protected int mAudioSource;
	protected int mOutputFormat;
	protected int mAudioEncoder;
	
	//Audio Quality with the default settings
	protected AudioQuality audQuality = AudioQuality.DEFAULT_AUDIO_QUALITY.clone();
	
	public AudioStream() {
		setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		//setAudioSource(MediaRecorder.AudioSource.MIC);
	}
	
	/** 
	 * Returns the quality of the audio stream.  
	 */
	public AudioQuality getAudioQuality() {
		return audQuality;
	}	
	
	//Methods for setting certain values within the audio
	public void setAudioSource(int audioSource) {
		mAudioSource = audioSource;
	}
	
	public void setAudiofps(int fps) {
		audQuality.fps = fps;
	}

	public void setAudioQuality(AudioQuality quality) {
		audQuality = quality;
	}
	
	/**
	 * Sets the encoding bit rate for the stream.
	 * @param bps bit rate in bit per second
	 */
	public void setAudioEncodingbps(int bps) {
		audQuality.bps = bps;
	}
	
	protected void setAudioEncoder(int audioEncoder) {
		mAudioEncoder = audioEncoder;
	}
	
	protected void setOutputFormat(int outputFormat) {
		mOutputFormat = outputFormat;
	}
	
	@Override
	protected void encodeWithMediaRecorder() throws IOException {
		
		//Local socket which will send the camera output to the packetizer
		createSockets();

		//Create 
		Log.v(TAG,"Requested audio with "+audQuality.bps/1000+"kbps"+" at "+audQuality.fps/1000+"kHz");
		
		//Make a new media recorder and give it our desired values
		mMediaRecorder = new MediaRecorder();
		mMediaRecorder.setAudioSource(mAudioSource);
		mMediaRecorder.setOutputFormat(mOutputFormat);
		mMediaRecorder.setAudioEncoder(mAudioEncoder);
		mMediaRecorder.setAudioChannels(1);
		mMediaRecorder.setAudioSamplingRate(audQuality.fps);
		mMediaRecorder.setAudioEncodingBitRate(audQuality.bps);
		
		
		//The output of the camera will be put into our local socket
		mMediaRecorder.setOutputFile(mSender.getFileDescriptor());
		//mMediaRecorder.setOutputFile("/sdcard/test.aac");
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
			throw new IOException("Error with the local sockets");
		}
		
	}
	
}
