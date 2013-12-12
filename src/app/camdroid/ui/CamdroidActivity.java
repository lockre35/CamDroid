package app.camdroid.ui;


import app.camdroid.R;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
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
import android.widget.Toast;
import app.camdroid.CamdroidApplication;
import app.camdroid.api.CustomHttpServer;
import app.camdroid.server.TinyHttpServer;

/** 
 * Camdroid basically launches an HTTP server, 
 * clients can then connect to them and start/stop audio/video streams on the phone.
 */
public class CamdroidActivity extends FragmentActivity {

	static final public String TAG = "CamdroidActivity";

	private ViewPager mViewPager;
	private SectionsPagerAdapter mAdapter;
	private CamdroidApplication mApplication;
	private CustomHttpServer mHttpServer;
	

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mApplication = (CamdroidApplication) getApplication();

		setContentView(R.layout.camdroid);
			mAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
			mViewPager = (ViewPager) findViewById(R.id.tablet_pager);
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			mApplication.videoQuality.orientation = 0;
		mViewPager.setAdapter(mAdapter);


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

	@Override    
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.quit), 1);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.options), 1);
		return true;
	}

	@Override    
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;

		switch (item.getItemId()) {
		case R.id.options:
			// Starts QualityListActivity where user can change the streaming quality
			intent = new Intent(this.getBaseContext(),OptionsActivity.class);
			startActivityForResult(intent, 0);
			return true;
		case R.id.quit:
			quitCamdroid();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void quitCamdroid() {      
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
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {}

	};


	public void log(String s) {
		Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
	}

	class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			return new TabletFragment();
		}

		@Override
		public int getCount() {
			return 1;
		}

		public HandsetFragment getHandsetFragment() {
				return (HandsetFragment) getSupportFragmentManager().findFragmentById(R.id.handset);
		}

		public PreviewFragment getPreviewFragment() {
				return (PreviewFragment) getSupportFragmentManager().findFragmentById(R.id.preview);
		}

		@Override
		public CharSequence getPageTitle(int position) {
				switch (position) {
				case 0: return getString(R.string.page0);
				case 1: return getString(R.string.page2);
				}
				return null;
		}

	}

}