package app.camdroid.streaming.mp4;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Finds SPS & PPS parameters in mp4 file.
 */
public class MP4Config {

	private MP4Parser mp4Parser;
	private String mProfilLevel, mPPS, mSPS;

	public MP4Config(String profil, String sps, String pps) {
		mProfilLevel = profil; 
		mPPS = pps; 
		mSPS = sps;
	}
	
	/**
	 * Finds sps & pps parameters inside a .mp4.
	 * @param path Path to the file to analyze
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public MP4Config (String path) throws IOException, FileNotFoundException {

		StsdBox stsdBox; 
		
		// We open the mp4 file
		mp4Parser = new MP4Parser(path);

		// We parse it
		try {
			mp4Parser.parse();
		} catch (IOException ignore) {
			// Maybe enough of the file has been parsed and we can get the stsd box
		}

		// We find the stsdBox
		stsdBox = mp4Parser.getStsdBox();
		mPPS = stsdBox.getB64PPS();
		mSPS = stsdBox.getB64SPS();
		mProfilLevel = stsdBox.getProfileLevel();
		
		// We're done !
		mp4Parser.close();

	}

	public String getProfileLevel() {
		return mProfilLevel;
	}

	public String getB64PPS() {
		return mPPS;
	}

	public String getB64SPS() {
		return mSPS;
	}

}