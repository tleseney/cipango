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
package org.cipango.tests.integration;

import static org.cipango.client.test.matcher.SipSessionMatchers.*;
import static org.cipango.client.test.matcher.SipMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.sip.Parameterable;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipApplicationSessionListener;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSessionBindingEvent;
import javax.servlet.sip.SipSessionBindingListener;
import javax.servlet.sip.SipSession.State;
import javax.servlet.sip.SipSessionEvent;
import javax.servlet.sip.SipSessionListener;
import javax.servlet.sip.TooManyHopsException;
import javax.servlet.sip.URI;
import javax.servlet.sip.annotation.SipListener;
import javax.servlet.sip.annotation.SipServlet;

import org.cipango.tests.AbstractServlet;
import org.cipango.tests.MainServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@SipListener
@SipServlet (name="org.cipango.tests.integration.InvalidateWhenReadyTest")
public class InvalidateWhenReadyServlet extends AbstractServlet implements SipSessionListener, SipApplicationSessionListener
{
	
	private static final Logger __logger = LoggerFactory.getLogger(InvalidateWhenReadyServlet.class);
	
	public static final String RECORD_ROUTE = "serverIsRecordRoute";
	private static final String SIP_SESSION_READY_TO_INVALIDATE = "Ready to invalidate listener has not been called for SIP session";
	private static final String APP_SESSION_READY_TO_INVALIDATE = "Ready to invalidate listener has not been called for SIP Application session";
	
	public void testUasCancel(SipServletRequest request) throws TooManyHopsException, IOException
	{
		String method = request.getMethod();
		SipSession session = request.getSession();
		addApplicationSessionListenerTest(session);
		assertTrue(session.isValid());
		if (method.equals("INVITE"))
		{
			assertFalse(session.isReadyToInvalidate());
			assertFalse(session.getApplicationSession().isReadyToInvalidate());
			assertThat(session, hasState(State.INITIAL));
			request.createResponse(SipServletResponse.SC_RINGING).send();
			assertThat(session, hasState(State.EARLY));
		}
		else if (method.equals("CANCEL"))
		{
			assertTrue(session.isReadyToInvalidate());
			assertTrue(session.getApplicationSession().isReadyToInvalidate());
			assertThat(session, hasState(State.TERMINATED));
		}
	}
	
	private void addApplicationSessionListenerTest(SipSession session)
	{
		if (getServletContext().getAttribute(getFailureKey()) == null)
		{
			getServletContext().setAttribute(getFailureKey(), APP_SESSION_READY_TO_INVALIDATE.getBytes());
			session.getApplicationSession().setAttribute(InvalidateWhenReadyServlet.class.getName(), getFailureKey());
		}
	}
	
	private void addSipSessionListenerTest(SipSession session)
	{
		if (getServletContext().getAttribute(getFailureKey()) == null)
		{
			getServletContext().setAttribute(getFailureKey(), SIP_SESSION_READY_TO_INVALIDATE.getBytes());
			session.setAttribute(InvalidateWhenReadyServlet.class.getName(), getFailureKey());
		}
	}
	
	public void testUasBye(SipServletRequest request) throws TooManyHopsException, IOException
	{
		String method = request.getMethod();
		SipSession session = request.getSession();
		addApplicationSessionListenerTest(session);
		assertTrue(session.isValid());
		if (!method.equals("ACK"))
			request.createResponse(SipServletResponse.SC_OK).send();
		
		if (method.equals("INVITE"))
		{
			assertFalse(session.isReadyToInvalidate());
			assertFalse(session.getApplicationSession().isReadyToInvalidate());
			assertThat(session, hasState(State.CONFIRMED));
		}
		else if (method.equals("BYE"))
		{
			assertThat(session, hasState(State.TERMINATED));
			assertTrue(session.isReadyToInvalidate());
			assertTrue(session.getApplicationSession().isReadyToInvalidate());
		}
	}
	
