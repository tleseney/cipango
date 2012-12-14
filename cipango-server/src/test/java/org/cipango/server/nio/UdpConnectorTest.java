package org.cipango.server.nio;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;

import org.cipango.server.MessageTooLongException;
import org.cipango.server.SipConnection;
import org.cipango.server.SipMessage;
import org.junit.Before;
import org.junit.Test;

public class UdpConnectorTest extends AbstractConnectorTest
{
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		UdpConnector connector = new UdpConnector(_server); 
		connector.setHost(getHost());
		connector.setPort(getPort());

		startServer(connector);
	}

	@Test
	public void testSendNearMtuSizeRequest() throws Exception
	{
		createPeer();

		_connector.stop();
		((UdpConnector) _connector).setMtu(2048);
		Thread.sleep(10);
		_connector.start();
    		
		SipConnection connection = _connector.getConnection(_peer.getInetAddress(), _peer.getPort());
		assertNotNull(connection);
    
		// Build a message 199 less than MTU, considering 23 for Content-* headers update.
		SipMessage orig = getAsMessage(SERIALIZED_REGISTER);
		byte[] buffer = new byte[2048 - SERIALIZED_REGISTER.length() - 23 - 199];
		Arrays.fill(buffer, (byte) 'a');
		orig.setContent(buffer, "text");
    
		try
		{
			connection.send(orig);
			fail();
		}
		catch (MessageTooLongException e)
		{
		}
	}
	
	@Test
	public void testSendBigRequest() throws Exception
	{
		createPeer();
		
		SipConnection connection = _connector.getConnection(_peer.getInetAddress(), _peer.getPort());
		assertNotNull(connection);

		SipMessage orig = getAsMessage(SERIALIZED_REGISTER);
		byte[] buffer = new byte[250 * 1024];
		Arrays.fill(buffer, (byte)'a');
		orig.setContent(buffer, "text");

		try
		{
			connection.send(orig);
			fail();
		}
		catch (MessageTooLongException e)
		{
		}
	}

	@Override
	protected void createPeer(int port) throws IOException
	{
		final DatagramSocket socket = new DatagramSocket(
				new InetSocketAddress(InetAddress.getByName(_connector.getHost()), port));
		_peer = new TestServerSocket(socket);
		_peer.start();
	}

	@Override
	protected void send(String message) throws Exception
	{
		DatagramSocket ds = new DatagramSocket();
		
		byte[] b = message.getBytes("UTF-8");
		DatagramPacket packet = new DatagramPacket(b, 0, b.length,
				InetAddress.getByName(getHost()), getPort());
	
		ds.send(packet);
		ds.close();
	}
	
	protected static class TestServerSocket extends Peer
	{
		private DatagramSocket _socket;
		private SocketAddress _peerAddress;
		
		public TestServerSocket(DatagramSocket socket) throws SocketException
		{
			_socket = socket;
			_socket.setSoTimeout(500);
		}

		@Override
		public void run()
		{
			try
			{
				super.run();
			}
			finally
			{
				_socket.close();
			}
		}

		@Override
		public InetAddress getInetAddress()
		{
			return _socket.getLocalAddress();
		}

		@Override
		public int getPort()
		{
			return _socket.getLocalPort();
		}

		@Override
		public void send(byte[] b) throws IOException
		{
			_socket.send(new DatagramPacket(b, b.length, _peerAddress));
		}

		@Override
		public int read(byte[] b) throws IOException
		{
			DatagramPacket packet = new DatagramPacket(b, b.length);
			try
			{
				_socket.receive(packet);
			}
			catch (SocketTimeoutException e)
			{
				return -1;
			}
			_peerAddress = packet.getSocketAddress();
			return packet.getLength();
		}

		@Override
		public void close() throws IOException
		{
			_socket.close();
		}
	}
}
