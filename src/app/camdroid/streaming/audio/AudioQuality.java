package app.camdroid.streaming.audio;
//comment
/**
 * This class deals with the quality of the audio streamed
 */
public class AudioQuality {

	//Default value for the audio stream quality
	public final static AudioQuality DEFAULT_AUDIO_QUALITY = new AudioQuality(8000,32000);

	//Constructors for objects which will represent the audio stream quality
	public AudioQuality() {}

	/**
	 * Constructor with given parameters
	 * @param fps The sampling rate
	 * @param bps The bit rate in bit per seconds
	 */
	public AudioQuality(int fps, int bps) {
		this.fps = fps;
		this.bps = bps;
	}	

	//Initialize our rates
	public int fps = 0;
	public int bps = 0;
	
	//Creates a new AudioQuality with the same sampling rate and bit rate
	public AudioQuality clone() {
		AudioQuality aq = new AudioQuality(fps, bps);
		return aq;
	}
	
	//Method for testing equality of bit rates and sampling rates
	public boolean equals(AudioQuality quality) {
		if((quality!= null) && ((quality.bps == this.bps) & (quality.fps == quality.fps)))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	//Method which returns an Audio Quality by parsing string
	public static AudioQuality parseQuality(String str) {
		AudioQuality newAudQuality = new AudioQuality(0,0);
		if (str != null) {
			String[] config = str.split("-");
			//Avoid any out of bounds errors
			//Might be able to get rid of try catch block
			try {
				newAudQuality.bps = Integer.parseInt(config[0]);
				newAudQuality.bps = newAudQuality.bps*1000; // Convert our parseInt value to bits per second
				newAudQuality.fps = Integer.parseInt(config[1]);
			}
			catch (IndexOutOfBoundsException e) {}
		}
		return newAudQuality;
	}

	public static AudioQuality merge(AudioQuality audioQuality1, AudioQuality audioQuality2) {
		
		if (audioQuality2 != null && audioQuality1 != null) 
		{
			if (audioQuality1.fps==0)
				{
				audioQuality1.fps = audioQuality2.fps;
				}
			
			if (audioQuality1.bps==0)
				{
				audioQuality1.bps = audioQuality2.bps;
				}
		}
		return audioQuality1;
	}

}
