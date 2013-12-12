package app.camdroid.ui;


import java.util.Locale;

import app.camdroid.R;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import app.camdroid.CamdroidApplication;
import app.camdroid.api.CustomHttpServer;
import app.camdroid.server.TinyHttpServer;
import app.camdroid.streaming.SessionBuilder;

/** 
 * Camdroid basically launches an HTTP server, 
 * clients can then connect to them and start/stop audio/video streams on the phone.
 */
public class StreamingActivity extends Activity {

	static final public String TAG = "CamdroidActivity";

	private ViewPager mViewPager;
	private CamdroidApplication mApplication;
	private CustomHttpServer mHttpServer;
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private TextView mTextView;

	

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mApplication = (CamdroidApplication) getApplication();

		setContentView(R.layout.preview);
		
		mSurfaceView = (SurfaceView)findViewById(R.id.tablet_camera_view);
		mSurfaceHolder = mSurfaceView.getHolder();
		SessionBuilder.getInstance().setSurfaceHolder(mSurfaceHolder);
		mTextView = (TextView)findViewById(R.id.tooltip);





		// Starts the service of the HTTP server
		this.startService(new Intent(this,CustomHttpServer.class));
		


	}

	public void onStart() {
		super.onStart();
		bindService(new Intent(this,CustomHttpServer.class), mHttpServiceConnection, Context.BIND_AUTO_CREATE);


	}

	@Override
	public void onStop() {
		super.onStop();
		unbindService(mHttpServiceConnection);

	}

	@Override
	public void onResume() {
		super.onResume();
		mApplication.applicationForeground = true;
	}

	@Override
	public void onPause() {
		super.onPause();
		Thread thread = new Thread()
		{
		    @Override
		    public void run() {
		        displayIpAddress();
		    }
		};
		thread.start();

		mApplication.applicationForeground = false;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG,"CamdroidActivity destroyed");
		super.onDestroy();
	}

	@Override    
	public void onBackPressed() {	 
		// Kills HTTP server
		this.stopService(new Intent(this,CustomHttpServer.class));

		// Returns to home menu
		finish();
	}


	

	private ServiceConnection mHttpServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mHttpServer = (CustomHttpServer) ((TinyHttpServer.LocalBinder)service).getService();
			mHttpServer.start();
			Thread thread = new Thread()
			{
			    @Override
			    public void run() {
			        displayIpAddress();
			    }
			};
			thread.start();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {}

	};


	public void log(String s) {
		Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
	}

    private void displayIpAddress() {
		runOnUiThread(new Runnable () {
			@Override
			public void run() {

					if (mHttpServer != null) {	
						if (!mHttpServer.isStreaming()){
							WifiManager wifiManager = (WifiManager) mApplication.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
							WifiInfo info = wifiManager.getConnectionInfo();
						    	int i = info.getIpAddress();
						        String ip = String.format(Locale.ENGLISH,"%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,i >> 16 & 0xff,i >> 24 & 0xff);
						    	
						    	mTextView.setText("http://");
						    	mTextView.append(ip);
						    	mTextView.append(":"+mHttpServer.getHttpPort());
					}		
				}
			}
		});

	    	//streamingState(0); 	
    }
	
}