package org.cipango.server.nio;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.junit.Before;

public class UdpConnectorTest extends AbstractConnectorTest
{
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		UdpConnector connector = new UdpConnector(_server); 
		connector.setHost("localhost");
		connector.setPort(5040);

		startServer(connector);
	}
	
	@Override
	protected void send(String message) throws Exception
	{
		DatagramSocket ds = new DatagramSocket();
		
		byte[] b = message.getBytes("UTF-8");
		DatagramPacket packet = new DatagramPacket(b, 0, b.length, InetAddress.getByName("localhost"), 5040);
	
		ds.send(packet);
		ds.close();
	}
}
