// ========================================================================
// Copyright 2012 NEXCOM Systems
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

import javax.servlet.sip.Address;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.client.Call;
import org.cipango.client.Dialog;
import org.cipango.client.UserAgent;
import org.junit.Ignore;

@Ignore
public class TestAgent extends UserAgent
{
	public static final String SERVLET_HEADER = "P-Servlet";
	public static final String METHOD_HEADER = "P-method";
	
	private String _testServlet;
	private String _testMethod;
	private String _alias;
	
	public TestAgent(Address aor)
	{
		super(aor);
	}

	public String getTestServlet()
	{
		return _testServlet;
	}

	public void setTestServlet(String testServlet)
	{
		_testServlet = testServlet;
	}

	public String getTestMethod()
	{
		return _testMethod;
	}

	public void setTestMethod(String testMethod)
	{
		_testMethod = testMethod;
	}

	public SipServletRequest decorate(SipServletRequest request)
	{
		if (request == null)
			return null;
				
		request.setHeader(SERVLET_HEADER, _testServlet);
		request.setHeader(METHOD_HEADER, _testMethod);
		return request;
	}
	
	public SipServletResponse decorate(SipServletResponse response)
	{
		if (response == null)
			return null;

		response.setHeader(SERVLET_HEADER, _testServlet);
		response.setHeader(METHOD_HEADER, _testMethod);
		return response;
	}

	@Override
	public Dialog customize(Dialog dialog)
	{
		Dialog dlg = null;
		if (dialog instanceof Call)
		{
			TestCall call = new TestCall((Call) dialog);
			call.setAgent(this);
			dlg = call;
		}
		else
			dlg = new TestDialog(dialog);
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
}
