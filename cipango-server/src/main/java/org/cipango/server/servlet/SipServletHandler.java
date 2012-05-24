package org.cipango.server.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.handler.AbstractSipHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.LazyList;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class SipServletHandler extends AbstractSipHandler
{
	private final Logger LOG = Log.getLogger(SipServletHandler.class);
	
	private SipServletHolder[] _servlets = new SipServletHolder[0];
	
	private Map<String, SipServletHolder> _nameMap = new HashMap<String, SipServletHolder>();
	private ServletContext _servletContext;
	
	public SipServletHolder addSipServlet(String className)
	{
		SipServletHolder holder = newSipServletHolder();
		holder.setClassName(className);
		holder.setName(className + "-" + _servlets.length);
		addServlet(holder);
		return holder;
	}
	
	@Override
	protected void doStart() throws Exception
	{
		initialize();
	}
	
	public SipServletHolder[] getServlets()
	{
		return _servlets;
	}
	
	public void addServlet(SipServletHolder holder)
	{
		SipServletHolder[] holders = getServlets();
		
		setServlets((SipServletHolder[]) LazyList.addToArray(holders, holder, SipServletHolder.class));
	}
	
	public SipServletHolder newSipServletHolder()
	{
		return new SipServletHolder();
	}
	
	public synchronized void updateNameMappings()
	{
		_nameMap.clear();
		if (_servlets != null)
		{
			for (SipServletHolder servlet : _servlets)
			{
				_nameMap.put(servlet.getName(), servlet);
			}
		}
	}
	
	public void setServlets(SipServletHolder[] holders)
	{
		_servlets = holders;
		updateNameMappings();
	}
	
	public void initialize() throws Exception
	{
		MultiException mex = new MultiException();
		if (_servlets != null)
		{
			SipServletHolder[] servlets = _servlets.clone();
			Arrays.sort(servlets);
			for (SipServletHolder servlet : servlets)
			{
				try
				{
					servlet.start();
				}
				catch (Exception e)
				{
					LOG.debug(Log.EXCEPTION, e);
					mex.add(e);
				}
			}
		}
		mex.ifExceptionThrow();
	}
	
	public void handle(SipMessage message) throws IOException, ServletException
	{
		System.out.println("yooooopi");
		if (message.isRequest())
		{
			SipRequest request = (SipRequest) message;
			
			SipServletHolder holder = getHolder(request);
			
			if (holder != null)
				holder.handle(request);
			else
				notFound(request);
		}
	}
	
	protected SipServletHolder getHolder(SipRequest request)
	{
		return _servlets[0];
	}
	
	protected void notFound(SipRequest request) throws IOException
	{
		request.createResponse(404).send();
	}
	
	public ServletContext getServletContext()
	{
		return _servletContext;
	}
}
