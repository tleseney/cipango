package org.cipango.server.servlet;

import java.io.IOException;
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
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.servlet.Holder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject("SIP servlet holder")
public class SipServletHolder extends Holder<SipServlet> implements UserIdentity.Scope, Comparable<SipServletHolder>
{
	
	private static final Logger LOG = Log.getLogger(SipServletHolder.class);
		
	private transient SipServlet _servlet;
	private transient Config _config;
	
	private long _unavailable;
	
	private int _initOrder;
	private boolean _initOnStartup = false;
	
	private SipServletHandler _servletHandler;
		
	private transient UnavailableException _unavailableEx;
	

    private Map<String, String> _roleMap;
    private String _runAsRole;
    
    public SipServletHolder()
	{
		super(Source.EMBEDDED);
	}
    
    public SipServletHolder(Source source)
	{
		super(source);
	}
	
	public void doStart() throws Exception
	{
		_unavailable = 0;
		try
        {
            super.doStart();
            if (_class==null || !SipServlet.class.isAssignableFrom(_class))
            {
                throw new UnavailableException("Servlet "+_class+" is not a javax.servlet.sip.SipServlet");
            }
        }
        catch (UnavailableException ue)
        {
            makeUnavailable(ue);
            throw ue;
        }
		
		_config = new Config();
				
		if (_extInstance || _initOnStartup)
        {
            try
            {
                initServlet();
            }
            catch(Exception e)
            {
               throw e;
            }
        }
	}
	
    public void doStop() throws Exception
	{
		if (_servlet != null)
		{
			try
			{
				_servlet.destroy();
				if (_servletHandler != null)
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
	
    /**
     * @return The unavailable exception or null if not unavailable
     */
    public UnavailableException getUnavailableException()
    {
        return _unavailableEx;
    }
	
	public synchronized void setServlet(SipServlet servlet)
	{
		if (servlet == null)
			throw new IllegalArgumentException();

		_servlet = servlet;
		setHeldClass(servlet.getClass());
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
	
    private void makeUnavailable(UnavailableException e)
    {
        if (_unavailableEx==e && _unavailable!=0)
            return;

        _servletHandler.getServletContext().log("unavailable",e);

        _unavailableEx=e;
        _unavailable=-1;
        if (e.isPermanent())
            _unavailable=-1;
        else
        {
            if (_unavailableEx.getUnavailableSeconds()>0)
                _unavailable=System.currentTimeMillis()+1000*_unavailableEx.getUnavailableSeconds();
            else
                _unavailable=System.currentTimeMillis()+5000; // TODO configure
        }
    }
	
    @SuppressWarnings("serial")
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
		
		@SuppressWarnings("unchecked")
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
    
    /** Link a user role.
     * Translate the role name used by a servlet, to the link name
     * used by the container.
     * @param name The role name as used by the servlet
     * @param link The role name as used by the container.
     */
    public synchronized void setUserRoleLink(String name,String link)
    {
        if (_roleMap==null)
            _roleMap=new HashMap<String, String>();
        _roleMap.put(name,link);
    }

    /* ------------------------------------------------------------ */
    /** get a user role link.
     * @param name The name of the role
     * @return The name as translated by the link. If no link exists,
     * the name is returned.
     */
    public String getUserRoleLink(String name)
    {
        if (_roleMap==null)
            return name;
        String link= _roleMap.get(name);
        return (link==null)?name:link;
    }

    /* ------------------------------------------------------------ */
    public Map<String, String> getRoleMap()
    {
        return _roleMap == null? ServletHolder.NO_MAPPED_ROLES : _roleMap;
    }
    
    @Override
    public String getContextPath()
    {
        return _config.getServletContext().getContextPath();
    }

    /* ------------------------------------------------------------ */
    /**
     * @see org.eclipse.jetty.server.UserIdentity.Scope#getRoleRefMap()
     */
    @Override
    public Map<String, String> getRoleRefMap()
    {
        return _roleMap;
    }

    /* ------------------------------------------------------------ */
    @ManagedAttribute(value="role to run servlet as", readonly=true)
    public String getRunAsRole()
    {
        return _runAsRole;
    }

    /* ------------------------------------------------------------ */
    public void setRunAsRole(String role)
    {
        _runAsRole = role;
    }
}