	public void testUac4xx(SipServletRequest request) throws Exception
	{
		SipSession session = request.getSession();
		assertThat(session, hasState(State.INITIAL));
		request.createResponse(SipServletResponse.SC_OK).send();
		assertThat(session, hasState(State.INITIAL)); // State is not changed on non-dialog created request
		Thread.sleep(200);
		
		request.getApplicationSession().invalidate();
		SipServletRequest invite = getSipFactory().createRequest(getSipFactory().createApplicationSession(),
				"INVITE", request.getTo(), request.getFrom());
		session = invite.getSession();
		session.setHandler(getServletName());
		invite.setRequestURI(request.getAddressHeader("Contact").getURI());
		assertFalse(session.isReadyToInvalidate());
		assertFalse(session.getApplicationSession().isReadyToInvalidate());
		assertThat(session, hasState(State.INITIAL));
		invite.send();
	}
	
	public void testUac4xx(SipServletResponse response) throws TooManyHopsException, IOException
	{
		SipSession session = response.getSession();
		if (response.getStatus() < SipServletResponse.SC_OK)
		{
			assertFalse(session.isReadyToInvalidate());
			assertFalse(session.getApplicationSession().isReadyToInvalidate());
			assertThat(session, hasState(State.EARLY));
		}
		else // 4xx
		{
			assertTrue(session.isReadyToInvalidate());
			assertTrue(session.getApplicationSession().isReadyToInvalidate());
			assertThat(session, hasState(State.INITIAL));
		}
	}
	
	public void testUacMessageTcp(SipServletRequest request) throws Exception
	{
		SipServletRequest message = getSipFactory().createRequest(getSipFactory().createApplicationSession(),
				"MESSAGE", request.getTo(), request.getFrom());
		SipSession session = message.getSession();
		session.setHandler(getServletName());
		message.setRequestURI(request.getAddressHeader("Contact").getURI().clone());
		message.getRequestURI().setParameter("transport", "tcp");

		getServletContext().setAttribute(getFailureKey(), "Servlet not invoked with response for testUacMessageTcp");
		message.send();
		
		request.createResponse(SipServletResponse.SC_OK).send();
	}
	
	public void testUacMessageTcp(SipServletResponse response) throws Exception
	{
		getServletContext().removeAttribute(getFailureKey());
		assertThat(response.getSession(), hasState(State.INITIAL));
	}
	
	public void testUacEarlyResponse(SipServletRequest request) throws Exception
	{
		SipSession session = request.getSession();
		assertThat(session, hasState(State.INITIAL));
		request.createResponse(SipServletResponse.SC_RINGING).send();
		
		SipServletRequest message = getSipFactory().createRequest(getSipFactory().createApplicationSession(),
				"MESSAGE", request.getFrom(), request.getTo());
		session = message.getSession();
		session.setAttribute(SipServletRequest.class.getName(),  request);
		session.setHandler(getServletName());
		message.setRequestURI(request.getRequestURI());
		assertFalse(session.isReadyToInvalidate());
		assertFalse(session.getApplicationSession().isReadyToInvalidate());
		assertThat(session, hasState(State.INITIAL));
		message.send();
		session.setAttribute("attribute", new BindingListener());
	}
	
	public void testUacEarlyResponse(SipServletResponse response) throws Exception
	{
		SipSession session = response.getSession();
		SipServletRequest invite = (SipServletRequest) session.getAttribute(SipServletRequest.class.getName());
		try
		{
			assertTrue(session.isReadyToInvalidate());
			assertTrue(session.getApplicationSession().isReadyToInvalidate());
			assertThat(session, hasState(State.INITIAL));
			BindingListener bindingListener = (BindingListener) session.getAttribute("attribute");
			assertNotNull("Attribute not set before response", bindingListener);
			assertThat(bindingListener.getBound(), is(1));
			assertThat(bindingListener.getUnbound(), is(0));
							
			invite.createResponse(SipServletResponse.SC_FORBIDDEN).send();
		}
		catch (Exception e)
		{
			sendError(invite, e);
		}
	}
	
