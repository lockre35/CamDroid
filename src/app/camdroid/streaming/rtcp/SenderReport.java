package app.camdroid.streaming.rtcp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Implementation of Sender Report RTCP packets.
 */
public class SenderReport {

	public static final int MTU = 1500;

	private MulticastSocket usock;
	private DatagramPacket upack;

	private byte[] buffer = new byte[MTU];
	private int ssrc, port = -1;
	private int octetCount = 0, packetCount = 0;

	public SenderReport() throws IOException {

		/*							     Version(2)  Padding(0)					 					*/
		/*									 ^		  ^			PT = 0	    						*/
		/*									 |		  |				^								*/
		/*									 | --------			 	|								*/
		/*									 | |---------------------								*/
		/*									 | ||													*/
		/*									 | ||													*/
		buffer[0] = (byte) Integer.parseInt("10000000",2);

		/* Packet Type PT */
		buffer[1] = (byte) 200;

		/* Byte 2,3          ->  Length		                     */
		setLong(28/4-1, 2, 4);

		/* Byte 4,5,6,7      ->  SSRC                            */
		/* Byte 8,9,10,11    ->  NTP timestamp hb				 */
		/* Byte 12,13,14,15  ->  NTP timestamp lb				 */
		/* Byte 16,17,18,19  ->  RTP timestamp		             */
		/* Byte 20,21,22,23  ->  packet count				 	 */
		/* Byte 24,25,26,27  ->  octet count			         */

		usock = new MulticastSocket();
		upack = new DatagramPacket(buffer, 1);

	}

	public void close() {
		usock.close();
	}

	/** Sends the RTCP packet over the network. */
	public void send() throws IOException {
		upack.setLength(28);
		usock.send(upack);
	}

	/** Sends the RTCP packet over the network. */
	public void send(long ntpts, long rtpts) throws IOException {
		long hb = ntpts/1000000000;
		long lb = ( ( ntpts - hb*1000000000 ) * 4294967296L )/1000000000;
		setLong(hb, 8, 12);
		setLong(lb, 12, 16);
		setLong(rtpts, 16, 20);
		upack.setLength(28);
		usock.send(upack);		
	}
	
	/** 
	 * Updates the number of packets sent, and the total amount of data sent.
	 * @param length The length of the packet 
	 **/
	public void update(int length) {
		packetCount += 1;
		octetCount += length;
		setLong(packetCount, 20, 24);
		setLong(octetCount, 24, 28);
	}

	/** Sets the RTP timestamp of the sender report. */
	public void setRtpTimestamp(long ts) {
		setLong(ts, 16, 20);
	}

	/** Sets the NTP timestamp of the sender report. */
	public void setNtpTimestamp(long ts) {
		long hb = ts/1000000000;
		long lb = ( ( ts - hb*1000000000 ) * 4294967296L )/1000000000;
		setLong(hb, 8, 12);
		setLong(lb, 12, 16);
	}

	public void setSSRC(int ssrc) {
		this.ssrc = ssrc; 
		setLong(ssrc,4,8);
		packetCount = 0;
		octetCount = 0;
		setLong(packetCount, 20, 24);
		setLong(octetCount, 24, 28);
	}

	public void setDestination(InetAddress dest, int dport) {
		port = dport;
		upack.setPort(dport);
		upack.setAddress(dest);
	}

	public int getPort() {
		return port;
	}

	public int getLocalPort() {
		return usock.getLocalPort();
	}

	public int getSSRC() {
		return ssrc;
	}

	private void setLong(long n, int begin, int end) {
		for (end--; end >= begin; end--) {
			buffer[end] = (byte) (n % 256);
			n >>= 8;
		}
	}	

}
