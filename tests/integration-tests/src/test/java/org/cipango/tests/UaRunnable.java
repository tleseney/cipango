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
package org.cipango.tests;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;

import junit.framework.Assert;

import org.cipango.client.Dialog;
import org.cipango.client.MessageHandler;
import org.cipango.client.SipMethods;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;


public abstract class UaRunnable extends Thread
{
	private Logger LOG = Log.getLogger(UaRunnable.class);
	
	protected TestAgent _ua;
	protected Dialog _dialog;
	private Throwable _e;
	private Boolean _isDone = Boolean.FALSE;

	public UaRunnable(TestAgent userAgent)
	{
		_ua = userAgent;
		_ua.setDefaultHandler(new InitialRequestHandler());
		_dialog = _ua.customize(new Dialog());
		_dialog.setCredentials(_ua.getCredentials());
		_dialog.setTimeout(_ua.getTimeout());
	}
	
	public void run()
	{
		try
		{
			doTest();
		}
		catch (Throwable e)
		{
			LOG.warn("Got throwable for agent " + _ua, e);
			_e = e;
		}
		finally
		{
			_isDone = true;
			synchronized (_isDone)
			{
				_isDone.notify();
			}
		}
	}
	
	public abstract void doTest() throws Throwable;
	

	protected void handlePotentialCancel() throws IOException
	{
		SipServletRequest request = _dialog.getSessionHandler().getLastRequest();
		if (request.getMethod().equals(SipMethods.CANCEL))
		{
			request.createResponse(SipServletResponse.SC_CALL_LEG_DONE).send();
		}
	}

	public Throwable getException()
	{
		return _e;
	}

	public boolean isDone()
	{
		return _isDone;
	}

	public String getUserName()
	{
		SipURI uri = (SipURI) _ua.getAor().getURI();
		return uri.getUser();
	}
	
	public void assertDone() throws Throwable
	{
		if (_e != null)
			throw _e;
		if (_isDone)
			return;
		
		synchronized (_isDone)
		{
			try
			{
				_isDone.wait(2000);
			}
			catch (InterruptedException e)
			{
			}
		}
		if (_e != null)
			throw _e;
		if (!_isDone)
			Assert.fail(getUserName() + " not done");
	}

	public SipServletRequest waitForInitialRequest()
	{
		synchronized(_dialog)
		{
			try { _dialog.wait(); } catch (InterruptedException e) { }
		}
		return (SipServletRequest) _dialog.getSession().getAttribute(
				Dialog.INITIAL_REQUEST_ATTRIBUTE);
	}
	
	public class InitialRequestHandler implements MessageHandler
	{
		public void handleRequest(SipServletRequest request)
				throws IOException, ServletException
		{
			_dialog.accept(request);
			synchronized(_dialog)
			{
				_dialog.notifyAll();
			}
		}

		public void handleResponse(SipServletResponse response)
				throws IOException, ServletException
		{
		}
	}
}
