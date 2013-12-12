package app.camdroid.ui;
//Captures video and saves, calls video upload
import java.io.IOException;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import app.camdroid.R;

public class VideoCapture extends Activity{

	private Camera myCamera;
	  private MyCameraSurfaceView myCameraSurfaceView;
	  private MediaRecorder mediaRecorder;
	
	Button myButton;
	SurfaceHolder surfaceHolder;
	boolean recording;
	
	  //Set up layout
	  @Override
	  public void onCreate(Bundle savedInstanceState) {
	      super.onCreate(savedInstanceState);   
	      recording = false;   
	      setContentView(R.layout.videocapture);
	    
	      //Get Camera for preview
	      myCamera = getCameraInstance();
	      if(myCamera == null){
	       Toast.makeText(VideoCapture.this,
	         "Fail to get Camera",
	         Toast.LENGTH_LONG).show();
	      }
	
	      //Surface view like camera capture
	      myCameraSurfaceView = new MyCameraSurfaceView(this, myCamera);
	      FrameLayout myCameraPreview = (FrameLayout)findViewById(R.id.videoview);
	      myCameraPreview.addView(myCameraSurfaceView);
	    
	      myButton = (Button)findViewById(R.id.mybutton);
	      myButton.setOnClickListener(myButtonOnClickListener);
	  }
	
	  Button.OnClickListener myButtonOnClickListener
	  = new Button.OnClickListener(){
	
	@Override
	public void onClick(View v) {
	 // TODO Auto-generated method stub
	 if(recording){
	              // stop recording and release camera
	              mediaRecorder.stop();  // stop the recording
	              releaseMediaRecorder(); // release the MediaRecorder object
	              //Exit after saved
	              start();
	
	 }else{
	
	  //Release Camera before MediaRecorder start
	  releaseCamera();
	
	        if(!prepareMediaRecorder()){
	         Toast.makeText(VideoCapture.this,
	           "Fail in prepareMediaRecorder()!\n - Ended -",
	           Toast.LENGTH_LONG).show();
	         finish();
	        }
	//Saves the video
	  mediaRecorder.start();
	  recording = true;
	  myButton.setText("STOP");
	 }
	}};
	
	//Start new activity
	public void start(){
		Intent Cam = new Intent(this,VideoUpload.class);
		startActivity(Cam);
	}
	
	//Initialize camera
	  @SuppressLint("NewApi")
	private Camera getCameraInstance(){
	// TODO Auto-generated method stub
	      Camera c = null;
	      try {
	          c = Camera.open(0); // attempt to get a Camera instance, need zero for nexus
	      }
	      catch (Exception e){
	          // Camera is not available (in use or does not exist)
	      }
	      return c; // returns null if camera is unavailable
	}
	
	  //Save the video using standard mediarecorder setup
	@SuppressLint("NewApi")
	private boolean prepareMediaRecorder(){
	   myCamera = getCameraInstance();
	   mediaRecorder = new MediaRecorder();
	
	   myCamera.unlock();
	   mediaRecorder.setCamera(myCamera);
	   //Get audion and video from camera and camcorder
	   mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
	   mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
	   //user front camera
	   mediaRecorder.setProfile(CamcorderProfile.get(0,CamcorderProfile.QUALITY_HIGH));
	   //Where to store and file max sizes
	   mediaRecorder.setOutputFile("/storage/emulated/0/Punch/myvideo.mp4");
	      mediaRecorder.setMaxDuration(60000); // Set max duration 60 sec.
	      mediaRecorder.setMaxFileSize(5000000); // Set max file size 5M
	  //Create preview
	   mediaRecorder.setPreviewDisplay(myCameraSurfaceView.getHolder().getSurface());
	
	   //Try catch for setup
	   try {
	       mediaRecorder.prepare();
	   } catch (IllegalStateException e) {
	       releaseMediaRecorder();
	       return false;
	   } catch (IOException e) {
	       releaseMediaRecorder();
	       return false;
	   }
	   return true;
	
	}
	
	  @Override
	  protected void onPause() {
	      super.onPause();
	      releaseMediaRecorder();       // if you are using MediaRecorder, release it first
	      releaseCamera();              // release the camera immediately on pause event
	  }
	
	  private void releaseMediaRecorder(){
	      if (mediaRecorder != null) {
	          mediaRecorder.reset();   // clear recorder configuration
	          mediaRecorder.release(); // release the recorder object
	          mediaRecorder = null;
	          myCamera.lock();           // lock camera for later use
	      }
	  }
	
	  private void releaseCamera(){
	      if (myCamera != null){
	          myCamera.release();        // release the camera for other applications
	          myCamera = null;
	      }
	  }
	
	public class MyCameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback{
	
	private SurfaceHolder mHolder;
	   private Camera mCamera;
	
	public MyCameraSurfaceView(Context context, Camera camera) {
	       super(context);
	       mCamera = camera;
	       // Install a SurfaceHolder.Callback for notification
	       // underlying surface is created and destroyed.
	       mHolder = getHolder();
	       mHolder.addCallback(this);
	       // deprecated setting, but required on Android versions prior to 3.0
	       mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	   }
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int weight,
	  int height) {
	       // Need to setup rotation
	       // Make sure to stop the preview before resizing or reformatting it.
	       if (mHolder.getSurface() == null){
	         // preview surface does not exist
	         return;
	       }
	
	       // stop preview before making changes
	       try {
	           mCamera.stopPreview();
	       } catch (Exception e){
	         // ignore: tried to stop a non-existent preview
	       }
	
	       // make any resize, rotate or reformatting changes here
	
	       // start preview with new settings
	       try {
	           mCamera.setPreviewDisplay(mHolder);
	           mCamera.startPreview();
	
	       } catch (Exception e){
	       }
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	 // TODO Auto-generated method stub
	 // Where to draw preview
	       try {
	           mCamera.setPreviewDisplay(holder);
	           mCamera.startPreview();
	       } catch (IOException e) {
	       }
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	 // TODO Auto-generated method stub
	
	}
	}
}