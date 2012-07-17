// ========================================================================
// Copyright 2010 NEXCOM Systems
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

import java.io.Serializable;
import java.util.Arrays;

import javax.servlet.sip.Proxy;
import javax.servlet.sip.ProxyBranch;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.TooManyHopsException;
import javax.servlet.sip.URI;
import javax.servlet.sip.annotation.SipServlet;

import org.cipango.test.common.AbstractServlet;

@SipServlet (name="org.cipango.sipunit.test.ProxyTest")
public class ProxyServlet extends AbstractServlet
{

	public void testProxyDiameter(SipServletRequest request) throws Throwable
	{
		Proxy proxy = request.getProxy();
		proxy.proxyTo(request.getRequestURI());
	}
	
	public void testProxyDiameter(SipServletResponse response) throws Throwable
	{
		int status = response.getStatus();
		if (status == SipServletResponse.SC_NOT_FOUND)
		{
			SipServletRequest request = response.getRequest();
			Object lock = new Object();
			request.setAttribute("lock", lock);

			getTimerService().createTimer(getSipFactory().createApplicationSession(), 1000, false, 
					new ProxyDiameterRunnable(request));
			synchronized (lock)
			{
				lock.wait(5000);
			}
			assertNotNull(request.getAttribute("done"));
			URI uri = response.getRequest().getAddressHeader("proxy").getURI();
			response.getProxy().proxyTo(uri);
		}
	}
	
	public void testVirtualBranch(SipServletRequest request) throws Throwable
	{
		Proxy proxy = request.getProxy();
		proxy.proxyTo(request.getRequestURI());
	}
	
	public void testVirtualBranch(SipServletResponse response) throws Throwable
	{
		int status = response.getStatus();
		if (status == SipServletResponse.SC_RINGING)
		{
			SipServletRequest request = response.getRequest();
			request.getProxy().cancel();
			request.getProxy().getOriginalRequest().createResponse(SipServletResponse.SC_REQUEST_TIMEOUT).send();
		}
	}
	
	public void testInvalidateBefore200(SipServletRequest request) throws Throwable
	{
		Proxy proxy = request.getProxy();
		proxy.setRecordRoute(true);
		proxy.proxyTo(request.getRequestURI());
		if ("BYE".equals(request.getMethod()))
			request.getApplicationSession().invalidate();
	}
	
	public void testInvalidateBefore200(SipServletResponse response) throws Throwable
	{
		if ("BYE".equals(response.getMethod()))
			response.addHeader("error", "servlet has been invoked on BYE and session is " + 
					(response.getSession().isValid() ? "valid" : "invalid"));
		else
			response.addHeader("good", "servlet has been invoked and session is " + 
					(response.getSession().isValid() ? "valid" : "invalid"));
	}
	
	
	public void testTelUri(SipServletRequest request) throws Throwable
	{
		Proxy proxy = request.getProxy();
		proxy.setRecordRoute(false);
		String reqUri = request.getHeader("req-uri");
		ProxyBranch branch;
		if (reqUri == null)
		{
			branch = proxy.createProxyBranches(Arrays.asList(getSipFactory().createURI("tel:1234"))).get(0);
			branch.getRequest().pushRoute(request.getPoppedRoute());
		}
		else
			branch = proxy.createProxyBranches(Arrays.asList(getSipFactory().createURI(reqUri))).get(0);
		branch.getRequest().setHeader("req-uri", request.getRequestURI().toString());
		proxy.startProxy();
	}
	
	public void testTelUri(SipServletResponse response) throws Throwable
	{
	}
	
		
	class ProxyDiameterRunnable implements Serializable, Runnable
	{
		private SipServletRequest _request;
		
		public ProxyDiameterRunnable(SipServletRequest request)
		{
			_request = request;
		}
		
		public void run()
		{
			try
			{
				assertTrue("Session is not valid", _request.getSession().isValid());
				_request.setAttribute("done", "timer");
			}
			catch (Throwable e) 
			{
				try
				{
					sendError(_request, e);
				}
				catch (TooManyHopsException e1)
				{
					e1.printStackTrace();
				}
			}
			finally
			{
				Object lock = _request.getAttribute("lock");
				synchronized (lock)
				{
					lock.notify();
				}
			}
		}
		
	}
}
