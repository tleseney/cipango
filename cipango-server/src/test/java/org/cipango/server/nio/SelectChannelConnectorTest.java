package org.cipango.server.nio;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Iterator;

import org.cipango.server.SipConnection;
import org.cipango.server.SipMessage;
import org.junit.Before;
import org.junit.Test;

public class SelectChannelConnectorTest extends AbstractConnectorTest
{
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		SelectChannelConnector connector = new SelectChannelConnector(_server); 
		connector.setHost(getHost());
		connector.setPort(getPort());

		startServer(connector);
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
		connection.send(orig);
		SipMessage message = _peer.getMessage();
		assertEquals(orig.getMethod(), message.getMethod());
		Iterator<String> it =  orig.getHeaderNames();
		while (it.hasNext())
		{
			String name = it.next();
			assertEquals(orig.getHeader(name), message.getHeader(name));
		}
		assertEquals(orig.getContent(), message.getContent());
	}

	@Override
	protected void createPeer() throws IOException
	{
		final ServerSocket socket = new ServerSocket();
		
		socket.bind(new InetSocketAddress(InetAddress.getByName(_connector.getHost()),
				getNextPeerPort()));
		_peer = new TestServerSocket(socket);
		_peer.start();
	}

	@Override
	protected void send(String message) throws Exception
	{
        Socket socket = new Socket(_connector.getHost(), _connector.getPort());

        socket.setSoTimeout(10000);
        socket.setTcpNoDelay(true);
        socket.setSoLinger(false, 0);
        
        OutputStream os = socket.getOutputStream();

        os.write(message.getBytes("UTF-8"));
        os.flush();
		socket.close();
	}
	
	protected static class TestServerSocket extends Peer
	{
		private ServerSocket _serverSocket;
		private Socket _socket;

		public TestServerSocket(ServerSocket socket)
		{
			_serverSocket = socket;
		}

		@Override
		public void run()
		{
			try
			{
				_socket = _serverSocket.accept();
				_socket.setSoTimeout(500);
				super.run();
			}
			catch (Exception e)
			{
				if (isActive())
					e.printStackTrace();
			}
			finally
			{
				try
				{
					_socket.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				try
				{
					_serverSocket.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		@Override
		public InetAddress getInetAddress()
		{
			return _serverSocket.getInetAddress();
		}

		@Override
		public int getPort()
		{
			return _serverSocket.getLocalPort();
		}

		@Override
		public void send(byte[] b) throws IOException
		{
			_socket.getOutputStream().write(b);
		}

		@Override
		public int read(byte[] b) throws IOException
		{
			try
			{
				return _socket.getInputStream().read(b);
			}
			catch (SocketTimeoutException e)
			{
				return -1;
			}
		}
	}
}