	public void testProxyRecordRoute(SipServletRequest request) throws TooManyHopsException, IOException
	{
		String method = request.getMethod();
		SipSession session = request.getSession();
		addApplicationSessionListenerTest(session);
		assertTrue(session.isValid());
		
		assertFalse(session.isReadyToInvalidate());
		assertFalse(session.getApplicationSession().isReadyToInvalidate());
		if (method.equals("INVITE"))
		{
			Proxy proxy = request.getProxy();
			boolean recordRoute = request.getHeader(RECORD_ROUTE).equals("true");
			proxy.setRecordRoute(recordRoute);
			proxy.setSupervised(true);
			session.setAttribute(RECORD_ROUTE, recordRoute);
			proxy.proxyTo(request.getRequestURI());	
			assertThat(session, hasState(State.INITIAL));
		}
		else if (method.equals("BYE"))
		{
			assertThat(session, hasState(State.CONFIRMED));
		}

	}
	
	public void testProxyRecordRoute(SipServletResponse response) throws TooManyHopsException, IOException
	{
		String method = response.getMethod();
		SipSession session = response.getSession();

		if (method.equals("INVITE"))
		{
			// AS INVITE transaction is in state ACCEPTED, the session cannot be invalidated even if
			// it is not Record-Route
			//boolean recordRoute = (Boolean) session.getAttribute(RECORD_ROUTE);
			assertFalse(session.isReadyToInvalidate());
			assertFalse(session.getApplicationSession().isReadyToInvalidate());	
			assertThat(session, hasState(State.CONFIRMED));
		}
		else if (method.equals("BYE"))
		{
			assertTrue(session.isReadyToInvalidate());
			assertTrue(session.getApplicationSession().isReadyToInvalidate());
			assertThat(session, hasState(State.TERMINATED));
		}
	}

	public void testProxyNoRecordRoute(SipServletRequest request) throws TooManyHopsException, IOException
	{
		testProxyRecordRoute(request);
	}

	public void testProxyNoRecordRoute(SipServletResponse response) throws TooManyHopsException, IOException
	{
		testProxyRecordRoute(response);
	}
	
	public void testProxy4xx(SipServletRequest request) throws TooManyHopsException, IOException
	{
		SipSession session = request.getSession();
		addApplicationSessionListenerTest(session);
		assertTrue(session.isValid());
		
		assertFalse(session.isReadyToInvalidate());
		assertFalse(session.getApplicationSession().isReadyToInvalidate());

		Proxy proxy = request.getProxy();
		proxy.setRecordRoute(true);
		proxy.setSupervised(true);
		proxy.proxyTo(request.getRequestURI());	
		assertThat(session, hasState(State.INITIAL));

	}
	
	public void testProxy4xx(SipServletResponse response) throws TooManyHopsException, IOException
	{
		SipSession session = response.getSession();
		addApplicationSessionListenerTest(session);
		// The session is not in the invalidate when ready state as a new branch can be created in doResponse().
		assertThat(session, hasState(State.INITIAL));
	}
	
	public void testProxySequential(SipServletRequest request) throws TooManyHopsException, IOException
	{
		String method = request.getMethod();
		SipSession session = request.getSession();
		addApplicationSessionListenerTest(session);
		assertTrue(session.isValid());
		
		assertFalse(session.isReadyToInvalidate());
		assertFalse(session.getApplicationSession().isReadyToInvalidate());
		if (method.equals("INVITE"))
		{
			Proxy proxy = request.getProxy();
			proxy.setRecordRoute(true);
			proxy.setSupervised(true);
			proxy.proxyTo(request.getRequestURI());	
			assertThat(session, hasState(State.INITIAL));
		}
		else if (method.equals("BYE"))
		{
			assertThat(session, hasState(State.CONFIRMED));
			assertHasOnlyOneSipSession(session);
		}

	}
	
	private void assertHasOnlyOneSipSession(SipSession session)
	{
		Iterator<?> it = session.getApplicationSession().getSessions();
		List<Object> l = new ArrayList<Object>();
		while (it.hasNext())
			l.add(it.next());
			
		if (l.size() != 1)
		{
			fail("Got sessions " + l + " while expected only one: " + session);
		}
	}
	
