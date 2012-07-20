package org.cipango.sipunit;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.servlet.sip.Address;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.client.Call;
import org.cipango.client.Dialog;
import org.cipango.client.UserAgent;

public class TestAgent extends UserAgent
{
	public static final String SERVLET_HEADER = "P-Servlet";
	public static final String METHOD_HEADER = "P-method";

	private static final List<String> TEST_HEADERS = Arrays.asList(
			SERVLET_HEADER, METHOD_HEADER);
	
	private String _testServlet;
	private String _testMethod;
	
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
		
		Iterator<String> it = request.getHeaderNames();
		while (it.hasNext())
		{
			String header = (String) it.next();
			if (TEST_HEADERS.contains(header))
				request.removeHeader(header);
		}
		
		request.addHeader(SERVLET_HEADER, _testServlet);
		request.addHeader(METHOD_HEADER, _testMethod);
		return request;
	}
	
	public SipServletResponse decorate(SipServletResponse response)
	{
		if (response == null)
			return null;

		Iterator<String> it = response.getHeaderNames();
		while (it.hasNext())
		{
			String header = (String) it.next();
			if (TEST_HEADERS.contains(header))
				response.removeHeader(header);
		}

		response.addHeader(SERVLET_HEADER, _testServlet);
		response.addHeader(METHOD_HEADER, _testMethod);
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
}
