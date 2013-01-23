// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================
package org.cipango.server.dns;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import junit.framework.Assert;

import org.cipango.server.AbstractSipConnector;
import org.cipango.server.SipConnector;
import org.cipango.server.SipRequest;
import org.cipango.server.SipServer;
import org.cipango.server.Transport;
import org.cipango.server.dns.BlackListImpl.Criteria;
import org.cipango.server.nio.SelectChannelConnector;
import org.cipango.server.nio.UdpConnector;
import org.cipango.server.servlet.DefaultServlet;
import org.cipango.server.servlet.SipServletHolder;
import org.cipango.server.sipapp.SipAppContext;
import org.cipango.server.transaction.RetryableTransactionManager;
import org.cipango.server.transaction.TransactionManagerTest.TestServlet;
import org.cipango.sip.SipHeader;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


@SuppressWarnings("serial")
public class DnsTest
{
	private static final Logger LOG = Log.getLogger(DnsTest.class);
	
	private SipServer _server;
	private RetryableTransactionManager _transactionManager;
	private SipAppContext _context;
	private TestServlet _testServlet;
	private BlackListImpl _blackList;
	
	private SipServer _testServer;
	private SipServer _testServer2;
	
	@Before
	public void setUp() throws Exception
	{
		_testServlet = new TestServlet();
		_server = newServer(45060, _testServlet);
		_transactionManager = (RetryableTransactionManager) _server.getTransactionManager();
				
		Rfc3263DnsResolver resolver = new Rfc3263DnsResolver();
		resolver.setDnsService(new TestDnsService());
		_server.getTransportProcessor().setDnsResolver(resolver);
		_blackList = new BlackListImpl();
		_blackList.setCriteria(Criteria.IP_PORT_TRANSPORT);
		_server.getTransportProcessor().setBlackList(_blackList);
		_context = (SipAppContext) _server.getHandler();
		_server.start();
		
		_testServer = newServer(45061, new DefaultServlet());
		_testServer.start();

	}
	
	private SipServer newServer(int port, SipServlet servlet) throws Exception
	{
		SipServer server = new SipServer(null, new RetryableTransactionManager());
		AbstractSipConnector connector = new UdpConnector(server);
		connector.setHost("localhost");
		connector.setPort(port);
		server.addConnector(connector);
		connector = new SelectChannelConnector(server);
		connector.setHost("localhost");
		connector.setPort(port);
		server.addConnector(connector);
		
		SipAppContext context = new SipAppContext();	
		context.setName("testContext");
		server.setHandler(context);
		SipServletHolder holder = new SipServletHolder();
		holder.setServlet(servlet);
		context.getServletHandler().addServlet(holder);
		return server;
	}
	
	@After
	public void tearDown() throws Exception
	{
        _server.stop();
        _testServer.stop();
        if (_testServer2 != null)
        	_testServer2.stop();
        Thread.sleep(100);
	}
	
	private SipRequest createRequest(String requestUri) throws ServletParseException
	{
		SipFactory factory = _context.getSipFactory();
		SipApplicationSession appSession = factory.createApplicationSession();
		SipRequest request = (SipRequest) factory.createRequest(appSession, "MESSAGE", "<sip:cipango@localhost>", "<sip:test@localhost>");
		request.setRequestURI(factory.createURI(requestUri));
		return request;
	}
	
	@Test
	public void testNaptr() throws Exception
	{
		createRequest("sip:local.cipango.voip").send();
		
		_testServlet.assertDone(1);
		//System.out.println(_testServlet.getResponses());

	}
	
	@Test
	public void testMaxRetryTime() throws Exception
	{
		_transactionManager.setMaxRetryTime(600);
		TestServlet testServlet = new TestServlet()
		{			
			protected void doMessage(SipServletRequest request) throws IOException
			{
				try { Thread.sleep(700); } catch (InterruptedException e) {}
				request.createResponse(SipServletResponse.SC_SERVICE_UNAVAILABLE).send();
			}
		};
		_testServer2 = newServer(45062, testServlet);
		_testServer2.start();
		
		SipRequest request = createRequest("sip:backup-udp.cipango.voip");
		request.send();
		//System.out.println(request);
		//System.out.println(testServlet.getRequests());
		_testServlet.assertDone(1);
		SipServletResponse response =  _testServlet.getResponses().peek();
		Assert.assertEquals(SipServletResponse.SC_REQUEST_TIMEOUT, response.getStatus());

		Assert.assertEquals("Server 2 has not been invoked", 1, testServlet.getRequests().size()); 
		//System.out.println(_testServlet.getResponses());
		//System.out.println(request.session().dump());
		
		//System.out.println(_server.getTransactionManager().dump());
	} 
	
	/**
	 * Test that if first hop is not available, the second is tried.
	 */
	@Test
	public void testBackup() throws Exception
	{
		SipRequest request = createRequest("sip:backup.cipango.voip");
		request.send();
		//System.out.println(request);
		
		_testServlet.assertDone(1);
		//System.out.println(_testServlet.getResponses());
		//System.out.println(request.session().dump());
		Assert.assertEquals("There should be only one transaction", 1, request.session().getClientTransactions().size()); 
		
		//System.out.println(_server.getTransactionManager().dump());
		Assert.assertEquals("There should be only one transaction", 1, _server.getTransactionManager().getClientTransactions());

		Assert.assertEquals(1, _transactionManager.getRetries());
	} 
	
