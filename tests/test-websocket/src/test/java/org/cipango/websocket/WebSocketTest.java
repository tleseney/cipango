package org.cipango.websocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;

import junit.framework.Assert;

import org.cipango.client.MessageHandler;
import org.cipango.client.SipClient;
import org.cipango.client.SipHeaders;
import org.cipango.client.UserAgent;
import org.cipango.server.AbstractSipConnector.MessageBuilder;
import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.sip.AddressImpl;
import org.cipango.sip.SipFields;
import org.cipango.sip.SipHeader;
import org.cipango.sip.SipMethod;
import org.cipango.sip.SipParser;
import org.cipango.sip.Via;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * @version $Revision$ $Date$
 */
public class WebSocketTest
{

	public static final String UAS = "uas";
	public static final String PROXY = "proxy";
	
    private static final String BASE_URL = "ws://127.0.0.1:8078/test-cipango-websocket/";
    private static int __port;
    private WebSocketClient _client;
    private TestWebSocket _websocket;
    
    
    @BeforeClass
    public static void startServer() throws Exception
    {
        __port = Math.abs(new Random().nextInt() % 30000) + 20000;
    }

    @AfterClass
    public static void stopServer() throws Exception
    {
    }
    
    @Before
    public void setUp() throws Exception
    {
    	_client = new WebSocketClient();
    	__port++;
    	_client.setBindAdddress(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), __port));
    	_websocket = new TestWebSocket();
    	_client.connect(_websocket, new URI(BASE_URL));
    	waitWsEvent();
    	Assert.assertTrue("Websocket is not open", _websocket.isOpen());
    	
    }
    
    private void waitWsEvent() throws InterruptedException
    {
    	synchronized (_websocket)
		{
    		_websocket.wait(2000);
		}
    }

    @Test
    public void testSimpleMessage() throws Exception
    {
    	_websocket.send(getRawMessage("/register.dat"));
    	
    	waitWsEvent();
    	Assert.assertNotNull("No response received", _websocket.getMessage());
    	
    	
    	assertSimilar(getRawMessage("/registerResponse.dat"), _websocket.getRawResponse());
    }
    
    
    @Test
	/**
	 * <pre>
	 * Web socket
	 *  client                   Cipango UAS
	 *   | INVITE                     |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
	 *   | ACK                        |
	 *   |--------------------------->|
	 *   |                        BYE |
	 *   |<---------------------------|
	 *   |                        200 |
	 *   |--------------------------->|
	 * </pre>
	 * 
	 */
    public void testUasCall() throws Exception
    {
    	_websocket.send(getRawMessage("/inviteUas.dat"));
    	
    	SipResponse response = (SipResponse) _websocket.waitMessage();
    	Assert.assertNotNull("No response received", response);
    	Assert.assertEquals(200, response.getStatus());
    	SipURI uri = (SipURI) response.getAddressHeader(SipHeaders.CONTACT).getURI();
    	Assert.assertEquals("ws", uri.getTransportParam());
    	
    	_websocket.send(createAck(response).toString());
    	
    	Assert.assertTrue("No BYE received", (_websocket.waitMessage() instanceof SipRequest));
    	SipRequest request = (SipRequest) _websocket.getMessage();
    	Assert.assertEquals("BYE", request.getMethod());
    	_websocket.send(new SipResponse(request, 200, "OK").toString());
    	Thread.sleep(50);
    }
    
    @Test
    /**
     * <pre>
	 * Web socket    proxy      SIP UA
	 *   client
	 *     |  INVITE    |          |
	 *     |----------->|          |
	 *     |            |INVITE    |
	 *     |            |--------->|
	 *     |            |      200 |
	 *     |            |<---------|
	 *     |       200  |          |
	 *     |<-----------|          |
	 *     | ACK        |          |
	 *     |----------->|          |
	 *     |            |ACK       |
	 *     |            |--------->|
	 *     |            |      BYE |
	 *     |            |<---------|
	 *     |        BYE |          |
	 *     |<-----------|          |
	 *     |       200  |          |
	 *     |----------->|          |
	 *     |            |      200 |
	 *     |            |--------->|
	 * </pre>
     */
    public void testProxyCall() throws Exception
    {
    	SipClient sipClient = new SipClient("localhost", 15070);
    	sipClient.start();
    	TestUserAgent userAgent = new TestUserAgent(new AddressImpl("sip:proxy@cipango.org"));
    	sipClient.addUserAgent(userAgent);
    	
    	_websocket.send(getRawMessage("/inviteProxy.dat"));
    	
    	SipServletRequest request = userAgent.waitInitialRequest();
    	Assert.assertNotNull(request);
    	System.out.println(request);
    	
    	// Ensure that there is a double route
    	Iterator<Address> it = request.getAddressHeaders(SipHeaders.RECORD_ROUTE);
    	Assert.assertTrue(it.hasNext());
    	Address address = it.next();
    	Assert.assertNull(((SipURI) address.getURI()).getTransportParam());
    	Assert.assertTrue(it.hasNext());
    	address = it.next();
    	Assert.assertEquals("ws", ((SipURI) address.getURI()).getTransportParam());
    	Assert.assertFalse(it.hasNext());
    	
    	request.createResponse(200).send();
    	
    	// Ensure that the proxy in invoked only once
    	SipResponse response = (SipResponse) _websocket.waitMessage();
    	Assert.assertNotNull("No response received", response);
    	Assert.assertEquals(200, response.getStatus());
    	Iterator<String> it2 = response.getHeaders("mode");
    	Assert.assertTrue(it2.hasNext());
    	it2.next();
    	Assert.assertFalse(it2.hasNext());
    	
    	_websocket.send(createAck(response).toString());
    	
    	request = userAgent.waitSubsequentRequest();
    	Assert.assertNotNull(request);
    	Assert.assertEquals("ACK", request.getMethod());
    	
    	request.getSession().createRequest("BYE").send();
    	
    	Assert.assertTrue("No BYE received", (_websocket.waitMessage() instanceof SipRequest));
    	request = (SipRequest) _websocket.getMessage();
    	Assert.assertEquals("BYE", request.getMethod());
    	_websocket.send(new SipResponse((SipRequest) request, 200, "OK").toString());
    	
    	SipServletResponse response2 = userAgent.waitResponse();
    	Assert.assertNotNull("No response received", response2);
    	Assert.assertEquals(200, response2.getStatus());
    }
    
    
    private SipRequest createAck(SipResponse response) throws ServletParseException
    {
    	SipRequest request = new SipRequest();
    	request.setMethod(SipMethod.ACK, null);
    	SipFields fields = request.getFields();
    	request.setRequestURI(response.getAddressHeader(SipHeaders.CONTACT).getURI());
    	fields.set(SipHeader.FROM, response.getFrom());
    	fields.set(SipHeader.TO, response.getTo());
    	fields.set(SipHeaders.CALL_ID, response.getCallId());
    	fields.set(SipHeader.CSEQ, "1 ACK");
    	Via via = new Via("SIP/2.0/WS 127.0.0.1:20565;branch=z9hG4bK56sdasks");
    	via.setBranch("z9hG4bK5" + new Random().nextInt());
    	fields.add(SipHeader.VIA.asString(), via, true);
    	ListIterator<String> it = response.getHeaders(SipHeaders.RECORD_ROUTE);
    	while (it.hasNext())
    		it.next();
    	while (it.hasPrevious())
			fields.add(SipHeader.ROUTE.asString(), it.previous());
    	
    	System.out.println(request.toString());
    	return request;
    }
 
    
    public SipMessage getSipMessage(String msg) throws Exception
    {
    	MessageBuilder builder = new MessageBuilder(null, null);
		SipParser parser = new SipParser(builder);
    	parser.parseNext(ByteBuffer.wrap(msg.getBytes()));
		return builder.getMessage();
    }
    
    /**
     * Compare ignoring tag values.
     * @param expected
     * @param actual
     */
    private void assertSimilar(String expected, String actual)
    {
    	int i = 0;
    	while (true)
		{
    		int j = expected.indexOf('\n', i);
    		if (j == -1)
    			break;
			String lineExpected = expected.substring(i, j);
			String lineActual = actual.substring(i, j);
			int indexTag = lineExpected.indexOf("tag=");
			if (indexTag == -1)
				indexTag = lineExpected.indexOf("app-session-id=");
			if (indexTag != -1)
			{
				lineExpected = lineExpected.substring(0, indexTag);
				lineActual = lineActual.substring(0, indexTag);
			}
			Assert.assertEquals(lineExpected, lineActual);
			i = j + 1;
		}
    	
    }
    
    private String getRawMessage(String name) throws IOException
    {
		InputStream is = getClass().getResourceAsStream(name);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int read;
		while ((read = is.read(buffer)) != -1)
		{
			os.write(buffer, 0, read);
		}
		String message = new String(os.toByteArray());
		message = message.replaceAll("\\$\\{callId\\}", new Random().nextInt() + "@localhost");
		message = message.replaceAll("\\$\\{host\\}", "127.0.0.1");
		message = message.replaceAll("\\$\\{port\\}", String.valueOf(__port));
		
		return message;

    }
    
    class TestWebSocket extends WebSocketAdapter
    {
    	private String _message;
    	private Session _session;
    	
    	


		@Override
		public void onWebSocketConnect(Session sess)
		{
			super.onWebSocketConnect(sess);
			_session = sess;
			synchronized (this)
  			{
                   notify();
  			}
		}

		@Override
		public void onWebSocketText(String data)
		{
			super.onWebSocketText(data);
			 if (!data.startsWith("SIP/2.0 100 Trying"))
        	 {
	        	 _message = data;
	             System.out.println("data = " + data);
	             synchronized (this)
				{
	                 notify();
				}
        	 }
		}
		        
     	@Override
 		public void onWebSocketClose(int statusCode, String reason)
 		{
 			super.onWebSocketClose(statusCode, reason);
 			_session = null;
 		}

		public SipMessage getMessage() throws Exception
		{
			if (_message == null)
				return null;
			return getSipMessage(_message);
		}
		
		public SipMessage waitMessage() throws Exception
		{
			synchronized (this)
			{
				wait(2000);
			}
			return getMessage();
		}
		
		public String getRawResponse() throws Exception
		{
			return _message;
		}

		public boolean isOpen()
		{
			return _session != null;
		}
		
		public void send(String message) throws IOException
		{
			getSession().getRemote().sendString(message);
		}

    }

    static class TestUserAgent extends UserAgent implements MessageHandler
    {
    	private SipServletRequest _initialRequest;
    	private SipServletRequest _subsequentRequest;
    	private SipServletResponse _response;
    	
		public TestUserAgent(Address address)
		{
			super(address);
		}

		@Override
		public void handleInitialRequest(SipServletRequest request)
		{
			_initialRequest = request;
			_initialRequest.getSession().setAttribute(MessageHandler.class.getName(), this);
			synchronized (this)
			{
				notify();
			}
		}

		public SipServletRequest getInitialRequest()
		{
			return _initialRequest;
		}
		
		public SipServletRequest waitInitialRequest() throws InterruptedException
		{
			synchronized (this)
			{
				wait(2000);
			}
			return _initialRequest;
		}

		public void handleRequest(SipServletRequest request) throws IOException, ServletException
		{
			_subsequentRequest = request;
			synchronized (this)
			{
				notify();
			}
		}

		public void handleResponse(SipServletResponse response) throws IOException, ServletException
		{
			_response = response;
			synchronized (this)
			{
				notify();
			}
		}

		public SipServletRequest waitSubsequentRequest() throws InterruptedException
		{
			synchronized (this)
			{
				wait(2000);
			}
			return _subsequentRequest;
		}

		public SipServletResponse waitResponse() throws InterruptedException
		{
			synchronized (this)
			{
				wait(2000);
			}
			return _response;
		}
		
    	
    }
}
