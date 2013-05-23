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
package org.cipango.client;


import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.Parameterable;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

public class Subscriber
{
	private String _event;
	private Dialog _dialog;
	private long _expires;
	private Parameterable _subscriptionState;
	
	public Subscriber(String event, Dialog dialog)
	{
		_event = event;
		_dialog = dialog;
	}
	
	public SipServletResponse stopSubscription() throws IOException
	{
		SipServletRequest request = _dialog.createRequest(SipMethods.SUBSCRIBE);
		request.setHeader(SipHeaders.EVENT, _event);
		request.setExpires(0);
		request.send();
		SipServletResponse response = _dialog.waitForFinalResponse();
		if (response.getStatus() <= 300)
			_expires = System.currentTimeMillis() + (response.getExpires() * 1000); 
		return response;
	}
	
	public SipServletResponse startSubscription(Address from, Address to, int expires) throws IOException, ServletException
	{
		SipServletRequest request = _dialog.createInitialRequest(SipMethods.SUBSCRIBE, from, to);
				
		request.setHeader(SipHeaders.EVENT, _event);
		request.setExpires(expires);
		_dialog.start(request);
		SipServletResponse response = _dialog.waitForFinalResponse();
		if (response.getStatus() <= 300)
			updateExpires(response.getExpires());
		return response;
	}

	public int getExpires()
	{
		long expires = _expires - System.currentTimeMillis();
		if (expires < 0)
			return 0;
		return (int) (expires /1000);
	}
	
	public boolean isActive()
	{
		checkForNotify();
		if (getSubscriptionState().equals("active"))
			return true;
		if (getSubscriptionState().equals("terminated"))
			return false;
		return getExpires() > 0;
	}
	
	public String getSubscriptionState()
	{
		checkForNotify();
		if (_subscriptionState != null)
			return _subscriptionState.getValue();
		else
			return "unknown";
	}
	
	private void checkForNotify()
	{
		SipServletRequest request = _dialog.getSessionHandler().getUnreadRequest();
		if (request != null)
		{
			try
			{
				_subscriptionState = request.getParameterableHeader(SipHeaders.SUBSCRIPTION_STATE);
				updateExpires(Integer.parseInt(_subscriptionState.getParameter("expires")));
			}
			catch (ServletParseException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void updateExpires(int expires)
	{
		_expires = System.currentTimeMillis() + (expires * 1000); 
	}
	
	
	public SipServletRequest waitForNotify() throws ServletParseException
	{
		SipServletRequest request = _dialog.waitForRequest();
		if (!SipMethods.NOTIFY.equals(request.getMethod()))
			throw new IllegalStateException("Received " + request.getMethod() + " when expected NOTIFY request");
		
		_subscriptionState = request.getParameterableHeader(SipHeaders.SUBSCRIPTION_STATE);
		if (isActive())
			updateExpires(Integer.parseInt(_subscriptionState.getParameter("expires")));
		return request;
	}
	
	
}
