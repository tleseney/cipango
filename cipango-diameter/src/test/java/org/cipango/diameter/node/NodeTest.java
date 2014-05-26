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
package org.cipango.diameter.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import org.cipango.diameter.AVP;
import org.cipango.diameter.AVPList;
import org.cipango.diameter.api.DiameterFactory;
import org.cipango.diameter.api.DiameterServletAnswer;
import org.cipango.diameter.api.DiameterServletRequest;
import org.cipango.diameter.api.DiameterSession;
import org.cipango.diameter.app.DiameterContext;
import org.cipango.diameter.base.Common;
import org.cipango.diameter.base.Common.AuthSessionState;
import org.cipango.diameter.ims.Cx;
import org.cipango.diameter.ims.Sh;
import org.cipango.diameter.ims.Sh.DataReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NodeTest
{
	private Node _client;
	private Node _server;
	private Peer _peer;

	@Before
	public void setUp() throws Exception
	{
		//Log.getLog().setDebugEnabled(true);
		_client = new Node(38681);
		_client.getConnectors()[0].setHost("127.0.0.1");
		_client.setIdentity("client");
		
		_peer = new Peer("server");
		_peer.setAddress(InetAddress.getByName("127.0.0.1"));
		_peer.setPort(38680);
		_client.addPeer(_peer);
		
		_server = new Node(38680);
		_server.getConnectors()[0].setHost("127.0.0.1");
		_server.setIdentity("server");
	}

	@After
	public void tearDown() throws Exception
	{
		_server.stop();
		_client.stop();
		Thread.sleep(10);
	}
	
	@Test
	public void testConnect() throws Exception
	{
		//org.eclipse.jetty.util.log.Log.getLog().setDebugEnabled(true);
		
		_server.start();
		
		_client.start();
		
		waitPeerOpened();
		
		Peer clientPeer = _server.getPeer("client");
		assertNotNull(clientPeer);
		assertTrue(clientPeer.isOpen());
		
		_peer.stop();
		Thread.sleep(100);
		assertTrue(_peer.isClosed());
		assertTrue(clientPeer.isClosed());
	}
	
	@Test
	public void testUdr() throws Throwable
	{
		//Log.getLog().setDebugEnabled(true);
		
		TestDiameterHandler serverHandler = new TestDiameterHandler()
		{

			@Override
			public void doHandle(DiameterMessage message) throws Throwable
			{
				DiameterServletAnswer uda;
				DiameterServletRequest request = (DiameterServletRequest) message;

				assertEquals(true, message.isRequest());
				assertEquals(Sh.UDR, request.getCommand());
				assertEquals(request.getApplicationId(), Sh.SH_APPLICATION_ID.getId());
				assertEquals(request.getDestinationHost(), "server");
				uda = request.createAnswer(Common.DIAMETER_SUCCESS);
				uda.send();
			}
			
		};
		_server.setHandler(serverHandler);
		_server.start();
		
		TestDiameterHandler clientHandler = new TestDiameterHandler()
		{
			
			@Override
			public void doHandle(DiameterMessage message) throws Throwable
			{
				DiameterServletAnswer uda = (DiameterServletAnswer) message;
	
				assertFalse(message.isRequest());
				assertEquals(Sh.UDA, uda.getCommand());
				assertEquals(Common.DIAMETER_SUCCESS, uda.getResultCode());
				assertEquals(uda.getApplicationId(), Sh.SH_APPLICATION_ID.getId());

			}
		};
		_client.setHandler(clientHandler);
		_client.start();
		
		waitPeerOpened();
				
		newUdr().send();
		serverHandler.assertDone();
		clientHandler.assertDone();
	}
	
	@Test
	public void testTimeout() throws Throwable
	{
		//Log.getLog().setDebugEnabled(true);
		
		TestDiameterHandler serverHandler = new TestDiameterHandler()
		{

			@Override
			public void doHandle(DiameterMessage message) throws Throwable
			{
				Thread.sleep(300);
				((DiameterServletRequest) message).createAnswer(Common.DIAMETER_SUCCESS).send();
			}
			
		};
		_server.setHandler(serverHandler);
		_server.start();
		
		TestDiameterHandler clientHandler = new TestDiameterHandler()
		{
			@Override
			public void doHandle(DiameterMessage message) throws Throwable
			{
			}
		};
		_client.setHandler(clientHandler);
		_client.setRequestTimeout(200);
		_client.start();
		
		waitPeerOpened();
				
		newUdr().send();
		assertEquals(1, clientHandler.waitNoAnswer());
		serverHandler.assertDone();
		Thread.sleep(150);
		clientHandler.assertDone(0);
		assertTrue(_peer.isOpen());
	}
	
	
	@Test
	public void testRedirectDefaultPort() throws Throwable
	{
		testRedirect(3868, Arrays.asList("aaa://localhost"));
	}
	
	@Test
	public void testRedirectCustomPort() throws Throwable
	{
		testRedirect(38682, Arrays.asList("aaa://localhost:38682"));
	}
	
	@Test
	public void testRedirectBadFirstHost() throws Throwable
	{
		testRedirect(38682, Arrays.asList("aaa://invalid", "aaa://localhost:38682"));
	}
		
	public void testRedirect(final int port, final List<String> redirectHosts) throws Throwable
	{
		Node server2 = null;
		try
		{
			TestDiameterHandler redirectHandler = new TestDiameterHandler()
			{
	
				@Override
				public void doHandle(DiameterMessage message) throws Throwable
				{
					DiameterServletAnswer uda;
					DiameterServletRequest request = (DiameterServletRequest) message;
	
					assertEquals(true, message.isRequest());
					assertEquals(Sh.UDR, request.getCommand());
					uda = request.createAnswer(Common.DIAMETER_REDIRECT_INDICATION);
					for (String uri : redirectHosts)
						uda.add(Common.REDIRECT_HOST, uri);
					uda.send();
				}
				
			};
			_server.setHandler(redirectHandler);
			_server.start();
			
			
			server2 = new Node(port);
			server2.getConnectors()[0].setHost("127.0.0.1");
			server2.setIdentity("localhost");
			TestDiameterHandler serverHandler = new TestDiameterHandler()
			{
	
				@Override
				public void doHandle(DiameterMessage message) throws Throwable
				{
					DiameterServletAnswer uda;
					DiameterServletRequest request = (DiameterServletRequest) message;
	
					assertEquals(true, message.isRequest());
					assertEquals(Sh.UDR, request.getCommand());
					assertEquals(request.getApplicationId(), Sh.SH_APPLICATION_ID.getId());
					assertEquals(request.getDestinationHost(), "server");
					uda = request.createAnswer(Common.DIAMETER_SUCCESS);
					uda.send();
				}
				
			};
			server2.setHandler(serverHandler);
			server2.start();
			
			TestDiameterHandler clientHandler = new TestDiameterHandler()
			{
				
				@Override
				public void doHandle(DiameterMessage message) throws Throwable
				{
					DiameterServletAnswer uda = (DiameterServletAnswer) message;
		
					assertFalse(message.isRequest());
					assertEquals(Sh.UDA, uda.getCommand());
					assertEquals(Common.DIAMETER_SUCCESS, uda.getResultCode());
					assertEquals(uda.getApplicationId(), Sh.SH_APPLICATION_ID.getId());
	
				}
			};
			_client.setHandler(clientHandler);
			_client.start();
			
			waitPeerOpened();
					
			newUdr().send();
			redirectHandler.assertDone();
			clientHandler.assertDone();
		}
		finally
		{
			if (server2 != null)
				server2.stop();
		}
	}
	
	/**
	 * Ensure that a new DiameterErrorEvent is thrown if unable to send the request after a redirect.
	 * @throws Throwable
	 */
	@Test
	public void testRedirectBad() throws Throwable
	{
		Node server2 = null;
		try
		{
			TestDiameterHandler redirectHandler = new TestDiameterHandler()
			{
	
				@Override
				public void doHandle(DiameterMessage message) throws Throwable
				{
					DiameterServletAnswer uda;
					DiameterServletRequest request = (DiameterServletRequest) message;
	
					assertEquals(true, message.isRequest());
					assertEquals(Sh.UDR, request.getCommand());
					uda = request.createAnswer(Common.DIAMETER_REDIRECT_INDICATION);
					uda.add(Common.REDIRECT_HOST, "aaa://invalid");
					uda.send();
				}
				
			};
			_server.setHandler(redirectHandler);
			_server.start();
			
						
			TestDiameterHandler clientHandler = new TestDiameterHandler()
			{
				@Override
				public void doHandle(DiameterMessage message) throws Throwable
				{
				}
			};
			_client.setHandler(clientHandler);
			_client.start();
			
			waitPeerOpened();
					
			
			newUdr().send();
			redirectHandler.assertDone();
			clientHandler.assertDone(0);
			assertEquals(1, clientHandler.waitNoAnswer());
		}
		finally
		{
			if (server2 != null)
				server2.stop();
		}
	}
	
	private DiameterRequest newUdr()
	{
		DiameterRequest udr = new DiameterRequest(_client, Sh.UDR, Sh.SH_APPLICATION_ID.getId(), _client.getSessionManager().newSessionId());
		udr.getAVPs().add(Common.DESTINATION_REALM, "server");
		udr.getAVPs().add(Common.DESTINATION_HOST, "server");
		udr.getAVPs().add(Sh.DATA_REFERENCE, DataReference.SCSCFName);
		AVP<AVPList> userIdentity = new AVP<AVPList>(Sh.USER_IDENTITY, new AVPList());
        userIdentity.getValue().add(Cx.PUBLIC_IDENTITY, "sip:alice@cipango.org");
		udr.getAVPs().add(userIdentity);
		udr.getAVPs().add(Common.AUTH_SESSION_STATE, AuthSessionState.NO_STATE_MAINTAINED);
		udr.getSession();
		return udr;
	}
	
	protected DiameterFactory createFactory(Node node)
	{
		DiameterFactoryImpl factory = new DiameterFactoryImpl();
		factory.setNode(node);
		return factory;
	}
	
	@Test
	public void testDiameterFactory() throws Throwable
	{
		//Log.getLog().setDebugEnabled(true);
		
		TestDiameterHandler serverHandler = new TestDiameterHandler()
		{

			@Override
			public void doHandle(DiameterMessage message) throws Throwable
			{
				DiameterServletAnswer uda;
				DiameterServletRequest request = (DiameterServletRequest) message;

				assertEquals(true, message.isRequest());
				assertEquals(Sh.UDR, request.getCommand());
				assertEquals(request.getApplicationId(), Sh.SH_APPLICATION_ID.getId());
				assertEquals(request.getDestinationHost(), "server");
				uda = request.createAnswer(Common.DIAMETER_SUCCESS);
				uda.send();
			}
			
		};
		_server.setHandler(serverHandler);
		_server.start();
		
		TestDiameterHandler clientHandler = new TestDiameterHandler()
		{
			
			@Override
			public void doHandle(DiameterMessage message) throws Throwable
			{
				DiameterServletAnswer uda = (DiameterServletAnswer) message;
	
				assertFalse(message.isRequest());
				assertEquals(Sh.UDA, uda.getCommand());
				assertEquals(uda.getApplicationId(), Sh.SH_APPLICATION_ID.getId());

			}
		};
		_client.setHandler(clientHandler);
		_client.start();
		
		waitPeerOpened();
		

		DiameterFactory clientFactory = createFactory(_client);
		DiameterServletRequest udr = clientFactory.createRequest(null, Sh.SH_APPLICATION_ID, Sh.UDR, "server");
		
		udr.add(Common.DESTINATION_HOST, "server");
		udr.getAVPs().add(Sh.DATA_REFERENCE, DataReference.SCSCFName);
		AVP<AVPList> userIdentity = new AVP<AVPList>(Sh.USER_IDENTITY, new AVPList());
        userIdentity.getValue().add(Cx.PUBLIC_IDENTITY, "sip:alice@cipango.org");
		udr.getAVPs().add(userIdentity);
		udr.getAVPs().add(Common.AUTH_SESSION_STATE, AuthSessionState.NO_STATE_MAINTAINED);
		udr.getSession();
		udr.send();
		serverHandler.assertDone();
		clientHandler.assertDone();
	}
	
	private void waitPeerOpened()
	{
		int i = 50;
		while (i != 0)
		{
			if (_peer.isOpen())
				return;
			try { Thread.sleep(20); } catch (InterruptedException e) {}
			i++;
		}
		assertTrue(_peer.isOpen());
	}
	
	@Test
	public void testSession() throws Throwable
	{
		//Log.getLog().setDebugEnabled(true);
		
		TestDiameterHandler serverHandler = new TestDiameterHandler()
		{
			private String _sessionId;
			private DiameterSession _session;
			
			@Override
			public void doHandle(DiameterMessage message) throws Throwable
			{
				if (message instanceof DiameterServletAnswer)
				{
					assertEquals(Sh.PNA, message.getCommand());
					assertEquals(_sessionId, message.getSessionId());
					assertEquals(_session, message.getSession());
				}
				else
				{
					DiameterServletAnswer sna;
					DiameterServletRequest request = (DiameterServletRequest) message;
	
					assertEquals(true, message.isRequest());
					assertEquals(Sh.SNR, request.getCommand());
					assertEquals(request.getApplicationId(), Sh.SH_APPLICATION_ID.getId());
					assertEquals(request.getDestinationHost(), "server");
					sna = request.createAnswer(Common.DIAMETER_SUCCESS);
					_sessionId = request.getSessionId();
					assertNotNull(_sessionId);
					_session = request.getSession();
					assertNotNull(_session);
					sna.send();
					
					Thread.sleep(50);
					DiameterServletRequest pnr = _session.createRequest(Sh.PNR, true);
					pnr.send();
				}
			}
			
		};
		_server.setHandler(serverHandler);
		_server.start();
		
		TestDiameterHandler clientHandler = new TestDiameterHandler()
		{
			private String _sessionId;
			private DiameterSession _session;
			
			@Override
			public void doHandle(DiameterMessage message) throws Throwable
			{
				if (message instanceof DiameterServletAnswer)
				{
					DiameterServletAnswer sna = (DiameterServletAnswer) message;
					assertEquals(Sh.SNA, sna.getCommand());
					assertEquals(sna.getApplicationId(), Sh.SH_APPLICATION_ID.getId());
					_sessionId = sna.getSessionId();
					_session = sna.getSession();
					assertNotNull(_sessionId);
					assertNotNull(_session);
					assertEquals(_sessionId, sna.getRequest().getSessionId());
				}
				else
				{
					DiameterServletRequest pnr = (DiameterServletRequest) message;
					assertEquals(Sh.PNR, pnr.getCommand());
					assertEquals(_sessionId, pnr.getSessionId());
					assertEquals(_session, pnr.getSession());
					pnr.createAnswer(Common.DIAMETER_SUCCESS).send();
				}
			}
		};
		_client.setHandler(clientHandler);
		_client.start();
		
		waitPeerOpened();
		
		String id = _client.getSessionManager().newSessionId();
		DiameterRequest snr = new DiameterRequest(_client, Sh.SNR, Sh.SH_APPLICATION_ID.getId(), id);
		snr.add(Common.DESTINATION_REALM, "server");
		snr.add(Common.DESTINATION_HOST, "server");
		snr.add(Sh.DATA_REFERENCE, DataReference.SCSCFName);
		AVP<AVPList> userIdentity = new AVP<AVPList>(Sh.USER_IDENTITY, new AVPList());
        userIdentity.getValue().add(Cx.PUBLIC_IDENTITY, "sip:alice@cipango.org");
		snr.getAVPs().add(userIdentity);
		snr.add(Common.AUTH_SESSION_STATE, AuthSessionState.NO_STATE_MAINTAINED);
		snr.getAVPs().add(Sh.SH_APPLICATION_ID.getAVP());
		
		snr.send();
		
		serverHandler.assertDone(2);
		clientHandler.assertDone(2);
	}
		
	public static abstract class TestDiameterHandler extends DiameterContext
	{
		private Throwable _e;
		private AtomicInteger _msgReceived = new AtomicInteger(0);
		private int _nbNoAnswer = 0;	
		
		@Override
		public void handle(DiameterMessage message)
		{
			try
			{
				doHandle(message);
			}
			catch (Throwable e)
			{
				e.printStackTrace();
				_e = e;
			}
			finally
			{
				_msgReceived.incrementAndGet();
				synchronized (_msgReceived)
				{
					_msgReceived.notify();
				}
			}
		}
		
		public abstract void doHandle(DiameterMessage message) throws Throwable;
		
		@Override
		public void fireNoAnswerReceived(DiameterRequest request, long timeout)
		{
			_nbNoAnswer++;
			synchronized (this)
			{
				notify();
			}
		}
		public void assertDone() throws Throwable
		{
			assertDone(1);
		}
		
		public void assertDone(int msgExpected) throws Throwable
		{
			if (_e != null)
				throw _e;
			
			long end = System.currentTimeMillis() + 5000;
			
			synchronized (_msgReceived)
			{
				while (end > System.currentTimeMillis() && _msgReceived.get() < msgExpected)
				{
					try
					{
						_msgReceived.wait(end - System.currentTimeMillis());
					}
					catch (InterruptedException e)
					{
					}
				}
			}
			if (_e != null)
				throw _e;
			if (_msgReceived.get() != msgExpected)
				Assert.fail("Received " + _msgReceived + " messages when expected " + msgExpected);
		}
		
		public int waitNoAnswer()
		{
			synchronized (this)
			{
				try
				{
					wait(5000);
				}
				catch (InterruptedException e)
				{
				}
			}
			return _nbNoAnswer;
		}

		public int getNbNoAnswer()
		{
			return _nbNoAnswer;
		}
	}
	
}
