// ========================================================================
// Copyright 2007-2012 NEXCOM Systems
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TimerService;
import javax.servlet.sip.TooManyHopsException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class SipServletTestCase extends SipServlet
{
	private static final Logger __logger = LoggerFactory.getLogger(SipServletTestCase.class);
	private SipFactory _sipFactory;	
	private TimerService _timerService;

	@Override
	public void init() throws ServletException
	{
		super.init();
		_sipFactory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
		_timerService = (TimerService) getServletContext().getAttribute(TIMER_SERVICE);
	}
	
	public SipFactory getSipFactory()
	{
		return _sipFactory;
	}
	
	public TimerService getTimerService()
	{
		return _timerService;
	}
	
	/**
	 * Send 200 OK if no exceptions has been previously thrown and not forwarded to sipunit 
	 * on this test case.
	 */
	public void checkForFailure(SipServletRequest request) throws IOException, TooManyHopsException
	{
		byte[] content = (byte[]) getServletContext().getAttribute(getFailureKey());
		if (content == null)
		{
			request.createResponse(SipServletResponse.SC_OK).send();
			return;
		}
		
       try
		{
   			SipServletResponse error = request.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR); 
			error.setContent(content, "text/plain");
			error.send();
		}
		catch (Exception e1)
		{
			__logger.warn("Failed to send error on " + request.getMethod(), e1);
		}

		getServletContext().removeAttribute(getFailureKey());	
	}
	
	public void resetFailure()
	{
		byte[] content = (byte[]) getServletContext().getAttribute(getFailureKey());
		if (content != null)
			__logger.warn("Got a failure key: {}, reset it", new String(content));
		getServletContext().removeAttribute(getFailureKey());
	}
	
	/**
	 * Send a response with exception stack trace.
	 * If the response cannot be created or sent (request is committed or ACK request) 
	 * the failure trace is saved on the servlet context and will be sent on 
	 * {@link #checkForFailure(SipServletRequest)} call.
	 * 
	 * @param request
	 * @param e
	 */
	public void sendError(SipServletRequest request, Throwable e) throws TooManyHopsException
	{
		if (e instanceof InvocationTargetException && e.getCause() != null)
			e = e.getCause();
		
		StringBuffer sb = new StringBuffer();
		sb.append("Unable to process ").append(request.getMethod());
		String methodName = request.getHeader(MainServlet.METHOD_HEADER);
		if (methodName != null)
			sb.append(" on test method ").append(methodName);
		sb.append(" on test class ").append(getServletName()).append("\n");

		__logger.warn(sb.toString() + request, e);
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(os);
        pw.print(sb);
        e.printStackTrace(pw);
        pw.flush();
        byte[] content = os.toByteArray();
		
		if (!request.isCommitted() && !request.getMethod().equalsIgnoreCase("ACK"))
		{
			if (e instanceof TooManyHopsException)
				throw (TooManyHopsException) e;
			
			SipServletResponse error = request.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR,
					e.getMessage());

            try
			{
				error.setContent(content, "text/plain");
				error.send();
				return;
			}
			catch (Exception e1)
			{
				if (!(e instanceof IllegalStateException) 
						|| !"Session is proxy".equalsIgnoreCase(e.getMessage()))
					__logger.warn("Failed to send error on " + request.getMethod(), e1);
			}
		}
		
		getServletContext().setAttribute(getFailureKey(), content);
	}
	
	public void sendError(SipServletResponse response, Throwable e)
	{
		if (e instanceof InvocationTargetException && e.getCause() != null)
			e = e.getCause();
		
		StringBuffer sb = new StringBuffer();
		sb.append("Unable to process ").append(response.getStatus() + "/").append(response.getMethod());
		String methodName = response.getHeader(MainServlet.METHOD_HEADER);
		if (methodName != null)
			sb.append(" on test method ").append(methodName);
		sb.append(" on test class ").append(getServletName()).append("\n");

		__logger.warn(sb.toString() + response, e);
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(os);
        pw.print(sb);
        e.printStackTrace(pw);
        pw.flush();
        byte[] content = os.toByteArray();
				
		getServletContext().setAttribute(getFailureKey(), content);
	}
	
	public void setContent(SipServletMessage message, Throwable e) throws UnsupportedEncodingException
	{
		if (e instanceof InvocationTargetException && e.getCause() != null)
			e = e.getCause();
		
		StringBuffer sb = new StringBuffer();
		sb.append("Unable to process ");
		if (message instanceof SipServletResponse)
			sb.append(((SipServletResponse) message).getStatus() + "/");
		sb.append(message.getMethod());
		String methodName = message.getHeader(MainServlet.METHOD_HEADER);
		if (methodName != null)
			sb.append(" on test method ").append(methodName);
		sb.append(" on test class ").append(getServletName()).append("\n");

		__logger.warn(sb.toString() + message, e);
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(os);
        pw.print(sb);
        e.printStackTrace(pw);
        pw.flush();
        byte[] content = os.toByteArray();
			
        message.setContent(content, "text/plain");
		getServletContext().setAttribute(getFailureKey(), content);
	}
	
	protected SipURI getOwnUri()
	{
		@SuppressWarnings("unchecked")
		List<SipURI> l = (List<SipURI>) getServletContext().getAttribute(OUTBOUND_INTERFACES);
		SipURI uri = l.get(0);
		uri = (SipURI) uri.clone();
		uri.setUser("cipango-servlet-test");
		uri.setLrParam(true);
		return uri;
	}
	
	
	protected String getFailureKey()
	{
		return "Failure: " + getServletName();
	}

}