	/**
	 * Test that if first hop is not available due to "503 Service unavailable", the second hop is tried.
	 */
	@Test
	public void testBackupUdp503() throws Exception
	{
		TestServlet testServlet = new TestServlet()
		{			
			protected void doMessage(SipServletRequest request) throws IOException
			{
				request.createResponse(SipServletResponse.SC_SERVICE_UNAVAILABLE).send();
			}
		};
		_testServer2 = newServer(45062, testServlet);
		_testServer2.start();
		
		SipRequest request = createRequest("sip:backup-udp.cipango.voip");
		request.send();
		//System.out.println(request);
		//System.out.println(testServlet.getRequests());
		_testServlet.assertDone(1);
		SipServletResponse response =  _testServlet.getResponses().peek();
		Assert.assertEquals(SipServletResponse.SC_OK, response.getStatus());
		Iterator<String> it = response.getHeaders(SipHeader.VIA.asString());
		Assert.assertTrue(it.hasNext()); 
		it.next();
		Assert.assertFalse(it.hasNext()); 
		Assert.assertEquals("Server 2 has not been invoked", 1, testServlet.getRequests().size()); 
		//System.out.println(_testServlet.getResponses());
		//System.out.println(request.session().dump());
		
		//System.out.println(_server.getTransactionManager().dump());
		Assert.assertEquals(1, _transactionManager.getRetries());
	} 
	
	/**
	 * Ensure that if a server is blacklisted it is not contacted on next request.
	 * @throws Exception
	 */
	@Test
	public void testBlacklist() throws Exception
	{
		TestServlet testServlet = new TestServlet()
		{			
			protected void doMessage(SipServletRequest request) throws IOException
			{
				request.createResponse(SipServletResponse.SC_SERVICE_UNAVAILABLE).send();
			}
		};
		_testServer2 = newServer(45062, testServlet);
		_testServer2.start();
		
		SipRequest request = createRequest("sip:backup-udp.cipango.voip");
		request.send();
		//System.out.println(request);
		//System.out.println(testServlet.getRequests());
		_testServlet.assertDone(1);
		Assert.assertEquals(SipServletResponse.SC_OK, _testServlet.getResponses().peek().getStatus());
		Assert.assertEquals("Server 2 has not been invoked", 1, testServlet.getRequests().size()); 
		Hop hop = new Hop();
		SipConnector connector = _testServer2.getConnectors()[0];
		hop.setAddress(connector.getAddress());
		hop.setPort(connector.getPort());
		hop.setHost(connector.getHost());
		hop.setTransport(Transport.UDP);
		System.out.println(_blackList.dump());
		Assert.assertTrue(_blackList.isBlacklisted(hop));
		
		request = createRequest("sip:backup-udp.cipango.voip");
		request.send();
		_testServlet.assertDone(2);
		Assert.assertEquals("Server 2 has been called while blacklisted", 1, testServlet.getRequests().size()); 

		Assert.assertEquals(1, _transactionManager.getRetries());
	} 
	
	/**
	 * Test that if first hop is not available due to timer B or timer F fire 
	 * ("408 Request timeout" locally generated), the second hop is tried.
	 */
	@Test
	public void testBackupUdp408() throws Exception
	{
		// Reduce T1 value in order timer F expires early. 
		_server.getTransactionManager().setT1(100);
		TestServlet testServlet = new TestServlet()
		{			
			protected void doMessage(SipServletRequest request) throws IOException
			{
				// Do nothing
			}
		};
		_testServer2 = newServer(45062, testServlet);
		_testServer2.start();
		
		SipRequest request = createRequest("sip:backup-udp.cipango.voip");
		request.send();
		//System.out.println(request);
		//System.out.println(testServlet.getRequests());
		
		LOG.info("Wait 6,5 seconds for timer F expiration");
		Thread.sleep(6500);
		_testServlet.assertDone(1);
		Assert.assertEquals(SipServletResponse.SC_OK, _testServlet.getResponses().peek().getStatus());
		Assert.assertEquals("Server 2 has not been invoked", 1, testServlet.getRequests().size()); 
		//System.out.println(_testServlet.getResponses());
		//System.out.println(request.session().dump());
		Assert.assertEquals("There should be only one transaction", 1, request.session().getClientTransactions().size()); 
		
		//System.out.println(_server.getTransactionManager().dump());
		Assert.assertEquals("There should be only one transaction", 1, _server.getTransactionManager().getClientTransactions()); 	

		Assert.assertEquals(1, _transactionManager.getRetries());	
	} 
	
	@Test
	public void testUnknown() throws Exception
	{
		SipRequest request = createRequest("sip:unknown.cipango.voip");
		request.send();
		//System.out.println(request);
		Thread.sleep(100);
		_testServlet.assertDone(0);
		//System.out.println(_testServlet.getResponses());
		//System.out.println(request.session().dump());
		Assert.assertEquals("There should be only one transaction", 1, request.session().getClientTransactions().size()); 
		
		//System.out.println(_server.getTransactionManager().dump());
		Assert.assertEquals("There should be only one transaction", 1, _server.getTransactionManager().getClientTransactions());

		Assert.assertEquals(0, _transactionManager.getRetries());
	} 
	
	/**
	 * Test to connect to an invalid local address.
	 * Ensures that it does not create infinite loop.
	 */
	@Test
	public void testUnavailableTcp() throws Exception
	{
		SipRequest request = createRequest("sip:127.1.1.1;transport=tcp");
		request.send();
		//System.out.println(request);
		Thread.sleep(100);
		_testServlet.assertDone(0);
		//System.out.println(_testServlet.getResponses());
		//System.out.println(request.session().dump());
		Assert.assertEquals("There should be only one transaction", 1, request.session().getClientTransactions().size()); 
		
		//System.out.println(_server.getTransactionManager().dump());
		Assert.assertEquals("There should be only one transaction", 1, _server.getTransactionManager().getClientTransactions());

		Assert.assertEquals(0, _transactionManager.getRetries());
	} 

}
