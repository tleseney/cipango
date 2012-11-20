// ========================================================================
// Copyright 2007-2008 NEXCOM Systems
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
package org.cipango.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSession.State;
import javax.servlet.sip.SipSessionEvent;
import javax.servlet.sip.SipSessionListener;
import javax.servlet.sip.TooManyHopsException;
import javax.servlet.sip.URI;
import javax.servlet.sip.annotation.SipListener;
import javax.servlet.sip.annotation.SipServlet;

import org.cipango.test.common.AbstractServlet;
import org.cipango.test.common.MainServlet;

@SuppressWarnings("serial")
@SipListener
@SipServlet (name="org.cipango.sipunit.test.InvalidateWhenReadyTest")
public class InvalidateWhenReadyServlet extends AbstractServlet implements SipSessionListener
{
	public static final String RECORD_ROUTE = "serverIsRecordRoute";
	private static final String LISTENER_TEST = "Ready to invalidate listener has not been called";
	
	public void testUasCancel(SipServletRequest request) throws TooManyHopsException, IOException
	{
		String method = request.getMethod();
		SipSession session = request.getSession();
		addListenerTest(session);
		assertTrue(session.isValid());
		if (method.equals("INVITE"))
		{
			assertFalse(session.isReadyToInvalidate());
			assertFalse(session.getApplicationSession().isReadyToInvalidate());
			assertEquals(State.INITIAL, session.getState());
			request.createResponse(SipServletResponse.SC_RINGING).send();
			assertEquals(State.EARLY, session.getState());
		}
		else if (method.equals("CANCEL"))
		{
			assertTrue(session.isReadyToInvalidate());
			assertTrue(session.getApplicationSession().isReadyToInvalidate());
			assertEquals(State.TERMINATED, session.getState());
		}
	}
	
	private void addListenerTest(SipSession session)
	{
		if (getServletContext().getAttribute(getFailureKey()) == null)
		{
			getServletContext().setAttribute(getFailureKey(), LISTENER_TEST.getBytes());
			session.setAttribute(InvalidateWhenReadyServlet.class.getName(), getFailureKey());
		}
	}
	
	public void testUasBye(SipServletRequest request) throws TooManyHopsException, IOException
	{
		String method = request.getMethod();
		SipSession session = request.getSession();
		addListenerTest(session);
		assertTrue(session.isValid());
		if (!method.equals("ACK"))
			request.createResponse(SipServletResponse.SC_OK).send();
		
		if (method.equals("INVITE"))
		{
			assertFalse(session.isReadyToInvalidate());
			assertFalse(session.getApplicationSession().isReadyToInvalidate());
			assertEquals(State.CONFIRMED, session.getState());
		}
		else if (method.equals("BYE"))
		{
			assertTrue(session.isReadyToInvalidate());
			assertTrue(session.getApplicationSession().isReadyToInvalidate());
			assertEquals(State.TERMINATED, session.getState());
		}
	}
	
	public void testUac4xx(SipServletRequest request) throws Exception
	{

		assertEquals(State.INITIAL, request.getSession().getState());
		request.createResponse(SipServletResponse.SC_OK).send();
		assertEquals(State.INITIAL, request.getSession().getState()); // State is not changed on non-dialog created request
		Thread.sleep(200);
		
		request.getApplicationSession().invalidate();
		SipServletRequest invite = getSipFactory().createRequest(getSipFactory().createApplicationSession(),
				"INVITE", request.getTo(), request.getFrom());
		SipSession session = invite.getSession();
		session.setHandler(getServletName());
		invite.setRequestURI(request.getAddressHeader("Contact").getURI());
		assertFalse(session.isReadyToInvalidate());
		assertFalse(session.getApplicationSession().isReadyToInvalidate());
		assertEquals(State.INITIAL, session.getState());
		invite.send();
	}
	
	public void testUac4xx(SipServletResponse response) throws TooManyHopsException, IOException
	{
		SipSession session = response.getSession();
		if (response.getStatus() < SipServletResponse.SC_OK)
		{
			assertFalse(session.isReadyToInvalidate());
			assertFalse(session.getApplicationSession().isReadyToInvalidate());
			assertEquals(State.EARLY, session.getState());
		}
		else // 4xx
		{
			assertTrue(session.isReadyToInvalidate());
			assertTrue(session.getApplicationSession().isReadyToInvalidate());
			assertEquals(State.INITIAL, session.getState());
		}
	}
	
