package org.cipango.server.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SingleThreadModel;
import javax.servlet.UnavailableException;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.server.SipMessage;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.util.component.AbstractLifeCycle;


public class SipServletHolder extends AbstractLifeCycle
{
	private String _name;
	
	protected String _className;
	protected Class<? extends SipServlet> _class;
	
	private Servlet _servlet;
	private Config _config;
	
	private long _unavailable;
	
	private int _initOrder;
	private boolean _initOnStartup = false;
	
	private SipServletHandler _servletHandler;
	
	private String _displayName;
	
	private final Map<String, String> _initParams = new HashMap<String, String>();
	
	public void doStart() throws Exception
	{
		_unavailable = 0;
		try
		{
			if (_class == null && (_className == null || _className.equals("")))
				throw new UnavailableException("No class for servlet " + _name, -1);
			
			if (_class == null)
			{
				try
				{
					_class = Loader.loadClass(SipServletHolder.class, _className);
				}
				catch (Exception e)
				{
					throw new UnavailableException(e.getMessage(), -1);
				}
			}
		}
		finally
		{
			
		}
		
		_config = new Config();
		
		 // TODO singlethread
		
		if (_initOnStartup)
		{
			try
			{
				initServlet();
			}
			catch (Exception e)
			{
				// TODO check start
				throw e;
			}
		}
	}
	
	public void setClassName(String className)
	{
		_className = className;
		_class = null;
	}
	
	public String getClassName()
    {
        return _className;
    }
	
	public String getName()
	{
		return _name;
	}
	
	public void setName(String name)
	{
		_name = name;
	}
	
	public synchronized void setServlet(SipServlet servlet)
	{
		if (servlet == null)
			throw new IllegalArgumentException();

		_servlet = servlet;
		setHeldClass(servlet.getClass());
	}
	
	public void setHeldClass(Class<? extends SipServlet> held)
    {
        _class=held;
        if (held!=null)
        {
            _className=held.getName();
            if (_name==null)
                _name=held.getName()+"-"+this.hashCode();
        }
    }
	
	public void handle(SipMessage message) throws ServletException, IOException
	{
		if (_class == null)
			throw new UnavailableException("Servlet not initialized");
		
		Servlet servlet = _servlet;
		synchronized(this)
		{
			if (_unavailable != 0 || !_initOnStartup)
				servlet = getServlet();
			if (servlet == null)
				throw new UnavailableException("Could not instantiate " + _class);
		}
		
		try
		{
			if (message.isRequest())
				servlet.service((SipServletRequest) message, null);
			else
				servlet.service(null, (SipServletResponse) message);
		}
		finally
		{
			
		}
		
	}
	
	public synchronized Servlet getServlet() throws ServletException
	{
		if (_servlet == null)
			initServlet();
		return _servlet;
	}
	
	protected Servlet newInstance() throws InstantiationException, IllegalAccessException 
	{
		
			return _class.newInstance(); // TODO complete
	}
	
	
	private void initServlet() throws ServletException
	{
		try
		{
			if (_servlet == null)
				_servlet = newInstance();
			if (_config == null)
				_config = new Config();
			
			_servlet.init(_config);
		}
		catch (Exception e)
		{
			e.printStackTrace(); // TODO complete
		}
	}
	
	public String getInitParameter(String name)
	{
		return _initParams.get(name);
	}
	
	public Enumeration<String> getInitParameterNames()
	{
		return Collections.enumeration(_initParams.keySet());
	}
	
    
    /* ------------------------------------------------------------ */
    public void setInitParameter(String param,String value)
    {
        _initParams.put(param,value);
    }
    
    /* ---------------------------------------------------------------- */
    public void setInitParameters(Map<String,String> map)
    {
        _initParams.clear();
        _initParams.putAll(map);
    }
    
	
	class Config implements ServletConfig
	{
		public String getServletName()
		{
			return getName();
		}
		
		public ServletContext getServletContext()
		{
			return _servletHandler.getServletContext();
		}
		
		public String getInitParameter(String name)
		{
			return SipServletHolder.this.getInitParameter(name);
		}
		
		public Enumeration<String> getInitParameterNames() 
		{
			return SipServletHolder.this.getInitParameterNames();
		}
	}


	public int getInitOrder()
	{
		return _initOrder;
	}

	public void setInitOrder(int initOrder)
	{
		_initOnStartup = true;
		_initOrder = initOrder;
	}

	public String getDisplayName()
	{
		return _displayName;
	}

	public void setDisplayName(String displayName)
	{
		_displayName = displayName;
	}
}
