package app.camdroid.ui;
//Call camera to show preview window then call camerupload when picture taken
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.Toast;
import app.camdroid.R;


@SuppressLint("NewApi")
public class CameraAct extends Activity implements SurfaceHolder.Callback{

protected static final String TAG = null;
Camera camera;
//Create surfaceview and holer for preview
SurfaceView surfaceView;
SurfaceHolder surfaceHolder;
boolean previewing = false;
LayoutInflater controlInflater = null;


  //Set up basic layout
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.camera_activity);
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
      //Set up preview window
      getWindow().setFormat(PixelFormat.UNKNOWN);
      surfaceView = (SurfaceView)findViewById(R.id.camerapreview);
      surfaceHolder = surfaceView.getHolder();
      surfaceHolder.addCallback(this);
      surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    

    
      Button buttonTakePicture = (Button)findViewById(R.id.takepicture);
      buttonTakePicture.setOnClickListener(new Button.OnClickListener(){

  //Take picture on button click  	  
  @Override
  public void onClick(View arg0) {
   // TODO Auto-generated method stub
   camera.takePicture(myShutterCallback,
     myPictureCallback_RAW, myPictureCallback_JPG);
  }});
  }

  //Method for starting next activity
  public void start(){
	   Intent Upload = new Intent(this, UploadImage.class);
	   startActivity(Upload);  
  }
  ShutterCallback myShutterCallback = new ShutterCallback(){

 //Shutter noise	  
 @Override
 public void onShutter() {
  // TODO Auto-generated method stub
 }};

//Picture callback from camera for saving image 
PictureCallback myPictureCallback_RAW = new PictureCallback(){

 @Override
 public void onPictureTaken(byte[] arg0, Camera arg1) {
  // TODO Auto-generated method stub
 }};

//Picture callback from camera in jpg form
PictureCallback myPictureCallback_JPG = new PictureCallback(){

	 @Override
	 public void onPictureTaken(byte[] arg0, Camera arg1) {
	  // TODO Auto-generated method stub 
	 //Set file name and path of image and store
	  ContentValues ins = new ContentValues();
	  String filename = "UploadFile";
	  ins.put(Media.TITLE,filename);
	  ins.put(Media.DESCRIPTION, "Captured By CamDroid");
	  OutputStream imageFileOS;
	 Uri uriTarget = Uri.parse("//media/external/images/media/" + "FileUpload" + ".jpg");
     File imagesFolder = new File(Environment.getExternalStorageDirectory(), "Punch");
     if (!imagesFolder.exists()) {
    	 imagesFolder.mkdirs();
    	 }
	    String fileName = "UploadFile.jpg";
	    File output = new File(imagesFolder, fileName);

	
	 if (output.exists()){
		 output.delete(); //DELETE existing file
		         fileName = "UploadFile.jpg";
		         output = new File(imagesFolder, fileName);

	}
	 
	 
	 try {
		 //Write image to location
		 Uri uriUp = Uri.fromFile(output);
		 imageFileOS = getContentResolver().openOutputStream(uriUp); 
	   imageFileOS.write(arg0);
	   imageFileOS.flush();
	   imageFileOS.close();
	   System.out.println("Exception : " + uriUp.toString());
	   Toast.makeText(CameraAct.this,
	     "Image saved: " + uriUp.toString(),
	     Toast.LENGTH_LONG).show();

	  } catch (FileNotFoundException e) {
	   // TODO Auto-generated catch block
	   e.printStackTrace();
	  } catch (IOException e) {
	   // TODO Auto-generated catch block
	   e.printStackTrace();
	  }
	 //Start next activity
	 start();
	  camera.startPreview();
	 }};

//Stops preview and releases the camera or starts the camera
@Override
public void surfaceChanged(SurfaceHolder holder, int format, int width,
  int height) {
 // TODO Auto-generated method stub
 if(previewing){
  camera.stopPreview();
  previewing = false;
 }

 if (camera != null){
  try {
   camera.setPreviewDisplay(surfaceHolder);
   camera.startPreview();
   previewing = true;
  } catch (IOException e) {
   // TODO Auto-generated catch block
   e.printStackTrace();
  }
 }
}

//Start the camera
@Override
public void surfaceCreated(SurfaceHolder holder) {
 // TODO Auto-generated method stub
 camera = Camera.open(0);
 Parameters params = camera.getParameters();
 params.setPictureSize(200, 200);
 camera.setParameters(params);
}

//Release the camera
@Override
public void surfaceDestroyed(SurfaceHolder holder) {
 // TODO Auto-generated method stub
 camera.stopPreview();
 camera.release();
 camera = null;
 previewing = false;
}


}