	public void testProxyRecordRoute(SipServletRequest request) throws TooManyHopsException, IOException
	{
		String method = request.getMethod();
		SipSession session = request.getSession();
		addListenerTest(session);
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
			assertEquals(State.INITIAL, session.getState());
		}
		else if (method.equals("BYE"))
		{
			assertEquals(State.CONFIRMED, session.getState());
		}

	}
	
	public void testProxyRecordRoute(SipServletResponse response) throws TooManyHopsException, IOException
	{
		String method = response.getMethod();
		SipSession session = response.getSession();
		addListenerTest(session);

		if (method.equals("INVITE"))
		{
			boolean recordRoute = (Boolean) session.getAttribute(RECORD_ROUTE);
			assertEquals(!recordRoute, session.isReadyToInvalidate());
			assertEquals(!recordRoute, session.getApplicationSession().isReadyToInvalidate());	
			assertEquals(State.CONFIRMED, session.getState());
		}
		else if (method.equals("BYE"))
		{
			assertTrue(session.isReadyToInvalidate());
			assertTrue(session.getApplicationSession().isReadyToInvalidate());
			assertEquals(State.TERMINATED, session.getState());
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
		addListenerTest(session);
		assertTrue(session.isValid());
		
		assertFalse(session.isReadyToInvalidate());
		assertFalse(session.getApplicationSession().isReadyToInvalidate());

		Proxy proxy = request.getProxy();
		proxy.setRecordRoute(true);
		proxy.setSupervised(true);
		proxy.proxyTo(request.getRequestURI());	
		assertEquals(State.INITIAL, session.getState());

	}
	
	public void testProxy4xx(SipServletResponse response) throws TooManyHopsException, IOException
	{
		SipSession session = response.getSession();
		addListenerTest(session);
		// The session is not in the invalidate when ready state as a new branch can be created in doResponse().
		assertEquals(State.INITIAL, session.getState());
	}
	
	public void testProxySequential(SipServletRequest request) throws TooManyHopsException, IOException
	{
		String method = request.getMethod();
		SipSession session = request.getSession();
		addListenerTest(session);
		assertTrue(session.isValid());
		
		assertFalse(session.isReadyToInvalidate());
		assertFalse(session.getApplicationSession().isReadyToInvalidate());
		if (method.equals("INVITE"))
		{
			Proxy proxy = request.getProxy();
			proxy.setRecordRoute(true);
			proxy.setSupervised(true);
			proxy.proxyTo(request.getRequestURI());	
			assertEquals(State.INITIAL, session.getState());
		}
		else if (method.equals("BYE"))
		{
			assertEquals(State.CONFIRMED, session.getState());
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
				assertEquals(State.INITIAL, session.getState());
				URI uri = response.getRequest().getAddressHeader("proxy").getURI();
				response.getProxy().proxyTo(uri);
			}
			else if (response.getStatus() == SipServletResponse.SC_OK)
			{
				assertEquals(State.CONFIRMED, session.getState());
			}
			else
				throw new IllegalAccessException("Unexpected " + response.getStatus() + "/INVITE");
		}
		else if (method.equals("BYE"))
		{
			assertTrue(session.isReadyToInvalidate());
			assertTrue(session.getApplicationSession().isReadyToInvalidate());
			assertEquals(State.TERMINATED, session.getState());
		}
	}
	
	public void testProxyProvisional4xx(SipServletRequest request) throws TooManyHopsException, IOException
	{
		String method = request.getMethod();
		SipSession session = request.getSession();
		addListenerTest(session);
		assertTrue(session.isValid());
		
		assertFalse(session.isReadyToInvalidate());
		assertFalse(session.getApplicationSession().isReadyToInvalidate());
		if (method.equals("INVITE"))
		{
			Proxy proxy = request.getProxy();
			proxy.setRecordRoute(true);
			proxy.setSupervised(true);
			proxy.proxyTo(request.getRequestURI());	
			assertEquals(State.INITIAL, session.getState());
		}
		else if (method.equals("BYE"))
		{
			assertEquals(State.CONFIRMED, session.getState());
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
				assertEquals(State.EARLY, session.getState());
			}
			else if (status == SipServletResponse.SC_NOT_FOUND)
			{
				assertEquals(State.EARLY, session.getState()); // FIXME EARLY or INITIAL ????
				URI uri = response.getRequest().getAddressHeader("proxy").getURI();
				response.getProxy().proxyTo(uri);
			}
			else if (status == SipServletResponse.SC_OK)
			{
				assertEquals(State.CONFIRMED, session.getState());
			}
			else
				throw new IllegalAccessException("Unexpected " + status + "/INVITE");
		}
		else if (method.equals("BYE"))
		{
			assertTrue(session.isReadyToInvalidate());
			// The app session may not be ready to invalidate as the derived session is not in ready
			// to invalidate state.
			assertEquals(State.TERMINATED, session.getState());
		}
	}
	
	public void testProxyParallel(SipServletRequest request) throws Exception
	{
		String method = request.getMethod();
		SipSession session = request.getSession();
		addListenerTest(session);
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
			assertEquals(State.INITIAL, session.getState());
		}
		else if (method.equals("BYE"))
		{
			assertEquals(State.CONFIRMED, session.getState());
		}
	}
	
	public void testProxyParallel(SipServletResponse response) throws Exception
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
				assertEquals(State.EARLY, session.getState());
			}
			else if (status == SipServletResponse.SC_NOT_FOUND)
			{
				assertEquals(State.EARLY, session.getState()); // FIXME state initial or early ? ?
				response.getApplicationSession().setAttribute("received 404", true);
			}
			else if (status == SipServletResponse.SC_OK)
			{
				assertEquals(State.CONFIRMED, session.getState());
				assertNotNull(response.getApplicationSession().getAttribute("received 404"));
			}
			else
				throw new IllegalAccessException("Unexpected " + status + "/INVITE");
		}
		else if (method.equals("BYE"))
		{
			assertTrue(session.isReadyToInvalidate());
			// The app session may not be ready to invalidate as the derived session is not in ready
			// to invalidate state.
			assertEquals(State.TERMINATED, session.getState());
		}
	}
	
	public void testProxyCancel1(SipServletRequest request) throws Exception
	{
		String method = request.getMethod();
		SipSession session = request.getSession();
		addListenerTest(session);
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
			assertEquals(State.INITIAL, session.getState());
		}
		else if (method.equals("BYE"))
		{
			assertEquals(State.CONFIRMED, session.getState());
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
				assertEquals(State.EARLY, session.getState());
			}
			else if (status == SipServletResponse.SC_OK)
			{
				assertEquals(State.CONFIRMED, session.getState());
			}
			else
				throw new IllegalAccessException("Unexpected " + status + "/INVITE");
		}
		else if (method.equals("BYE"))
		{
			assertTrue(session.isReadyToInvalidate());
			// The app session may not be ready to invalidate as the derived session is not in ready
			// to invalidate state.
			assertEquals(State.TERMINATED, session.getState());
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
		assertEquals(State.INITIAL, session.getState());
		request.createResponse(SipServletResponse.SC_OK).send();
	}
	
	public void testTwoProxy(SipServletRequest request) throws Throwable
	{
		String method = request.getMethod();
		SipSession session = request.getSession();
		addListenerTest(session);
		assertTrue(session.isValid());
		
		assertFalse(session.isReadyToInvalidate());
		assertFalse(session.getApplicationSession().isReadyToInvalidate());
		if (method.equals("INVITE"))
		{
			Proxy proxy = request.getProxy();
			proxy.setRecordRoute(true);
			proxy.setSupervised(true);
			request.pushRoute(getOwnUri());
			request.setHeader(MainServlet.SERVLET_HEADER, ProxyTwoServlet.class.getName());
			proxy.proxyTo(request.getRequestURI());	
			assertEquals(State.INITIAL, session.getState());
		}
		else if (method.equals("BYE"))
		{
			assertEquals(State.CONFIRMED, session.getState());
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
				assertEquals(State.EARLY, session.getState());
			}
			else if (response.getStatus() == SipServletResponse.SC_OK)
			{
				assertEquals(State.CONFIRMED, session.getState());
			}
			else
				throw new IllegalAccessException("Unexpected " + response.getStatus() + "/INVITE");
		}
		else if (method.equals("BYE"))
		{
			assertTrue(session.isReadyToInvalidate());
			assertEquals(State.TERMINATED, session.getState());
		}
	}
	
	public void testSubscribe(SipServletRequest request) throws Throwable
	{
		SipSession session = request.getSession();
		assertFalse(session.isReadyToInvalidate());
		assertFalse(session.getApplicationSession().isReadyToInvalidate());
		assertEquals(State.INITIAL, session.getState());
		request.createResponse(SipServletResponse.SC_OK).send();
		Thread.sleep(50);
		session.createRequest("NOTIFY").send();
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
		//System.out.println("sessionReadyToInvalidate");
		//new Exception("sessionReadyToInvalidate").printStackTrace();
		try
		{
			String key = (String) e.getSession().getAttribute(InvalidateWhenReadyServlet.class.getName());
			if (key != null)
			{
				byte[] c = (byte[]) getServletContext().getAttribute(key);
				if (c != null && LISTENER_TEST.equals(new String(c)))
					getServletContext().removeAttribute(key);
			}
		}
		catch (Throwable e1) 
		{
			e1.printStackTrace();
		}
	}

}
