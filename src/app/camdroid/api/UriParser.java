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

package app.camdroid.api;

import static app.camdroid.streaming.SessionBuilder.AUDIO_AAC;
import static app.camdroid.streaming.SessionBuilder.AUDIO_AMRNB;
import static app.camdroid.streaming.SessionBuilder.AUDIO_NONE;
import static app.camdroid.streaming.SessionBuilder.VIDEO_H264;
import static app.camdroid.streaming.SessionBuilder.VIDEO_NONE;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import android.hardware.Camera.CameraInfo;
import android.util.Log;
import app.camdroid.streaming.Session;
import app.camdroid.streaming.SessionBuilder;
import app.camdroid.streaming.audio.AudioQuality;
import app.camdroid.streaming.video.VideoQuality;

/**
 * This class parses URIs received by the RTSP server and configures a Session accordingly.
 */
public class UriParser {

	public final static String TAG = "UriParser";
	
	/**
	 * Configures a Session according to the given URI.
	 * Here are some examples of URIs that can be used to configure a Session:
	 * <ul><li>rtsp://xxx.xxx.xxx.xxx:8086?h264&flash=on</li>
	 * <li>rtsp://xxx.xxx.xxx.xxx:8086?h263&camera=front&flash=on</li>
	 * <li>rtsp://xxx.xxx.xxx.xxx:8086?h264=200-20-320-240</li>
	 * <li>rtsp://xxx.xxx.xxx.xxx:8086?aac</li></ul>
	 * @param uri The URI
	 * @throws IllegalStateException
	 * @throws IOException
	 * @return A Session configured according to the URI
	 */
	public static Session parse(String uri) throws IllegalStateException, IOException {		
		SessionBuilder builder = SessionBuilder.getInstance().clone();

		List<NameValuePair> params = URLEncodedUtils.parse(URI.create(uri),"UTF-8");
		if (params.size()>0) {

			builder.setAudioEncoder(AUDIO_NONE).setVideoEncoder(VIDEO_NONE);

			// Those parameters must be parsed first or else they won't necessarily be taken into account
			for (Iterator<NameValuePair> it = params.iterator();it.hasNext();) {
				NameValuePair param = it.next();

				// UNICAST -> the client can use this to specify where he wants the stream to be sent
				/*if (param.getName().equalsIgnoreCase("unicast")) {
					if (param.getValue()!=null) {
						try {
							InetAddress addr = InetAddress.getByName(param.getValue());
							builder.setDestination(addr);
							Log.v("UriParser","Destination Address = " + param.getValue());
						} catch (UnknownHostException e) {
							throw new IllegalStateException("Invalid destination address !");
						}
					}					
				}*/

				// H.264
				if (param.getName().equalsIgnoreCase("h264")) {
					VideoQuality quality = VideoQuality.parseQuality("1000-20-640-480");
					builder.setVideoQuality(quality).setVideoEncoder(VIDEO_H264);
					Log.v("UriParser","Video Quality = " + param.getValue());
				}

				// AAC
				else if (param.getName().equalsIgnoreCase("aac")) {
					AudioQuality quality = AudioQuality.parseQuality(null);
					builder.setAudioQuality(quality).setAudioEncoder(AUDIO_AAC);
					Log.v("UriParser","Audio Quality = " + param.getValue());
				}

			}

		}

		if (builder.getVideoEncoder()==VIDEO_NONE && builder.getAudioEncoder()==AUDIO_NONE) {
			SessionBuilder b = SessionBuilder.getInstance();
			builder.setVideoEncoder(b.getVideoEncoder());
			builder.setAudioEncoder(b.getAudioEncoder());
		}

		return builder.build();

	}

}
