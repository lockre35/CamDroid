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

package app.camdroid;



import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import app.camdroid.streaming.SessionBuilder;
import app.camdroid.streaming.video.VideoQuality;

public class CamdroidApplication extends android.app.Application {

	public final static String TAG = "CamdroidApplication";
	
	/** We will be using this as the default quality */
	public VideoQuality videoQuality = new VideoQuality(640,480,15,500000);

	/** We will be using AAC */
	public int audioEncoder = SessionBuilder.AUDIO_AAC;

	/** We will be using Video_H264 */
	public int videoEncoder = SessionBuilder.VIDEO_H264;

	/** If the notification is enabled in the status bar of the phone. */
	public boolean notificationEnabled = true;

	/** The HttpServer will use those variables to send reports about the state of the app to the web interface. */
	public boolean applicationForeground = true;
	public Exception lastCaughtException = null;

	/** Contains an approximation of the battery level. */
	public int batteryLevel = 0;
	
	private static CamdroidApplication sApplication;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate() {

		sApplication = this;

		super.onCreate();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

		notificationEnabled = settings.getBoolean("notification_enabled", true);
		
		/*SessionBuilder.getInstance() 
		.setContext(getApplicationContext())
		.setAudioEncoder(audioEncoder)
		.setVideoEncoder(videoEncoder)
		.setVideoQuality(videoQuality);*/

		
	}

	public static CamdroidApplication getInstance() {
		return sApplication;
	}


}
