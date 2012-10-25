package org.cipango.server.nio;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.cipango.server.SipConnection;
import org.cipango.server.SipMessage;
import org.cipango.server.AbstractSipConnector.MessageBuilder;
import org.cipango.sip.SipParser;
import org.junit.Before;
import org.junit.Test;

public class SelectChannelConnectorTest extends AbstractConnectorTest
{

	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		SelectChannelConnector connector = new SelectChannelConnector(_server); 
		connector.setHost("localhost");
		connector.setPort(5040);

		startServer(connector);
	}
	
	@Test
	public void testSendRequest() throws Exception
	{
		final ServerSocket socket = new ServerSocket();
		
		socket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), _connector.getPort() + 1));
		TestServerSocket ts = new TestServerSocket(socket);
		ts.start();
		
		SipConnection connection = _connector.getConnection(socket.getInetAddress(), socket.getLocalPort());
		assertNotNull(connection);
		
		SipMessage orig = getAsMessage(SERIALIZED_REGISTER);
		connection.send(orig);
		SipMessage message = read(ts);
		socket.close();
		assertEquals(orig.getMethod(), message.getMethod());
		Iterator<String> it =  orig.getHeaderNames();
		while (it.hasNext())
		{
			String name = it.next();
			assertEquals(orig.getHeader(name), message.getHeader(name));
		}
	}
	
	@Test
	public void testSendBigRequest() throws Exception
	{
		final ServerSocket socket = new ServerSocket();
		
		socket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), _connector.getPort() + 1));
		TestServerSocket ts = new TestServerSocket(socket);
		ts.start();
		
		SipConnection connection = _connector.getConnection(socket.getInetAddress(), socket.getLocalPort());
		assertNotNull(connection);

		SipMessage orig = getAsMessage(SERIALIZED_REGISTER);
		byte[] buffer = new byte[250 * 1024];
		Arrays.fill(buffer, (byte)'a');
		orig.setContent(buffer, "text");
		connection.send(orig);
		SipMessage message = read(ts);
		socket.close();
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
	protected void send(String message) throws Exception
	{
        Socket socket = new Socket("localhost", _connector.getPort());

        socket.setSoTimeout(10000);
        socket.setTcpNoDelay(true);
        socket.setSoLinger(false, 0);
        
        OutputStream os = socket.getOutputStream();

        os.write(message.getBytes("UTF-8"));
        os.flush();
		socket.close();
	}
	
	private SipMessage read(TestServerSocket ts) throws UnsupportedEncodingException
	{
    	TestMessageBuilder builder = new TestMessageBuilder();
    	SipParser parser = new SipParser(builder);

    	while (!builder.finished)
    	{
    		byte[] b = ts.pop();
    		assertNotNull(b);
    		parser.parseNext(ByteBuffer.wrap(new String(b, "UTF-8").getBytes()));
    	}
    	return builder.getMessage();
	}
	
	class TestServerSocket extends Thread
	{
		private List<byte[]> _read = new ArrayList<byte[]>();
		private ServerSocket _serverSocket;
		private Socket _socket;
		
		private boolean _active = true;
		
		public TestServerSocket(ServerSocket socket)
		{
			_serverSocket = socket;
		}
		
		public void setUnactive()
		{
			_active = false;
		}
		
		@Override
		public void run()
		{
			try
			{
				_socket = _serverSocket.accept();
				byte[] b = new byte[2048];
				while (_active)
				{
					int i = _socket.getInputStream().read(b);
					if (i == -1)
						continue;
					byte[] b2 = new byte[i];
					System.arraycopy(b, 0, b2, 0, i);
					_read.add(b2);
				}
			}
			catch (Exception e)
			{
				if (_active)
					e.printStackTrace();
			}
			finally
			{
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

		public byte[] pop()
		{
			if (_read.size() > 0)
				return _read.remove(0);
			return null;
		}

		public Socket getSocket()
		{
			return _socket;
		}
		
		public void send(byte[] b) throws IOException
		{
			getSocket().getOutputStream().write(b);
		}
	}
	
	class TestMessageBuilder extends MessageBuilder
	{
		boolean finished = false;

		public TestMessageBuilder()
		{
			super(null, null);
		}
  		
		@Override
		public boolean messageComplete(ByteBuffer content)
		{
			finished = true;
			return super.messageComplete(content);
		}
   	}
}
