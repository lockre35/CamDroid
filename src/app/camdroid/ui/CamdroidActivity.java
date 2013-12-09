/*
 * Copyright (C) 2011-2013 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of Spydroid (http://code.google.com/p/spydroid-ipcamera/)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package app.camdroid.ui;

import java.io.IOException;
import java.net.Socket;

import app.camdroid.R;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;
import app.camdroid.CamdroidApplication;
import app.camdroid.api.CustomHttpServer;
import app.camdroid.server.TinyHttpServer;
import app.camdroid.streaming.Session;
import app.camdroid.streaming.SessionBuilder;

/** 
 * Camdroid basically launches an HTTP server, 
 * clients can then connect to them and start/stop audio/video streams on the phone.
 */
public class CamdroidActivity extends FragmentActivity {

	static final public String TAG = "CamdroidActivity";

	private ViewPager mViewPager;
	private SectionsPagerAdapter mAdapter;
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private CamdroidApplication mApplication;
	private CustomHttpServer mHttpServer;
	

	@SuppressWarnings("deprecation")
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
		//SessionBuilder builder = SessionBuilder.getInstance().clone();
		/*Socket socket = mHttpServer.mSocket;
		Session testSession = new Session();
		testSession.setOrigin(socket.getLocalAddress());
		testSession.setDestination(socket.getInetAddress());
		try {
			testSession.start();
			Log.v("TestSessionStart","STARTED ");
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

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