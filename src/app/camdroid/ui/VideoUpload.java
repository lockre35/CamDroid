package app.camdroid.ui;
//Upload video to server in chunk form
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.widget.TextView;
import app.camdroid.R;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
@SuppressLint("NewApi")
public class VideoUpload extends Activity {
    TextView tv = null;
    
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
	@SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videoupload);
        tv = (TextView) findViewById(R.id.tv);
        
        //Override policy for main thread checking
        //NEED TO CHANGE THIS---VERY BAD PRACTICE
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy); 

          HttpURLConnection conn = null;
          DataOutputStream dos = null;
          DataInputStream inStream = null;    
          String exsistingFileName = "/storage/emulated/0/Punch/myvideo.mp4";
          
          // Is this the place are you doing something wrong.
          String lineEnd = "\r\n";
          String twoHyphens = "--";
          String boundary =  "*****";

          int bytesRead, bytesAvailable, bufferSize;
          byte[] buffer;
          int maxBufferSize = 1*1024*1024;
          String urlString = "http://www.camdroid.loganlaughery.com/Camdroid_WebApplication_v0.2/Upload_video_ANDROID/upload_video.php";
                
          try
          {       
           Log.e("MediaPlayer","Inside second Method");
           FileInputStream fileInputStream = new FileInputStream(new File(exsistingFileName) );

           URL url = new URL(urlString);
           conn = (HttpURLConnection) url.openConnection();
           conn.setDoInput(true);

           // Allow Outputs
           conn.setDoOutput(true);

           // Don't use a cached copy.
           conn.setUseCaches(false);

           // Use a post method.
           conn.setRequestMethod("POST");
           conn.setRequestProperty("Connection", "Keep-Alive");       
           conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);      
           dos = new DataOutputStream( conn.getOutputStream() );
           dos.writeBytes(twoHyphens + boundary + lineEnd);
           dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + exsistingFileName +"\"" + lineEnd);
           dos.writeBytes(lineEnd);
           Log.e("MediaPlayer","Headers are written");

           bytesAvailable = fileInputStream.available();
           bufferSize = Math.min(bytesAvailable, maxBufferSize);
           buffer = new byte[bufferSize];
           bytesRead = fileInputStream.read(buffer, 0, bufferSize);
           
           while (bytesRead > 0)
           {
            dos.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
           }

           dos.writeBytes(lineEnd);
           dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
           BufferedReader in = new BufferedReader(
                           new InputStreamReader(
                           conn.getInputStream()));
                String inputLine;
                
                while ((inputLine = in.readLine()) != null) 
                    tv.append(inputLine);
                  
           // close streams
           Log.e("MediaPlayer","File is written");
           fileInputStream.close();
           dos.flush();
           dos.close();
           //Start new activity
    		Intent Cam = new Intent(this,VideoCapture.class);
    		finish();
    		startActivity(Cam);
          }
          catch (MalformedURLException ex)
          {
               Log.e("MediaPlayer", "error: " + ex.getMessage(), ex);
          }
          catch (IOException ioe)
          {
               Log.e("MediaPlayer", "error: " + ioe.getMessage(), ioe);
          }

          //read the SERVER RESPONSE
          try {
                inStream = new DataInputStream ( conn.getInputStream() );
                String str;
               
                while (( str = inStream.readLine()) != null)
                {
                     Log.e("MediaPlayer","Server Response"+str);
                }
                /*while((str = inStream.readLine()) !=null ){
                    
                }*/
                inStream.close();

          }
          catch (IOException ioex){
               Log.e("MediaPlayer", "error: " + ioex.getMessage(), ioex);
          }
          
        }    
}
