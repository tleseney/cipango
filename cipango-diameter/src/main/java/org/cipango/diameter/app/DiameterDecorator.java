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
package org.cipango.diameter.app;

import java.util.EventListener;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.cipango.diameter.api.DiameterErrorListener;
import org.cipango.diameter.api.DiameterListener;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler.Decorator;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

public class DiameterDecorator implements Decorator
{

	private DiameterContext _context;
	private WebAppContext _appContext;
	
	public DiameterDecorator(DiameterContext context, WebAppContext appContext)
	{
		_context = context;
		_appContext = appContext;
	}
	
	public <T extends Filter> T decorateFilterInstance(T filter) throws ServletException
	{
		return filter;
	}

	public <T extends Servlet> T decorateServletInstance(T servlet) throws ServletException
	{
		return servlet;
	}

	public <T extends EventListener> T decorateListenerInstance(T listener) throws ServletException
	{
		if (listener instanceof DiameterListener)
			_context.addListener(_appContext, (DiameterListener) listener);
		if (listener instanceof DiameterErrorListener)
			_context.addErrorListener(_appContext, (DiameterErrorListener) listener);
		return listener;
	}

	public void decorateFilterHolder(FilterHolder filter) throws ServletException
	{
	}

	public void decorateServletHolder(ServletHolder servlet) throws ServletException
	{
	}

	public void destroyServletInstance(Servlet s)
	{
	}

	public void destroyFilterInstance(Filter f)
	{
	}

	public void destroyListenerInstance(EventListener f)
	{
		if (f instanceof DiameterListener)
			_context.removeListener(_appContext, (DiameterListener) f);
		if (f instanceof DiameterErrorListener)
			_context.removeErrorListener(_appContext, (DiameterErrorListener) f);
	}

}