	public void testProxySequential(SipServletResponse response) throws Exception
	{
		String method = response.getMethod();
		SipSession session = response.getSession();

		if (method.equals("INVITE"))
		{
			assertFalse(session.isReadyToInvalidate());
			assertFalse(session.getApplicationSession().isReadyToInvalidate());	
			if (response.getStatus() == SipServletResponse.SC_NOT_FOUND)
			{
				assertThat(session, hasState(State.INITIAL));
				URI uri = response.getRequest().getAddressHeader("proxy").getURI();
				response.getProxy().proxyTo(uri);
			}
			else if (response.getStatus() == SipServletResponse.SC_OK)
			{
				assertThat(session, hasState(State.CONFIRMED));
			}
			else
				throw new IllegalAccessException("Unexpected " + response.getStatus() + "/INVITE");
		}
		else if (method.equals("BYE"))
		{
			assertTrue(session.isReadyToInvalidate());
			assertTrue(session.getApplicationSession().isReadyToInvalidate());
			assertThat(session, hasState(State.TERMINATED));
		}
	}
	
	public void testProxyProvisional4xx(SipServletRequest request) throws TooManyHopsException, IOException
	{
		String method = request.getMethod();
		SipSession session = request.getSession();
		addApplicationSessionListenerTest(session);
		assertTrue(session.isValid());
		
		assertFalse(session.isReadyToInvalidate());
		assertFalse(session.getApplicationSession().isReadyToInvalidate());
		if (method.equals("INVITE"))
		{
			Proxy proxy = request.getProxy();
			proxy.setRecordRoute(true);
			proxy.setSupervised(true);
			proxy.proxyTo(request.getRequestURI());	
			assertThat(session, hasState(State.INITIAL));
		}
		else if (method.equals("BYE"))
		{
			assertThat(session, hasState(State.CONFIRMED));
			assertHasOnlyOneSipSession(session);
		}

	}
	
	public void testProxyProvisional4xx(SipServletResponse response) throws Exception
	{
		String method = response.getMethod();
		SipSession session = response.getSession();

		if (method.equals("INVITE"))
		{
			assertFalse(session.isReadyToInvalidate());
			assertFalse(session.getApplicationSession().isReadyToInvalidate());	
			//assertEquals("In sequential mode, no clone should be done", response.getRequest().getSession(), session);
			int status = response.getStatus();
			if (status == SipServletResponse.SC_RINGING)
			{
				assertThat(session, hasState(State.EARLY));
			}
			else if (status == SipServletResponse.SC_NOT_FOUND)
			{
				assertThat(session, hasState(State.INITIAL));// FIXME EARLY or INITIAL ????
				URI uri = response.getRequest().getAddressHeader("proxy").getURI();
				response.getProxy().proxyTo(uri);
			}
			else if (status == SipServletResponse.SC_OK)
			{
				assertThat(session, hasState(State.CONFIRMED));
			}
			else
				throw new IllegalAccessException("Unexpected " + status + "/INVITE");
		}
		else if (method.equals("BYE"))
		{
			assertTrue(session.isReadyToInvalidate());
			// The app session may not be ready to invalidate as the derived session is not in ready
			// to invalidate state.
			assertThat(session, hasState(State.TERMINATED));
		}
	}
	
	public void testProxyParallel(SipServletRequest request) throws Exception
	{
		String method = request.getMethod();
		SipSession session = request.getSession();
		addApplicationSessionListenerTest(session);
		assertTrue(session.isValid());
		
		assertFalse(session.isReadyToInvalidate());
		assertFalse(session.getApplicationSession().isReadyToInvalidate());
		if (method.equals("INVITE"))
		{
			Proxy proxy = request.getProxy();
			proxy.setRecordRoute(true);
			proxy.setSupervised(true);
			proxy.setParallel(true);
			List<URI> l = new ArrayList<URI>();
			l.add(request.getRequestURI());
			l.add(request.getAddressHeader("proxy").getURI());
			proxy.proxyTo(l);	
			assertThat(session, hasState(State.INITIAL));
		}
		else if (method.equals("BYE"))
		{
			assertThat(session, hasState(State.CONFIRMED));
			assertHasOnlyOneSipSession(session);
		}
	}
	
