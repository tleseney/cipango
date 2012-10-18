package org.cipango.server.transaction;

import java.util.LinkedList;
import java.util.Queue;

import javax.servlet.sip.SipServletResponse;

import junit.framework.Assert;

import org.cipango.server.AbstractSipConnector;
import org.cipango.server.SipConnection;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.SipServer;
import org.cipango.server.nio.TestSipHandler;
import org.cipango.server.nio.UdpConnector;
import org.cipango.server.servlet.DefaultServlet;
import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.Session;
import org.cipango.server.session.SessionManager;
import org.cipango.server.sipapp.SipAppContext;
import org.cipango.server.transaction.Transaction.State;
import org.cipango.sip.AddressImpl;
import org.cipango.sip.CSeq;
import org.cipango.sip.SipHeader;
import org.cipango.sip.SipMethod;
import org.cipango.sip.SipURIImpl;
import org.cipango.sip.Via;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TransactionManagerTest
{
	private SipServer _server;
	private AbstractSipConnector _connector;
	
	private SipServer _testServer;
	private SipAppContext _testContext;
	
	@Before
	public void setUp() throws Exception
	{
		_server = new SipServer();
		_connector = new UdpConnector(_server);
		_connector.setHost("localhost");
		_connector.setPort(45060);
		_server.addConnector(_connector);
		_server.setHandler(new TestSipHandler());
		_server.start();
		
		_testServer = new SipServer();
		AbstractSipConnector connector = new UdpConnector(_testServer);
		connector.setHost("localhost");
		connector.setPort(45061);
		_testServer.addConnector(connector);
		_testContext = new SipAppContext();	
		_testServer.setHandler(_testContext);
		_testContext.getSipServletHandler().addSipServlet(DefaultServlet.class.getName());
		_testServer.start();

	}
	
	@After
	public void tearDown() throws Exception
	{
        _server.stop();
	}
	
	@Test
	public void testClientTx() throws Exception
	{
		
		SipRequest request = new SipRequest();		
		request.setMethod(SipMethod.MESSAGE, SipMethod.MESSAGE.asString());
		request.setRequestURI(new SipURIImpl("localhost", 45061));
		request.getFields().add(SipHeader.CALL_ID.asString(), "1234@localhost");
		AddressImpl addr =  new AddressImpl("<sip:cipango@localhost>;tag=1");
		addr.parse();
		request.getFields().add(SipHeader.FROM.asString(), addr, true);
		addr = new AddressImpl("<sip:test@localhost>");
		addr.parse();
		request.getFields().add(SipHeader.TO.asString(), addr, true);
		request.getFields().add(SipHeader.CSEQ.asString(), new CSeq("1 MESSAGE"), true);
		
		SessionManager sessionManager = new SessionManager();
		ApplicationSession appSession = new ApplicationSession(sessionManager, "123");
		Session session = appSession.createSession(request);
		request.setSession(session);
		TestTxListener listener = new TestTxListener();
		ClientTransaction tx = _server.getTransactionManager().sendRequest(request, listener);
		//System.out.println(request);
		Via via = request.getTopVia();
		Assert.assertNotNull(via);
		Assert.assertEquals(_connector.getHost(), via.getHost());
		Assert.assertEquals(_connector.getPort(), via.getPort());
		Assert.assertEquals(_connector.getTransport().getName(), via.getTransport());
		listener.assertDone(1);
		//System.out.println(tx);
		Assert.assertEquals(State.COMPLETED, tx.getState());
	}
	
	
	public static class TestTxListener implements ClientTransactionListener
	{
		private Queue<SipServletResponse> _responses = new LinkedList<SipServletResponse>();
		@Override
		public void handleResponse(SipResponse response)
		{
			synchronized (_responses)
			{
				_responses.add(response);
				_responses.notify();
			}
		}

		@Override
		public void customizeRequest(SipRequest request, SipConnection connection)
		{
		}
		
		public void assertDone(int msgExpected) throws Exception
		{			
			long end = System.currentTimeMillis() + 5000;
			
			synchronized (_responses)
			{
				while (end > System.currentTimeMillis() && _responses.size() < msgExpected)
				{
					try
					{
						_responses.wait(end - System.currentTimeMillis());
					}
					catch (InterruptedException e)
					{
					}
				}
			}
			
			if (_responses.size() != msgExpected)
				Assert.fail("Received " + _responses.size() + " messages when expected " + msgExpected);
		}
		
	}
}
