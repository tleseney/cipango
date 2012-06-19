package org.cipango.server.sipapp;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.cipango.server.SipMessage;
import org.cipango.server.handler.AbstractSipHandler;
import org.cipango.server.servlet.SipServletHandler;
import org.cipango.server.session.SessionHandler;
import org.eclipse.jetty.webapp.WebAppContext;

public class SipAppContext extends AbstractSipHandler 
{
	private String _name;
	
	private SessionHandler _sessionHandler;
	private SipServletHandler _servletHandler;
	
	private WebAppContext _context;
	private ServletContext _sContext;
	
	public SipAppContext()
	{
		_sContext = new SContext();
		_sessionHandler = new SessionHandler();
		_servletHandler = new SipServletHandler();
	}
	
	public ServletContext getServletContext()
	{
		return _sContext;
	}
	
	public void setName(String name)
	{
		_name = name;
	}
	
	public String getName()
	{
		return _name;
	}
	
	@Override
	protected void doStart() throws Exception
	{
		
		_sessionHandler.setHandler(_servletHandler);
		_sessionHandler.start();
		super.doStart();
	}
	
	public SipServletHandler getSipServletHandler()
	{
		return _servletHandler;
	}
	
	public void handle(SipMessage message) throws IOException, ServletException 
	{
		_sessionHandler.handle(message);
		// TODO Auto-generated method stub
	}
	
	class SContext implements ServletContext
	{
		public ServletContext getWebContext()
		{
			return _context.getServletContext();
		}

		@Override
		public String getContextPath() 
		{
			return getWebContext().getContextPath();
		}

		@Override
		public ServletContext getContext(String uripath) 
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getMajorVersion() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getMinorVersion() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public String getMimeType(String file) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Set getResourcePaths(String path) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public URL getResource(String path) throws MalformedURLException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public InputStream getResourceAsStream(String path) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public RequestDispatcher getRequestDispatcher(String path) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public RequestDispatcher getNamedDispatcher(String name) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Servlet getServlet(String name) throws ServletException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Enumeration getServlets() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Enumeration getServletNames() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void log(String msg) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void log(Exception exception, String msg) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void log(String message, Throwable throwable) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getRealPath(String path) {
			// TODO Auto-generated method stub
			return null;
		}

		public String getServerInfo() { return getWebContext().getServerInfo(); }

		@Override
		public String getInitParameter(String name) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Enumeration getInitParameterNames() {
			// TODO Auto-generated method stub
			return null;
		}

		public Object getAttribute(String name) 
		{
			return getWebContext().getAttribute(name);
		}
		
		@Override
		public Enumeration getAttributeNames() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setAttribute(String name, Object object) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void removeAttribute(String name) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getServletContextName() {
			// TODO Auto-generated method stub
			return null;
		}
		
		
	}
}
