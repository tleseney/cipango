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
package org.cipango.server.sipapp;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
public class ServletContextProxy implements ServletContext
{
	private ServletContext _context;
	
	public ServletContextProxy(ServletContext servletContext)
	{
		_context = servletContext;
	}
	
	@Override
	public RequestDispatcher getNamedDispatcher(String name)
	{
		return _context.getNamedDispatcher(name);
	}

	@Override
	public String getContextPath()
	{
		return _context.getContextPath();
	}

	@Override
	public ServletContext getContext(String uripath)
	{
		return _context.getContext(uripath);
	}

	@Override
	public int getMajorVersion()
	{
		return _context.getMajorVersion();
	}

	@Override
	public int getMinorVersion()
	{
		return _context.getMinorVersion();
	}

	@Override
	public String getMimeType(String file)
	{
		return _context.getMimeType(file);
	}

	@Override
	public Set getResourcePaths(String path)
	{
		return _context.getResourcePaths(path);
	}

	@Override
	public URL getResource(String path) throws MalformedURLException
	{
		return _context.getResource(path);
	}

	@Override
	public InputStream getResourceAsStream(String path)
	{
		return _context.getResourceAsStream(path);
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path)
	{
		return _context.getRequestDispatcher(path);
	}


	@Override
	public Servlet getServlet(String name) throws ServletException
	{
		return _context.getServlet(name);
	}

	@Override
	public Enumeration getServlets()
	{
		return _context.getServlets();
	}

	@Override
	public Enumeration getServletNames()
	{
		return _context.getServletNames();
	}

	@Override
	public void log(String msg)
	{
		_context.log(msg);
	}

	@Override
	public void log(Exception exception, String msg)
	{
		_context.log(exception, msg);
	}

	@Override
	public void log(String message, Throwable throwable)
	{
		_context.log(message, throwable);
	}

	@Override
	public String getRealPath(String path)
	{
		return _context.getRealPath(path);
	}

	public String getServerInfo()
	{
		return _context.getServerInfo();
	}

	@Override
	public String getInitParameter(String name)
	{
		return _context.getInitParameter(name);
	}

	@Override
	public Enumeration getInitParameterNames()
	{
		return _context.getInitParameterNames();
	}

	@Override
	public Object getAttribute(String name)
	{
		return _context.getAttribute(name);
	}

	@Override
	public Enumeration getAttributeNames()
	{
		return _context.getAttributeNames();
	}

	@Override
	public void setAttribute(String name, Object object)
	{
		_context.setAttribute(name, object);
	}

	@Override
	public void removeAttribute(String name)
	{
		_context.removeAttribute(name);
	}

	@Override
	public String getServletContextName()
	{
		return _context.getServletContextName();
	}

	@Override
	public Dynamic addFilter(String arg0, String arg1)
	{
		return _context.addFilter(arg0, arg1);
	}

	@Override
	public Dynamic addFilter(String arg0, Filter arg1)
	{
		return _context.addFilter(arg0, arg1);
	}

	@Override
	public Dynamic addFilter(String arg0, Class<? extends Filter> arg1)
	{
		return _context.addFilter(arg0, arg1);
	}

	@Override
	public void addListener(String arg0)
	{
		_context.addListener(arg0);
	}

	@Override
	public <T extends EventListener> void addListener(T arg0)
	{
		_context.addListener(arg0);
	}

	@Override
	public void addListener(Class<? extends EventListener> arg0)
	{
		_context.addListener(arg0);
	}

	@Override
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, String arg1)
	{
		return _context.addServlet(arg0, arg1);
	}

	@Override
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, Servlet arg1)
	{
		return _context.addServlet(arg0, arg1);
	}

	@Override
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, Class<? extends Servlet> arg1)
	{
		return _context.addServlet(arg0, arg1);
	}

	@Override
	public <T extends Filter> T createFilter(Class<T> arg0) throws ServletException
	{
		return _context.createFilter(arg0);
	}

	@Override
	public <T extends EventListener> T createListener(Class<T> arg0) throws ServletException
	{
		return _context.createListener(arg0);
	}

	@Override
	public <T extends Servlet> T createServlet(Class<T> arg0) throws ServletException
	{
		return _context.createServlet(arg0);
	}

	@Override
	public void declareRoles(String... arg0)
	{
		_context.declareRoles(arg0);
	}

	@Override
	public ClassLoader getClassLoader()
	{
		return _context.getClassLoader();
	}

	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes()
	{
		return _context.getDefaultSessionTrackingModes();
	}

	@Override
	public int getEffectiveMajorVersion()
	{
		return _context.getEffectiveMajorVersion();
	}

	@Override
	public int getEffectiveMinorVersion()
	{
		return _context.getEffectiveMinorVersion();
	}

	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes()
	{
		return _context.getEffectiveSessionTrackingModes();
	}

	@Override
	public FilterRegistration getFilterRegistration(String arg0)
	{
		return _context.getFilterRegistration(arg0);
	}

	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations()
	{
		return _context.getFilterRegistrations();
	}

	@Override
	public JspConfigDescriptor getJspConfigDescriptor()
	{
		return _context.getJspConfigDescriptor();
	}

	@Override
	public ServletRegistration getServletRegistration(String arg0)
	{
		return _context.getServletRegistration(arg0);
	}

	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations()
	{
		return _context.getServletRegistrations();
	}

	@Override
	public SessionCookieConfig getSessionCookieConfig()
	{
		return _context.getSessionCookieConfig();
	}

	@Override
	public boolean setInitParameter(String arg0, String arg1)
	{
		return _context.setInitParameter(arg0, arg1);
	}

	@Override
	public void setSessionTrackingModes(Set<SessionTrackingMode> arg0)
	{
		_context.setSessionTrackingModes(arg0);
	}

	@Override
	public String getVirtualServerName() 
	{
		return _context.getVirtualServerName();
	}

}