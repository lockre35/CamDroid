package app.camdroid.ui;
/*Handles activities for testing*/
/*Will be replaced with gui*/



import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import app.camdroid.CamdroidApplication;
import app.camdroid.R;
 
public class ActivityHandler extends Activity {
    Button a;
    Button b;
    Button c;
    CamdroidApplication mApplication=null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	//mApplication = (CamdroidApplication) getApplication();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_manager);
        a = (Button)findViewById(R.id.Button01);  
        b = (Button)findViewById(R.id.Button02);
        c = (Button)findViewById(R.id.Button03);
        
        /*Button listeners for starting activities*/
        a.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	startStreamAct();
            }
        });
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	startImgAct();
            }
        });
        c.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	startPrefAct();
            }
        });
    }
    public void startStreamAct(){
        Intent Vid = new Intent(this,StreamingActivity.class);
        startActivity(Vid);
    }	
    public void startImgAct(){
        Intent Cam = new Intent(this,CameraAct.class);
        startActivity(Cam);
    }
    public void startPrefAct(){
        Intent Pref = new Intent(this,VideoCapture.class);
        startActivity(Pref);
    }
}   