	public void testProxyParallel(SipServletResponse response) throws Exception
	{
		String method = response.getMethod();
		SipSession session = response.getSession();

		if (method.equals("INVITE"))
		{
			assertThat(session, isNotReadyToInvalidate());
			assertFalse(session.getApplicationSession().isReadyToInvalidate());	
			int status = response.getStatus();
			if (status == SipServletResponse.SC_RINGING)
			{
				assertThat(session, hasState(State.EARLY));
			}
			else if (status == SipServletResponse.SC_NOT_FOUND)
			{
				assertThat(session, hasState(State.INITIAL)); // FIXME state initial or early ? ?
				response.getApplicationSession().setAttribute("received 404", true);
			}
			else if (status == SipServletResponse.SC_OK)
			{
				assertThat(session, hasState(State.CONFIRMED));
				assertNotNull(response.getApplicationSession().getAttribute("received 404"));
				
				// The others derived sessions should be now invalidated.
				
			}
			else
				throw new IllegalAccessException("Unexpected " + status + "/INVITE");
		}
		else if (method.equals("BYE"))
		{
			assertThat(session, isReadyToInvalidate());
			// The app session may not be ready to invalidate as the derived session is not in ready
			// to invalidate state.
			assertThat(session.getApplicationSession().isReadyToInvalidate(), is(true));
			assertThat(session, hasState(State.TERMINATED));
		}
	}
	
	public void testProxyCancel1(SipServletRequest request) throws Exception
	{
		String method = request.getMethod();
		SipSession session = request.getSession();
		addApplicationSessionListenerTest(session);
		assertTrue(session.isValid());
		
		assertFalse(session.isReadyToInvalidate());
		assertFalse(session.getApplicationSession().isReadyToInvalidate());
		if (method.equals("INVITE"))
		{
			Proxy proxy = request.getProxy();
			proxy.setRecordRoute(true);
			proxy.setSupervised(true);
			proxy.setParallel(true);
			List<URI> l = new ArrayList<URI>();
			l.add(request.getRequestURI());
			l.add(request.getAddressHeader("proxy").getURI());
			proxy.proxyTo(l);	
			assertThat(session, hasState(State.INITIAL));
		}
		else if (method.equals("BYE"))
		{
			assertThat(session, hasState(State.CONFIRMED));
			assertHasOnlyOneSipSession(session);
		}
	}
	
	public void testProxyCancel1(SipServletResponse response) throws Exception
	{
		String method = response.getMethod();
		SipSession session = response.getSession();

		if (method.equals("INVITE"))
		{
			assertFalse(session.isReadyToInvalidate());
			assertFalse(session.getApplicationSession().isReadyToInvalidate());	
			int status = response.getStatus();
			if (status == SipServletResponse.SC_RINGING)
			{
				assertThat(session, hasState(State.EARLY));
			}
			else if (status == SipServletResponse.SC_OK)
			{
				assertThat(session, hasState(State.CONFIRMED));
			}
			else
				throw new IllegalAccessException("Unexpected " + status + "/INVITE");
		}
		else if (method.equals("BYE"))
		{
			assertTrue(session.isReadyToInvalidate());
			// The app session may not be ready to invalidate as the derived session is not in ready
			// to invalidate state.
			assertThat(session, hasState(State.TERMINATED));
		}
	}
	
	public void testProxyCancel2(SipServletRequest request) throws Exception
	{
		testProxyCancel1(request);
	}
	
	public void testProxyCancel2(SipServletResponse response) throws Exception
	{
		testProxyCancel1(response);
	}
	
	public void testMessage(SipServletRequest request) throws Throwable 
	{
		SipSession session = request.getSession();
		assertFalse(session.isReadyToInvalidate());
		assertFalse(session.getApplicationSession().isReadyToInvalidate());
		assertThat(session, hasState(State.INITIAL));
		request.createResponse(SipServletResponse.SC_OK).send();
	}
	
