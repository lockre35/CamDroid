package app.camdroid.streaming;

import java.io.IOException;
import java.net.InetAddress;

/**
 * An interface that represents a Stream. 
 */
public interface Stream {

	public void start() throws IllegalStateException, IOException;
	public void stop();

	/** 
	 * Sets the destination ip address of the stream.
	 * @param dest The destination address of the stream 
	 */
	public void setDestinationAddress(InetAddress dest);	
	
	/** 
	 * Sets the destination ports of the stream.
	 * If an odd number is supplied for the destination port then the next 
	 * lower even number will be used for RTP and it will be used for RTCP.
	 * If an even number is supplied, it will be used for RTP and the next odd
	 * number will be used for RTCP.
	 * @param dport The destination port
	 */
	public void setDestinationPorts(int dport);
	
	/**
	 * Sets the destination ports of the stream.
	 * @param rtpPort Destination port that will be used for RTP
	 * @param rtcpPort Destination port that will be used for RTCP
	 */
	public void setDestinationPorts(int rtpPort, int rtcpPort);
	
	/** 
	 * Returns a pair of source ports, the first one is the 
	 * one used for RTP and the second one is used for RTCP. 
	 **/	
	public int[] getLocalPorts();
	
	/** 
	 * Returns a pair of destination ports, the first one is the 
	 * one used for RTP and the second one is used for RTCP. 
	 **/
	public int[] getDestinationPorts();
	
	public int getSSRC();

	/**
	 * Returns an approximation of the bitrate of the stream in bit per second. 
	 */
	public long getBitrate();
	
	/**
	 * The SSRC identifier of the stream.
	 * @return The SSRC
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public String generateSessionDescription() throws IllegalStateException, IOException;

	public boolean isStreaming();

}
