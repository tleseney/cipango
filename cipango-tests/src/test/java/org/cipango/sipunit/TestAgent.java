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
	
	public static SipServletRequest decorate(SipServletRequest request)
	{
		if (request == null)
			return null;
		
		Exception e = new Exception();
		StackTraceElement[] stackTrace = e.getStackTrace();
		for (StackTraceElement element : stackTrace)
		{
			if (element.getClassName().endsWith("Test")
					&& element.getMethodName().startsWith("test"))
			{
				request.addHeader(SERVLET_HEADER, element.getClassName());
				request.addHeader(METHOD_HEADER, element.getMethodName());
				return request;
			}
		}
		throw new IllegalStateException("Could not found test method");
	}
	
	public static SipServletResponse decorate(SipServletResponse response)
	{
		SipServletRequest request = response.getRequest();
		Iterator<String> it = request.getHeaderNames();
		while (it.hasNext())
		{
			String header = (String) it.next();
			if (TEST_HEADERS.contains(header))
			{
				for (Iterator<?> headers = request.getHeaders(header); headers.hasNext();)
					response.addHeader(header, (String) headers.next());
			}
		}
		return response;
	}
	
	public TestAgent(Address aor)
	{
		super(aor);
	}

	@Override
	public Dialog customize(Dialog dialog)
	{
		Dialog dlg = null;
		if (dialog instanceof Call)
			dlg = new TestCall((Call) dialog);
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
