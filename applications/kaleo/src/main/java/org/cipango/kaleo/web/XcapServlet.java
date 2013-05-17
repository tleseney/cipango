// ========================================================================
// Copyright 2008-2009 NEXCOM Systems
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
package org.cipango.kaleo.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cipango.kaleo.xcap.XcapException;
import org.cipango.kaleo.xcap.XcapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class XcapServlet extends HttpServlet
{
	private Logger _log = LoggerFactory.getLogger(XcapServlet.class);
	private XcapService _xcapService;
	
	public XcapServlet()
	{
	}
	
	public void setXcapService(XcapService service)
	{
		_xcapService = service;
	}

	@Override
	public void init() throws ServletException
	{
		try
		{
			_xcapService = (XcapService) getServletContext().getAttribute(XcapService.class.getName());
			String contextPath = getServletContext().getContextPath();
			if (contextPath == null || contextPath.equals("/"))
				_xcapService.setRootName("/");
			else
				_xcapService.setRootName( contextPath + "/xcap/");
		} catch (Exception e)
		{
			_log.warn("Failed to init XCAP servlet", e);
			throw new ServletException(e);
		}
	}
	
	public XcapService getXcapService()
	{
		return _xcapService;
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
	{
		try
		{
			if (XcapService.POST.equals(request.getMethod())) 
				throw new XcapException(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			
			_xcapService.service(request, response);
		} 
		catch (Exception e)
		{
			_log.warn("Failed to handle XCAP request", e);
		}
	}
	

}
