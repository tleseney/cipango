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
import org.cipango.server.sipapp.SipServletMapping;
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
	private SipServletHolder _mainServlet;
	private SipServletMapping[] _servletMappings;
	
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
		super.doStart();
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
		
		updateSipMappings();  
        
        if (_mainServlet == null && _servlets != null && (_servletMappings == null || _servletMappings.length == 0))
        {
        	if (_servlets.length == 1)
        		_mainServlet = _servlets[0];
        	else if (_servlets.length != 0)
        		mex.add(new IllegalStateException("Multiple servlets and no SIP servlet mappping defined."));
        }
		
		mex.ifExceptionThrow();
	}
	
	protected void updateSipMappings() throws ServletException
    {
        if (_servlets == null)
        {
            _nameMap = null;
        }
        else
        {   
            Map<String, SipServletHolder> nm = new HashMap<String, SipServletHolder>();
            
            for (int i = 0; i < _servlets.length; i++)
            {
            	if (nm.containsKey(_servlets[i].getName()))
            		throw new ServletException("A servlet with name " + _servlets[i].getName() + " is already registered");
                nm.put(_servlets[i].getName(), _servlets[i]);
                //_sipServlets[i].setServletHandler(this);
            }
            _nameMap = nm;
        }
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
	
	public SipServletHolder getHolder(String name)
	{
		return _nameMap == null ? null : _nameMap.get(name);
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
	
	public void addServletMapping(SipServletMapping mapping) 
	{		
		setServletMappings((SipServletMapping[]) LazyList.addToArray(getServletMappings(), mapping,
				SipServletMapping.class));
	}
	
	public SipServletMapping[] getServletMappings()
	{
		return _servletMappings;
	}
	
	
	public void setServletMappings(SipServletMapping[] servletMappings)
	{
//		if (getServer() != null)
//            getServer().getContainer().update(this, _servletMappings, servletMappings, "servletMapping", true);
         
        _servletMappings = servletMappings;
    }
	
	public void setMainServletName(String name)
	{
		SipServletHolder previous = _mainServlet;
		_mainServlet = getServlet(name);
//		if (getServer() != null)
//			getServer().getContainer().update(this, previous, _mainServlet, "mainServlet", true);
	}
	
	public SipServletHolder getMainServlet()
	{
		return _mainServlet;
	}
	
	 public SipServletHolder getServlet(String name)
	    {
	        return (SipServletHolder) _nameMap.get(name);
	    }
}
