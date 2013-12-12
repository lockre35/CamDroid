package app.camdroid.ui;
//Upload image to serve using Base64
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Toast;
import app.camdroid.R;

public class UploadImage extends Activity {
    InputStream inputStream;
    //Setup layout
        @Override
    public void onCreate(Bundle icicle) {
            super.onCreate(icicle);
            setContentView(R.layout.uploadimage);
 
            //Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher);           
            Bitmap bitmap = BitmapFactory.decodeFile("/storage/emulated/0/Punch/UploadFile.jpg");//address of file
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream); //compress to which format you want.
            byte [] byte_arr = stream.toByteArray();
            String image_str = Base64.encodeBytes(byte_arr);
            final ArrayList<NameValuePair> nameValuePairs = new  ArrayList<NameValuePair>();
            //Add to name value pairs for sending
            nameValuePairs.add(new BasicNameValuePair("image",image_str));
 
             Thread t = new Thread(new Runnable() {
             
            @Override
            public void run() {
                  try{
                	  	 //Send to server like in login as http post
                         HttpClient httpclient = new DefaultHttpClient();
                         HttpPost httppost = new HttpPost("http://www.camdroid.loganlaughery.com/Camdroid_WebApplication_v0.2/Upload_image_ANDROID/upload_image.php");
                         httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                         HttpResponse response = httpclient.execute(httppost);
                         final String the_string_response = convertResponseToString(response);
                         //Handle responses
                         runOnUiThread(new Runnable() {
                                 
                                @Override
                                public void run() {
                                    Toast.makeText(UploadImage.this, "Response " + the_string_response, Toast.LENGTH_LONG).show(); 
                                    start();
                                }
                            });
                     }catch(final Exception e){
                          runOnUiThread(new Runnable() {
                             
                            @Override
                            public void run() {
                                Toast.makeText(UploadImage.this, "ERROR " + e.getMessage(), Toast.LENGTH_LONG).show();                              
                            }
                        });
                           System.out.println("Error in http connection "+e.toString());
                     }  
            }
        });
         t.start();
        }
        //Start next activity
        public void start(){
        	Intent camera = new Intent(this, CameraAct.class);
        	finish();
            startActivity(camera);
        }
        //Make response printable
        public String convertResponseToString(HttpResponse response) throws IllegalStateException, IOException{
 
             String res = "";
             StringBuffer buffer = new StringBuffer();
             inputStream = response.getEntity().getContent();
             final int contentLength = (int) response.getEntity().getContentLength(); //getting content length…..
              runOnUiThread(new Runnable() {
             
            @Override
            public void run() {
                Toast.makeText(UploadImage.this, "contentLength : " + contentLength, Toast.LENGTH_LONG).show();                     
            }
        });
          
             if (contentLength < 0){
             }
             else{
                    byte[] data = new byte[512];
                    int len = 0;
                    try
                    {
                        while (-1 != (len = inputStream.read(data)) )
                        {
                            buffer.append(new String(data, 0, len)); //converting to string and appending  to stringbuffer…..
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    try
                    {
                        inputStream.close(); // closing the stream…..
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    res = buffer.toString();     // converting stringbuffer to string…..
                    final String test = res;
                    runOnUiThread(new Runnable() {
                     
                    @Override
                    public void run() {
                       Toast.makeText(UploadImage.this, "Result : " + test, Toast.LENGTH_LONG).show();
                    }
                });
                    //System.out.println("Response => " +  EntityUtils.toString(response.getEntity()));
             }
             return res;
        }
}