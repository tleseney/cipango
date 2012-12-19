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
package org.cipango.test.common;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.TooManyHopsException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractServlet extends SipServletTestCase
{
	private static final Logger __logger = LoggerFactory.getLogger(AbstractServlet.class);
	
	@Override
	protected void doRequest(SipServletRequest request) throws ServletException, IOException, TooManyHopsException
	{
		String methodName = request.getHeader(MainServlet.METHOD_HEADER);
		try
		{
			if (methodName == null) 
			{
				if (request.isInitial())
					throw new IllegalArgumentException("Missing header: " + MainServlet.METHOD_HEADER);
				else
				{
					__logger.info("Continue test "
							+ getServletSimpleName() + "." + methodName + "()" 
							+ " on subsequent " + request.getMethod());
					doSubsequentRequest(request);
				}
			}
			else if (methodName.equals("checkForFailure"))
			{
				checkForFailure(request);
			}
			else
			{
				if (request.isInitial())
				{
					resetFailure();
					request.getApplicationSession().setAttribute("Tests method", methodName);
				}
				__logger.info((request.isInitial() ? "Starting test " : "continue test ")
						+ getServletSimpleName() + "." + methodName + "()" 
						+ (request.isInitial() ? "" : " on " + request.getMethod()));
				Method method = getClass().getDeclaredMethod(methodName, SipServletRequest.class);
				method.invoke(this, request);
			}	
		}
		catch (Throwable e)
		{
			sendError(request, e);
		} 
	}
	
	protected void doSubsequentRequest(SipServletRequest request) throws IOException
	{
		if (!request.getMethod().equalsIgnoreCase("ACK")
				&& !request.getMethod().equalsIgnoreCase("CANCEL"))
		{
			request.createResponse(SipServletResponse.SC_OK).send();
		}
	}
	
	protected  String getServletSimpleName()
	{
		return getServletName().substring(getServletName().lastIndexOf('.') + 1);
	}
	

	@Override
	protected void doResponse(SipServletResponse response) throws ServletException, IOException, TooManyHopsException
	{
		String methodName = response.getHeader(MainServlet.METHOD_HEADER);
		try
		{
			if (methodName != null)
			{
				__logger.info("continue test " 
						+  getServletSimpleName() + "." + methodName + "()"
						+ " on " + response.getStatus() + "/" + response.getMethod());
				Method method = getClass().getDeclaredMethod(methodName, SipServletResponse.class);
				method.invoke(this, response);
			}	
			else
				__logger.warn("Ignore response as no method header present\n " + response);
		}
		catch (Throwable e)
		{
			sendError(response, e);
		} 
	}
}