	public void testTwoProxy(SipServletRequest request) throws Throwable
	{
		String method = request.getMethod();
		SipSession session = request.getSession();
		addSipSessionListenerTest(session);
		assertTrue(session.isValid());
		
		assertFalse(session.isReadyToInvalidate());
		assertFalse(session.getApplicationSession().isReadyToInvalidate());
		if (method.equals("INVITE"))
		{
			Proxy proxy = request.getProxy();
			proxy.setRecordRoute(true);
			proxy.setSupervised(true);
			request.pushRoute(getOwnUri(request));
			request.setHeader(MainServlet.SERVLET_HEADER, ProxyTwoServlet.class.getName());
			proxy.proxyTo(request.getRequestURI());	
			assertThat(session, hasState(State.INITIAL));
		}
		else if (method.equals("BYE"))
		{
			assertThat(session, hasState(State.CONFIRMED));
		}
	}
	
	public void testTwoProxy(SipServletResponse response) throws Exception
	{
		String method = response.getMethod();
		SipSession session = response.getSession();

		if (method.equals("INVITE"))
		{
			assertFalse(session.isReadyToInvalidate());
			assertFalse(session.getApplicationSession().isReadyToInvalidate());	
			if (response.getStatus() == SipServletResponse.SC_RINGING)
			{
				assertThat(session, hasState(State.EARLY));
			}
			else if (response.getStatus() == SipServletResponse.SC_OK)
			{
				assertThat(session, hasState(State.CONFIRMED));
			}
			else
				throw new IllegalAccessException("Unexpected " + response.getStatus() + "/INVITE");
		}
		else if (method.equals("BYE"))
		{
			assertTrue(session.isReadyToInvalidate());
			assertThat(session, hasState(State.TERMINATED));
		}
	}
	
	public void testSubscribe(SipServletRequest request) throws Throwable
	{
		SipSession session = request.getSession();
		assertFalse(session.isReadyToInvalidate());
		assertFalse(session.getApplicationSession().isReadyToInvalidate());
		assertThat(session, hasState(State.INITIAL));
		request.createResponse(SipServletResponse.SC_OK).send();
		Thread.sleep(50);
		assertThat(session, hasState(State.CONFIRMED));
		SipServletRequest notify = session.createRequest("NOTIFY");
		notify.addHeader("Subscription-State", "terminated");
		notify.send();
	}

	public void testSubscribe(SipServletResponse response) throws Throwable
	{
		SipSession session = response.getSession();
		assertTrue(session.isReadyToInvalidate());
		assertTrue(session.getApplicationSession().isReadyToInvalidate());
	}
	
	public void testSubscribe2(SipServletRequest request) throws Throwable
	{
		SipSession session = request.getSession();
		assertFalse(session.isReadyToInvalidate());
		assertFalse(session.getApplicationSession().isReadyToInvalidate());
		
		if (request.isInitial())
			assertThat(session, hasState(State.INITIAL));
		else
			assertThat(session, hasState(State.CONFIRMED));
		SipServletResponse response = request.createResponse(SipServletResponse.SC_OK);
		response.setExpires(request.getExpires());
		response.send();
		Thread.sleep(50);

		SipServletRequest notify = session.createRequest("NOTIFY");
		if (request.getExpires() > 0)
			notify.addHeader("Subscription-State", "active;expires=" + request.getExpires());
		else
			notify.addHeader("Subscription-State", "terminated");
		notify.send();
	}

	public void testSubscribe2(SipServletResponse response) throws Throwable
	{
		SipSession session = response.getSession();
		Parameterable p = response.getRequest().getParameterableHeader("Subscription-State");
		if ("terminated".equals(p.getValue()))
		{
			assertTrue(session.isReadyToInvalidate());
			assertTrue(session.getApplicationSession().isReadyToInvalidate());
		}
		else
		{
			assertFalse(session.isReadyToInvalidate());
			assertFalse(session.getApplicationSession().isReadyToInvalidate());
		}
	}
	
