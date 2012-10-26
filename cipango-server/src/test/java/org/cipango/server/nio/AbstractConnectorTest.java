package org.cipango.server.nio;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServletMessage;

import org.cipango.server.AbstractSipConnector;
import org.cipango.server.AbstractSipConnector.MessageBuilder;
import org.cipango.server.SipConnection;
import org.cipango.server.SipMessage;
import org.cipango.server.SipServer;
import org.cipango.server.handler.AbstractSipHandler;
import org.cipango.sip.SipParser;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractConnectorTest
{
	private static final String DEFAULT_HOST = "localhost";
	private static final int DEFAULT_PORT = 5040;
	private static final int DEFAULT_PEER_PORT = 4000;

	static private int __port = DEFAULT_PEER_PORT;

	protected SipServer _server;
	protected AbstractSipConnector _connector;
	protected Peer _peer;
	protected SipServletMessage _message;

	static int getNextPeerPort()
	{
		return __port++;
	}

	@Before
	public void setUp() throws Exception
	{
		_server = new SipServer();
		_server.setHandler(new TestHandler());
		_peer = null;
		_message = null;
	}
	
	@After
	public void tearDown() throws Exception
	{
		if (_peer != null)
			_peer.tearDown();

        _server.stop();
        Thread.sleep(100);
        //_server.getThreadPool().join();
	}

    protected void startServer(AbstractSipConnector connector) throws Exception
    {
        _connector = connector;
        _server.addConnector(_connector);
        _server.start();
    }

    public String getHost()
    {
    	return DEFAULT_HOST;
    }

    public int getPort()
    {
    	return DEFAULT_PORT;
    }
    
	@Test
	public void testLifeCycle() throws Exception
	{
		for (int i = 0; i < 10; i++)
		{
			_connector.stop();
			assertFalse(_connector.isRunning());
			_connector.start();
			assertTrue(_connector.isRunning());
			Thread.sleep(10);
		}
	}

	@Test
	public void testPing() throws Exception
	{
		for (int i = 0; i < 100; i++)
		{
			send(_pingEol);
			send(_pingEolEol);
		}
	}
	
	@Test
	public void testReceiveRequest() throws Exception
	{
		send(SERIALIZED_REGISTER);
		
		SipServletMessage message = getMessage(1000);
		assertNotNull(message);
		assertEquals("REGISTER", message.getMethod());
		assertEquals("c117fdfda2ffd6f4a859a2d504aedb25@127.0.0.1", message.getCallId());
	}

	@Test
	public void testSendRequest() throws Exception
	{
		createPeer();
		
		SipConnection connection = _connector.getConnection(_peer.getInetAddress(), _peer.getPort());
		assertNotNull(connection);
		
		SipMessage orig = getAsMessage(SERIALIZED_REGISTER);
		connection.send(orig);
		SipMessage message = _peer.getMessage();
		assertEquals(orig.getMethod(), message.getMethod());
		Iterator<String> it =  orig.getHeaderNames();
		while (it.hasNext())
		{
			String name = it.next();
			assertEquals(orig.getHeader(name), message.getHeader(name));
		}
	}
	
	@Test
	public void testParam() throws Exception
	{
		String uri = "sip:" + _connector.getHost() + ":" + _connector.getPort();
		String fullUri = uri + ";transport=" + _connector.getTransport().toString().toLowerCase();
		assertEquals(uri, _connector.getURI().toString());
		_connector.stop();
		_connector.setTransportParamForced(true);
		_connector.start();
		assertEquals(fullUri, _connector.getURI().toString());
	}
	
	@Test
	public void testBinaryContent() throws Exception
	{
		send(SERIALIZED_MESSAGE);
		
		String body = "A line in the dirt";
		SipServletMessage message = getMessage(1000);
		assertNotNull(message);
		assertEquals(body.length(), message.getContentLength());
		assertEquals("application/cipango-test", message.getContentType());
		assertArrayEquals(body.getBytes(), (byte[])message.getContent());
	}

	@Test
	public void testTextContent() throws Exception
	{
		send(SERIALIZED_MESSAGE.replace("application/cipango-test", "text/cipango-test"));
		
		String body = "A line in the dirt";
		SipServletMessage message = getMessage(1000);
		assertNotNull(message);
		assertEquals(body.length(), message.getContentLength());
		assertEquals("text/cipango-test", message.getContentType());
		assertEquals(body, message.getContent());

		send(SERIALIZED_MESSAGE.replace("application/cipango-test", "application/sdp"));

		message = getMessage(1000);
		assertNotNull(message);
		assertEquals(body.length(), message.getContentLength());
		assertEquals("text/cipango-test", message.getContentType());
		assertEquals(body, message.getContent());
	}

	class TestHandler extends AbstractSipHandler
	{
		public SipServer getServer() {
			return null;
		}

		public void setServer(Server server) {
		}

		@Override
		public void handle(SipMessage message) throws IOException,
				ServletException
		{
			_message = message;
		}	
	}

	static class TestMessageBuilder extends MessageBuilder
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

	protected static abstract class Peer implements Runnable
	{
		private boolean _active = false;
		private List<byte[]> _read = new ArrayList<byte[]>();
		private Thread _thread;

		public void start()
		{
			_active = true;
			_thread = new Thread(this);
			_thread.start();
		}

		public boolean isActive()
		{
			return _active;
		}

		public void tearDown() throws InterruptedException
		{
			_active = false;
			if (_thread != null)
				_thread.join();
		}

		public SipMessage getMessage() throws UnsupportedEncodingException
		{
	    	TestMessageBuilder builder = new TestMessageBuilder();
	    	SipParser parser = new SipParser(builder);

	    	while (!builder.finished)
	    	{
	    		byte[] b = _read.remove(0);
	    		assertNotNull(b);
	    		parser.parseNext(ByteBuffer.wrap(new String(b, "UTF-8").getBytes()));
	    	}
	    	return builder.getMessage();
		}

		@Override
		public void run()
		{
			try
			{
				byte[] b = new byte[2048];
				int length;
				while (isActive())
				{
					length = read(b);
                    if (length == -1)
                    	continue;
                    byte[] copy = new byte[length];
                    System.arraycopy(b, 0, copy, 0, length);
                    _read.add(copy);
				}
			}
			catch (Exception e)
			{
				if (isActive())
					e.printStackTrace();
			}
		}

		public abstract InetAddress getInetAddress();

		public abstract int getPort();

		public abstract void send(byte[] b) throws IOException;

		public abstract int read(byte[] b) throws IOException;
	}

	protected abstract void createPeer() throws Exception;

	protected abstract void send(String message) throws Exception;

	protected SipMessage getAsMessage(String messsage)
	{
		ByteBuffer b = ByteBuffer.wrap(messsage.getBytes());
		MessageBuilder builder = new MessageBuilder(null, null);
		SipParser parser = new SipParser(builder);
		parser.parseNext(b);
		return builder.getMessage();
	}
	
	private SipServletMessage getMessage(long timeout) throws InterruptedException
	{
		if (_message != null)
			return _message;
		long absTimeout = System.currentTimeMillis() + timeout;
		while (absTimeout - System.currentTimeMillis() > 0)
		{
			Thread.sleep(50);
			if (_message != null)
				return _message;
		}
		return null;
	}

	private String _pingEol = "\r\n";
	
	private String _pingEolEol = "\r\n\r\n";
	
	public static final String SERIALIZED_REGISTER = 
	        "REGISTER sip:127.0.0.1:5070 SIP/2.0\r\n"
	        + "Call-ID: c117fdfda2ffd6f4a859a2d504aedb25@127.0.0.1\r\n"
	        + "CSeq: 2 REGISTER\r\n"
	        + "From: <sip:cipango@cipango.org>;tag=9Aaz+gQAAA\r\n"
	        + "To: <sip:cipango@cipango.org>\r\n"
	        + "Via: SIP/2.0/UDP 127.0.0.1:6010;branch=z9hG4bKaf9d7cee5d176c7edf2fbf9b1e33fc3a\r\n"
	        + "Max-Forwards: 70\r\n"
	        + "User-Agent: Test Script\r\n"
	        + "Contact: \"Cipango\" <sip:127.0.0.1:6010;transport=udp>\r\n"
	        + "Allow: INVITE, ACK, BYE, CANCEL, PRACK, REFER, MESSAGE, SUBSCRIBE\r\n"
	        + "MyHeader: toto\r\n"
	        + "Content-Length: 0\r\n\r\n";
		
	private static final String SERIALIZED_MESSAGE = 
			"MESSAGE sip:proxy-gen2xx@127.0.0.1:5060 SIP/2.0\r\n"
			+ "Call-ID: 13a769769217a57d911314c67df8c729@192.168.1.205\r\n"
			+ "CSeq: 1 MESSAGE\r\n"
			+ "From: \"Alice\" <sip:alice@192.168.1.205:5071>;tag=1727584951\r\n"
			+ "To: \"JSR289_TCK\" <sip:JSR289_TCK@127.0.0.1:5060>\r\n"
			+ "Via: SIP/2.0/UDP 192.168.1.205:5071;branch=z9hG4bKaf9d7cee5d176c7edf2fbf9b1e33fc3a\r\n"
			+ "Max-Forwards: 5\r\n"
			+ "Route: \"JSR289_TCK\" <sip:proxy-gen2xx@127.0.0.1:5060;lr>\r\n"
			+ "route: <sip:127.0.0.1:5060;transport=udp;lr>\r\n"
			+ "Servlet-Name: Addressing\r\n"
			+ "Content-Type: application/cipango-test\r\n"
			+ "Content-Length: 18\r\n\r\n"
			+ "A line in the dirt";
}
