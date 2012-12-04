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
import javax.servlet.UnavailableException;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.server.SipMessage;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject("SIP servlet holder")
public class SipServletHolder extends AbstractLifeCycle implements Comparable<SipServletHolder>
{
	private static final Logger LOG = Log.getLogger(SipServletHolder.class);
	
	private String _name;
	
	protected String _className;
	protected Class<? extends SipServlet> _class;
	
	private SipServlet _servlet;
	private Config _config;
	
	private long _unavailable;
	
	private int _initOrder;
	private boolean _initOnStartup = false;
	
	private SipServletHandler _servletHandler;
	
	private String _displayName;
	
	private final Map<String, String> _initParams = new HashMap<String, String>();
	
	private transient UnavailableException _unavailableEx;
	
	protected void doStart() throws Exception
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
	
    protected void doStop() throws Exception
	{
		if (_servlet != null)
		{
			try
			{
				_servlet.destroy();
				_servletHandler.destroyServlet(_servlet);
			}
			catch (Exception e)
			{
				LOG.warn(e);
			}

		}

		_servlet = null;
		_config = null;
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
	
	@ManagedAttribute(value="Servlet name", readonly=true)
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
	
    /** Get the servlet instance (no initialization done).
     * @return The servlet or null
     */
    public Servlet getServletInstance()
    {
        return _servlet;
    }
	
	protected SipServlet newInstance() throws InstantiationException, IllegalAccessException, ServletException 
	{
		try
		{
			ServletContext ctx = _servletHandler.getServletContext();
			if (ctx == null)
				return _class.newInstance();
			return ctx.createServlet(_class);
		}
		catch (ServletException se)
		{
			Throwable cause = se.getRootCause();
			if (cause instanceof InstantiationException)
				throw (InstantiationException) cause;
			if (cause instanceof IllegalAccessException)
				throw (IllegalAccessException) cause;
			throw se;
		}
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
			
			if (_servletHandler != null && _servletHandler.getServer() != null
					&& _servletHandler.getServer().isStarted())
			{
				_servletHandler.getAppContext().fireServletInitialized(_servlet);
			}
		}
		catch (UnavailableException e)
        {
            makeUnavailable(e);
            _servlet=null;
            _config=null;
            throw e;
        }
        catch (ServletException e)
        {
            makeUnavailable(e.getCause()==null?e:e.getCause());
            _servlet=null;
            _config=null;
            throw e;
        }
        catch (Exception e)
        {
            makeUnavailable(e);
            _servlet=null;
            _config=null;
            throw new ServletException(this.toString(),e);
        }
	}
	
    private void makeUnavailable(final Throwable e)
    {
        if (e instanceof UnavailableException)
            makeUnavailable((UnavailableException)e);
        else
        {
            ServletContext ctx = _servletHandler.getServletContext();
            if (ctx==null)
                LOG.info("unavailable",e);
            else
                ctx.log("unavailable",e);
            _unavailableEx=new UnavailableException(String.valueOf(e),-1)
            {
                {
                    initCause(e);
                }
            };
            _unavailable=-1;
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
    
    public void setServletHandler(SipServletHandler servletHandler)
    {
        _servletHandler = servletHandler;
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

	@ManagedAttribute(value="Init order", readonly=true)
	public int getInitOrder()
	{
		return _initOrder;
	}

	public void setInitOrder(int initOrder)
	{
		_initOnStartup = true;
		_initOrder = initOrder;
	}

	@ManagedAttribute(value="Display name", readonly=true)
	public String getDisplayName()
	{
		return _displayName;
	}

	public void setDisplayName(String displayName)
	{
		_displayName = displayName;
	}

	@Override
	public int compareTo(SipServletHolder sh)
	{
		if (sh==this)
            return 0;
        if (sh._initOrder<_initOrder)
            return 1;
        if (sh._initOrder>_initOrder)
            return -1;

        int c=(_className!=null && sh._className!=null)?_className.compareTo(sh._className):0;
        if (c==0)
            c=_name.compareTo(sh._name);
        if (c==0)
            c=this.hashCode()>sh.hashCode()?1:-1;
            return c;
	}
	
    @Override
    public String toString()
    {
        return String.format("%s@%x==%s,%d,%b",_name,hashCode(),_className,_initOrder,_servlet!=null);
    }
}
