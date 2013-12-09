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

import java.util.Locale;

import app.camdroid.R;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.camdroid.CamdroidApplication;
import app.camdroid.api.CustomHttpServer;
import app.camdroid.server.TinyHttpServer;

public class HandsetFragment extends Fragment {

	public final static String TAG = "HandsetFragment";
	
    private TextView mDescription1, mDescription2, mLine1, mLine2, mSignWifi;
    private LinearLayout mSignInformation, mSignStreaming;
    private Animation mPulseAnimation;
    
    private CamdroidApplication mApplication;
    private CustomHttpServer mHttpServer;
 
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	mApplication  = (CamdroidApplication) getActivity().getApplication();
    }
    
    
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View rootView = inflater.inflate(R.layout.main,container,false);
        mLine1 = (TextView)rootView.findViewById(R.id.line1);
        mLine2 = (TextView)rootView.findViewById(R.id.line2);
        mDescription1 = (TextView)rootView.findViewById(R.id.line1_description);
        mDescription2 = (TextView)rootView.findViewById(R.id.line2_description);
        mSignWifi = (TextView)rootView.findViewById(R.id.advice);
        mSignStreaming = (LinearLayout)rootView.findViewById(R.id.streaming);
        mSignInformation = (LinearLayout)rootView.findViewById(R.id.information);
        mPulseAnimation = AnimationUtils.loadAnimation(mApplication.getApplicationContext(), R.anim.pulse);
        return rootView ;
    }
	
	@Override
    public void onStart() {
    	super.onStart();   	
    }
    
	@Override
    public void onPause() {
    	super.onPause();
    	update();
    	getActivity().unregisterReceiver(mWifiStateReceiver);
    	getActivity().unbindService(mHttpServiceConnection);
    }
	
	@Override
    public void onResume() {
    	super.onResume();
		getActivity().bindService(new Intent(getActivity(),CustomHttpServer.class), mHttpServiceConnection, Context.BIND_AUTO_CREATE);
getActivity().registerReceiver(mWifiStateReceiver,new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }
	
	public void update() {
		getActivity().runOnUiThread(new Runnable () {
			@Override
			public void run() {
				if (mDescription1 != null) {
					if (mHttpServer != null) {
							mDescription1.setVisibility(View.VISIBLE);
							mLine1.setVisibility(View.VISIBLE);
							mDescription2.setVisibility(View.INVISIBLE);
							mLine2.setVisibility(View.INVISIBLE);
						
						if (!mHttpServer.isStreaming()) displayIpAddress();
						else streamingState(1);
					}		
				}
			}
		});
	}
	
	private void streamingState(int state) {
		if (state==0) {
			// Not streaming
			mSignStreaming.clearAnimation();
			mSignWifi.clearAnimation();
			mSignStreaming.setVisibility(View.GONE);
			mSignInformation.setVisibility(View.VISIBLE);
			mSignWifi.setVisibility(View.GONE);
		} else if (state==1) {
			// Streaming
			mSignWifi.clearAnimation();
			mSignStreaming.setVisibility(View.GONE);
			mSignStreaming.startAnimation(mPulseAnimation);
			mSignInformation.setVisibility(View.INVISIBLE);
			mSignWifi.setVisibility(View.GONE);
		} 
	}
	
    private void displayIpAddress() {
		WifiManager wifiManager = (WifiManager) mApplication.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiManager.getConnectionInfo();
	    	int i = info.getIpAddress();
	        String ip = String.format(Locale.ENGLISH,"%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,i >> 16 & 0xff,i >> 24 & 0xff);
	    	
	    	mLine1.setText("http://");
	    	mLine1.append(ip);
	    	mLine1.append(":"+mHttpServer.getHttpPort());
	    	streamingState(0); 	
    }

    
    private final ServiceConnection mHttpServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mHttpServer = (CustomHttpServer) ((TinyHttpServer.LocalBinder)service).getService();
			update();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {}
		
	};
    
    // BroadcastReceiver that detects wifi state changements
    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	// This intent is also received when app resumes even if wifi state hasn't changed :/
        	if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
        		update();
        	}
        } 
    };
    
    
	
}
