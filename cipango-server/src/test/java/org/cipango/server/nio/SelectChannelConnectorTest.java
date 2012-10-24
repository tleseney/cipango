package org.cipango.server.nio;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
		byte[] b = ts.pop();
		assertNotNull(b);
		String msg = new String(b , "UTF-8");
		//System.out.println(msg);
		SipMessage message = getAsMessage(SERIALIZED_REGISTER);
		assertEquals(orig.getMethod(), message.getMethod());
		Iterator<String> it =  orig.getHeaderNames();
		while (it.hasNext())
		{
			String name = it.next();
			assertEquals(orig.getHeader(name), message.getHeader(name));
		}
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
	
	
}
