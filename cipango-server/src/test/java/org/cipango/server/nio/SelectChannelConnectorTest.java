package org.cipango.server.nio;

import java.io.OutputStream;
import java.net.Socket;

import org.junit.Before;

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
}
