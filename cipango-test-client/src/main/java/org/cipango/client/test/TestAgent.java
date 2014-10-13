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
package org.cipango.client.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.URI;

import org.cipango.client.Call;
import org.cipango.client.Dialog;
import org.cipango.client.SipMethods;
import org.cipango.client.UserAgent;
import org.junit.Ignore;

@Ignore
public class TestAgent extends UserAgent
{

	private final Map<String, String> _extraHeaders = new HashMap<String, String>();
	private String _alias;
	
	public TestAgent(Address aor)
	{
		super(aor);
	}


	public <T extends SipServletMessage> T decorate(T message)
	{
		if (message == null)
			return null;
				
		for (Map.Entry<String, String> entry : _extraHeaders.entrySet())
			message.addHeader(entry.getKey(), entry.getValue());

		return message;
	}

	@Override
	public Dialog customize(Dialog dialog)
	{
		super.customize(dialog);
		Dialog dlg = null;
		if (dialog instanceof Call)
		{
			TestCall call = new TestCall((Call) dialog);
			call.setAgent(this);
			dlg = call;
		}
		else
			dlg = new TestDialog(dialog, _extraHeaders);
		return super.customize(dlg); 
	}
	
	/**
	 * Decorates <code>request</code> with headers identifying the running test.
	 * 
	 * @param request the request to be decorated.
	 * @return the <code>request</code> object.
	 */
	@Override
	public SipServletRequest customize(SipServletRequest request)
	{
		if (request == null)
			return null;
		
		return decorate(super.customize(request));
	}
	
	public SipServletResponse createResponse(SipServletRequest request, int status)
	{
		return decorate(request.createResponse(status));
	}
	
	public SipServletResponse createResponse(SipServletRequest request, int status, String reason)
	{
		return decorate(request.createResponse(status, reason));
	}
	
	public String getAlias()
	{
		return _alias;
	}

	public void setAlias(String alias)
	{
		_alias = alias;
	}


	public Map<String, String> getExtraHeaders()
	{
		return _extraHeaders;
	}

	public Call createCall(UserAgent remoteUa) throws IOException, ServletException {
		Call call = (Call) customize(new Call());
		SipServletRequest request = call.createInitialRequest(SipMethods.INVITE, getAor(),  remoteUa.getAor());
		request.setRequestURI(remoteUa.getContact().getURI());
		call.start(request);
		return call;
	}

}
