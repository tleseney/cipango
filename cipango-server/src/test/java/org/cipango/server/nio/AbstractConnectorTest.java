package org.cipango.server.nio;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipURI;

import org.cipango.server.AbstractSipConnector;
import org.cipango.server.SipMessage;
import org.cipango.server.SipServer;
import org.cipango.server.handler.AbstractSipHandler;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractConnectorTest
{
	protected SipServer _server;
	protected AbstractSipConnector _connector;
	SipServletMessage _message;
	
	@Before
	public void setUp() throws Exception
	{
		_server = new SipServer();
		_server.setHandler(new TestHandler());
		_message = null;
	}
	
	@After
	public void tearDown() throws Exception
	{
        _server.stop();
        //_server.getThreadPool().join();
	}

    protected void startServer(AbstractSipConnector connector) throws Exception
    {
        _connector = connector;
        _server.addConnector(_connector);
        _server.start();
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
	public void testMessage() throws Exception
	{
		send(_serializedRegister);
		
		SipServletMessage message = getMessage(1000);
		send(_serializedRegister2);
		Thread.sleep(300);
		assertNotNull(message);
		assertEquals("REGISTER", message.getMethod());
		assertEquals("c117fdfda2ffd6f4a859a2d504aedb25@127.0.0.1", message.getCallId());
	}

	@Test
	public void testRoute() throws Exception
	{
		send(_serializedMessage);
		
		SipServletMessage message = getMessage(1000);
		send(_serializedRegister2);
		send(_serializedRegister2);
		send(_serializedRegister2);
		send(_serializedRegister2);
		
		Thread.sleep(100);
		assertNotNull(_message);
		
		Iterator<Address> it = message.getAddressHeaders("route");
		assertEquals("proxy-gen2xx", ((SipURI) it.next().getURI()).getUser());
		assertTrue(it.hasNext());
		
		assertEquals("com.bea.sipservlet.tck.apps.spectestapp.uas", message.getHeader("application-name"));
	}

//	@Test
//	public void testParam() throws Exception
//	{
//		assertEquals("sip:localhost:5040", _connector.getSipUri().toString());
//		_connector.stop();
//		_connector.setTransportParam(true);
//		_connector.start();
//		assertEquals("sip:localhost:5040;transport=udp", _connector.getSipUri().toString());
//	}
	
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
	
	protected abstract void send(String message) throws Exception;

	
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
	
	private String _serializedRegister = 
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
		
	private String _serializedRegister2 = 
	        "REGISTER sip:127.0.0.1:5070 SIP/2.0\r\n"
	        + "Call-ID: foo@bar\r\n"
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
		
	private String _serializedMessage = 
			"MESSAGE sip:proxy-gen2xx@127.0.0.1:5060 SIP/2.0\r\n"
			+ "Call-ID: 13a769769217a57d911314c67df8c729@192.168.1.205\r\n"
			+ "CSeq: 1 MESSAGE\r\n"
			+ "From: \"Alice\" <sip:alice@192.168.1.205:5071>;tag=1727584951\r\n"
			+ "To: \"JSR289_TCK\" <sip:JSR289_TCK@127.0.0.1:5060>\r\n"
			+ "Via: SIP/2.0/UDP 192.168.1.205:5071;branch=z9hG4bKaf9d7cee5d176c7edf2fbf9b1e33fc3a\r\n"
			+ "Max-Forwards: 5\r\n"
			+ "Route: \"JSR289_TCK\" <sip:proxy-gen2xx@127.0.0.1:5060;lr>,<sip:127.0.0.1:5060;transport=udp;lr>\r\n"
			+ "Application-Name: com.bea.sipservlet.tck.apps.spectestapp.uas\r\n"
			+ "Servlet-Name: Addressing\r\n"
			+ "Content-Type: text/plain\r\n"
			+ "Content-Length: 0\r\n\r\n";
}