	public void testUacSubscribe(SipServletRequest request) throws Throwable
	{
		SipSession session = request.getSession();
		if ("REGISTER".equals(request.getMethod()))
		{		
			request.createResponse(SipServletResponse.SC_OK).send();
			request.getApplicationSession().invalidate();
			SipServletRequest subscribe = getSipFactory().createRequest(getSipFactory().createApplicationSession(),
					"SUBSCRIBE", request.getTo(), request.getFrom());
			session = subscribe.getSession();
			session.setHandler(getServletName());
			subscribe.setRequestURI(request.getAddressHeader("Contact").getURI());
			subscribe.setExpires(60);
			subscribe.setHeader("Event", "presence");
			subscribe.send();
		} 
		else if ("NOTIFY".equals(request.getMethod()))
		{
			request.createResponse(SipServletResponse.SC_OK).send();
			if (session.getAttribute("first") == null)
			{
				__logger.info("First NOTIFY");
				session.setAttribute("first", "");
				SipServletRequest subscribe = session.createRequest("SUBSCRIBE");
				subscribe.setExpires(0);
				subscribe.setHeader("Event", "presence");
				subscribe.send();
				
				assertFalse(session.isReadyToInvalidate());
				assertFalse(session.getApplicationSession().isReadyToInvalidate());
			}
			else
			{
				__logger.info("Final NOTIFY");
				assertTrue(session.isReadyToInvalidate());
				assertTrue(session.getApplicationSession().isReadyToInvalidate());
			}
					
		}
		else
			fail("Unexpected request: " + request.getMethod());
	}

	public void testUacSubscribe(SipServletResponse response) throws Throwable
	{
		SipSession session = response.getSession();
		assertThat(response, isSuccess());
		
		if (session.getAttribute("first") == null)
		{
			assertFalse(session.isReadyToInvalidate());
			assertFalse(session.getApplicationSession().isReadyToInvalidate());
		}
	}

	
	public void sessionCreated(SipSessionEvent e)
	{
	}

	public void sessionDestroyed(SipSessionEvent e)
	{
		//System.out.println("sessionDestroyed");
		//new Exception("sessionDestroyed").printStackTrace();
	}

	public void sessionReadyToInvalidate(SipSessionEvent e)
	{
		try
		{
			String key = (String) e.getSession().getAttribute(InvalidateWhenReadyServlet.class.getName());
			if (key != null)
			{
				byte[] c = (byte[]) getServletContext().getAttribute(key);
				if (c != null && SIP_SESSION_READY_TO_INVALIDATE.equals(new String(c)))
					getServletContext().removeAttribute(key);
			}
		}
		catch (Throwable e1) 
		{
			__logger.warn("Failed to handle sessionReadyToInvalidate", e1);
		}
	}

	public void sessionCreated(SipApplicationSessionEvent e)
	{
	}

	public void sessionDestroyed(SipApplicationSessionEvent e)
	{
	}

	public void sessionExpired(SipApplicationSessionEvent e)
	{
	}

	public void sessionReadyToInvalidate(SipApplicationSessionEvent e)
	{
		try
		{
			String key = (String) e.getApplicationSession().getAttribute(InvalidateWhenReadyServlet.class.getName());
			if (key != null)
			{
				byte[] c = (byte[]) getServletContext().getAttribute(key);
				if (c != null && APP_SESSION_READY_TO_INVALIDATE.equals(new String(c)))
					getServletContext().removeAttribute(key);
			}
		}
		catch (Throwable e1) 
		{
			__logger.warn("Failed to handle sessionReadyToInvalidate", e1);
		}
	}

	
	static class BindingListener implements SipSessionBindingListener
	{
		private int _bound = 0;
		private int _unbound = 0;
		
		public void valueUnbound(SipSessionBindingEvent event)
		{	
			_unbound++;
		}
		
		public void valueBound(SipSessionBindingEvent arg0)
		{
			try { Thread.sleep(500); } catch (InterruptedException e){}
			_bound++;
		}

		public int getBound()
		{
			return _bound;
		}

		public int getUnbound()
		{
			return _unbound;
		}
	}
}
