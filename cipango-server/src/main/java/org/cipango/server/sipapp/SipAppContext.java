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

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.AuthInfo;
import javax.servlet.sip.ConvergedHttpSession;
import javax.servlet.sip.Parameterable;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionAttributeListener;
import javax.servlet.sip.SipApplicationSessionListener;
import javax.servlet.sip.SipErrorListener;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletContextEvent;
import javax.servlet.sip.SipServletListener;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSessionAttributeListener;
import javax.servlet.sip.SipSessionListener;
import javax.servlet.sip.SipSessionsUtil;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TimerListener;
import javax.servlet.sip.TimerService;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;

import org.cipango.server.SipConnector;
import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.SipServer;
import org.cipango.server.handler.SipHandlerWrapper;
import org.cipango.server.log.event.Events;
import org.cipango.server.security.AuthInfoImpl;
import org.cipango.server.security.ConstraintSecurityHandler;
import org.cipango.server.security.SipSecurityHandler;
import org.cipango.server.servlet.SipDispatcher;
import org.cipango.server.servlet.SipServletHandler;
import org.cipango.server.servlet.SipServletHolder;
import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.Session;
import org.cipango.server.session.SessionHandler;
import org.cipango.server.session.SessionManager;
import org.cipango.server.session.SessionManager.AppSessionIf;
import org.cipango.server.session.SessionManager.ApplicationSessionScope;
import org.cipango.server.session.http.ConvergedSessionManager;
import org.cipango.server.session.scoped.ScopedAppSession;
import org.cipango.server.session.scoped.ScopedTimer;
import org.cipango.server.util.ReadOnlySipURI;
import org.cipango.sip.AddressImpl;
import org.cipango.sip.ParameterableImpl;
import org.cipango.sip.SipHeader;
import org.cipango.sip.SipMethod;
import org.cipango.sip.SipURIImpl;
import org.cipango.sip.URIFactory;
import org.cipango.util.StringUtil;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.util.ArrayUtil;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.webapp.WebAppContext;

@ManagedObject("SIP application context")
public class SipAppContext extends SipHandlerWrapper 
{
	private static final Logger LOG = Log.getLogger(SipAppContext.class);
	
	public static final int VERSION_10 = 10;
	public static final int VERSION_11 = 11;
    
	public final static String[] EXTENSIONS = { "MESSAGE", "INFO", "SUBSCRIBE", "NOTIFY", "UPDATE", "PUBLISH", "REFER",  "100rel" };
	
	public static final String[] SUPPORTED_RFC = new String[] {
		"2976", // The SIP INFO Method
		"3261", // SIP: Session Initiation Protocol
		"3262", // Reliability of Provisional Responses
		"3263", // SIP Locating SIP Servers
		"3265", // (SIP)-Specific Event Notification. 
		"3311", // SIP UPDATE Method
		"3327", // SIP Extension Header Field for Registering Non-Adjacent Contacts (Path header)
		"3428", // SIP Extension for Instant Messaging  
		"3515", // SIP Refer Method
		"3903", // SIP Extension for Event State Publication
		"5658", // Addressing Record-Route Issues in SIP
		"6026"	// Correct Transaction Handling for 2xx Responses to Session Initiation Protocol (SIP) INVITE Requests
	};
	
	public static final String EXTERNAL_INTERFACES = "org.cipango.externalOutboundInterfaces";
	public final static String SIP_DEFAULTS_XML="org/cipango/server/sipapp/sipdefault.xml";
	
	private static final ThreadLocal<SipAppContext> __context = new ThreadLocal<SipAppContext>();
	
	private String _name;
	private String _defaultsDescriptor=SIP_DEFAULTS_XML;
	private final List<String> _overrideDescriptors = new ArrayList<String>();
	
    private int _proxyTimeout = -1;
	


	private final SessionHandler _sessionHandler;
	private SipServletHandler _servletHandler;
	private SipSecurityHandler<?> _securityHandler;
	private int _specVersion;
	
	private WebAppContext _context;
	private ServletContext _sContext;
	private MetaData _metaData;
	
    private final List<TimerListener> _timerListeners = new CopyOnWriteArrayList<TimerListener>();
    private final List<SipErrorListener> _errorListeners = new CopyOnWriteArrayList<SipErrorListener>();
    private final List<SipServletListener> _servletListeners =  new CopyOnWriteArrayList<SipServletListener>();
    private final List<ServletContextListener> _contextListeners = new CopyOnWriteArrayList<ServletContextListener>();
    
    private final SipFactory _sipFactory;
    private TimerService _timerService;
    private SipSessionsUtil _sipSessionsUtil = new SessionUtil();
    
    protected final List<Decorator> _decorators= new ArrayList<Decorator>();
    
    private String _id;
    private ServerListener _serverListener;
    private Throwable _unavailableException;
    
    public static SipAppContext getCurrentContext()
    {
        return __context.get();
    }
	
	public SipAppContext()
	{
		_sessionHandler = new SessionHandler();
		_servletHandler = new SipServletHandler();
		_securityHandler = new ConstraintSecurityHandler();
		_metaData = new MetaData();
		_sipFactory = new Factory();
		_timerService = new TimerServiceImpl();
	}
	
	protected void doStart() throws Exception
	{
		ClassLoader oldClassLoader = null;
		Thread currentThread = null;
		 SipAppContext oldContext = __context.get();
		try
		{
			if (getClassLoader() != null)
            {
                currentThread = Thread.currentThread();
                oldClassLoader = currentThread.getContextClassLoader();
                currentThread.setContextClassLoader(getClassLoader());
            }
			
	         __context.set(this);
			
			relinkHandlers();
	
			_metaData.resolve(SipAppContext.this);
	
			super.doStart();
			
			// As SipAppContext is started by a servletContextListener, if some servletContextListeners have been defined
			// in sip, its will not be added too late on WebAppContext, so invoke them here
	    	if (!_contextListeners.isEmpty())
	    	{
	    		ServletContextEvent sce = new ServletContextEvent(getServletContext());
				for (ServletContextListener listener : _contextListeners)
					listener.contextInitialized(sce);
	    	}
	    				
			_servletHandler.initialize();
	
			for (Decorator decorator : _decorators)
			{
				if (getServletHandler().getServlets() != null)
					for (SipServletHolder holder : getServletHandler().getServlets())
						decorator.decorateServletHolder(holder);
			}
		}
		catch (Throwable e)
		{
			if (_name == null)
				setName(getDefaultName());
			LOG.warn("Failed to start SipAppContext {}: {}", getName(), e);
			_unavailableException = e;
			if (_context != null)
				_context.setAvailable(false);
			throw e;
		}
		finally
		{
			__context.set(oldContext);
			if (currentThread != null && oldClassLoader != null)
				currentThread.setContextClassLoader(oldClassLoader);
			
			if (isAvailable())
			{
				if (getServer().isStarted())
					serverStarted();
			}
			else
			{
				Throwable exception = _unavailableException;
				if (exception == null)
					exception = _context.getUnavailableException();

				Events.fire(Events.DEPLOY_FAIL,
						 "Unable to deploy application " + getName()
						 + ": " + exception);
			}
			
		}
	}
	
	protected void doStop() throws Exception
	{
		super.doStop();	
				
		if (getServer() != null && _serverListener != null)
			getServer().removeLifeCycleListener(_serverListener);
		_serverListener = null;
		_unavailableException = null;
	}
	
	private void relinkHandlers()
	{
		SipHandlerWrapper handler = this;

		// Skip any injected handlers
		while (handler.getHandler() instanceof HandlerWrapper)
		{
			SipHandlerWrapper wrapper = (SipHandlerWrapper) handler.getHandler();
			if (wrapper instanceof SessionHandler)
				break;
			handler = wrapper;
		}

		if (getSessionHandler() != null)
		{
			handler.setHandler(_sessionHandler);
			handler = _sessionHandler;
		}
		
		if (getSecurityHandler() != null)
		{
			handler.setHandler(_securityHandler);
			handler = _securityHandler;
		}

		if (getServletHandler() != null)
		{
			handler.setHandler(_servletHandler);
		}
	}
	
	public void serverStarted()
	{	
		ClassLoader oldClassLoader = null;
		Thread currentThread = null;
		
		if (getClassLoader() != null)
		{
			currentThread = Thread.currentThread();
			oldClassLoader = currentThread.getContextClassLoader();
			currentThread.setContextClassLoader(getClassLoader());
		}
		try
		{
			List<SipURI> outbounds = new ArrayList<SipURI>();
			List<SipURI> externals = new ArrayList<SipURI>();

			SipConnector[] connectors = getServer().getConnectors();
			
			if (connectors != null)
			{
				for (SipConnector connector : connectors) 
				{
					SipURI uri = new SipURIImpl(null, connector.getAddress().getHostAddress(), connector.getPort());
					if (!outbounds.contains(uri))
						outbounds.add(new ReadOnlySipURI(uri));
					if (!externals.contains(connector.getURI()))
						externals.add(new ReadOnlySipURI(connector.getURI()));
				}
			}
			if (_context != null) // Could be null is some tests
			{
				_context.setAttribute(SipServlet.OUTBOUND_INTERFACES, Collections.unmodifiableList(outbounds));
				_context.setAttribute(EXTERNAL_INTERFACES, Collections.unmodifiableList(externals));
			}
			
			SipServletHolder[] holders = getServletHandler().getServlets();
			if (holders != null)
			{
				for (SipServletHolder holder : holders)
				{
					if (holder.getServletInstance() != null && holder.getServletInstance() instanceof SipServlet)
					{
						fireServletInitialized((SipServlet) holder.getServletInstance());
					}
				}
			}
		}
		finally
		{
			if (getClassLoader() != null)
			{
				currentThread.setContextClassLoader(oldClassLoader);
			}
		}
	}
	
	public void fireServletInitialized(SipServlet servlet)
	{
		for (SipServletListener l : _servletListeners)
		{
			try
			{
				l.servletInitialized(new SipServletContextEvent(servlet.getServletContext(), servlet));
			}
			catch (Throwable t)
			{
				LOG.debug(t);
			}
		}
	}

	
	@SuppressWarnings("deprecation")
	public void setWebAppContext(WebAppContext context)
	{
		if (_context != context)
		{
			_context = context;
			// As ServletContextListener is added, this class already managed by WebAppContext.
			context.addBean(this, false);
			
			_context.setAttribute(SipServlet.PRACK_SUPPORTED, Boolean.TRUE);
		    _context.setAttribute(SipServlet.SIP_FACTORY, getSipFactory());
		    _context.setAttribute(SipServlet.TIMER_SERVICE, getTimerService());
		    _context.setAttribute(SipServlet.SIP_SESSIONS_UTIL, getSipSessionsUtil());
		    _context.setAttribute(SipServlet.SUPPORTED, Collections.unmodifiableList(Arrays.asList(EXTENSIONS)));
		    _context.setAttribute(SipServlet.SUPPORTED_RFCs, Collections.unmodifiableList(Arrays.asList(SUPPORTED_RFC)));
			
			// Add first a bean to WebAppContext, when this bean is started, it adds a
			// ServletContextListener that will start this SipAppContext (so this listener will the
			// last initialized).
			// This mechanism ensures that all ServletContextListeners (including those defined in
			// web.xml) are initialized before servlets.
			// @see CIPANGO-199
		    _context.addBean(new AbstractLifeCycle()
			{

				@Override
				protected void doStart() throws Exception
				{
					// In order to ensure that ServletContextListener is called before HTTP and SIP servlet initialization
					// a ServletContextListener is added to WebAppContext in order for SipAppContext to be started
					_context.addEventListener(new ServletContextListener()
					{
						
						@Override
						public void contextInitialized(ServletContextEvent sce)
						{
							try
							{
								SipAppContext.this.start();
							}
							catch (Exception e)
							{
								throw new RuntimeException(e);
							}
							
						}
						
						@Override
						public void contextDestroyed(ServletContextEvent sce)
						{
							try
							{
								SipAppContext.this.stop();
							}
							catch (Exception e)
							{
								LOG.warn("Failed to stop SipAppContext " + getName(), e);
							}
						}
					});
				}
		    	
			});
							
			_sContext = new SContext(_context.getServletContext());
	
			if (context.getConfigurations() == null  && context.getConfigurationClasses() == WebAppContext.getDefaultConfigurationClasses())
			{
				String[] classes = ArrayUtil.addToArray(WebAppContext.getDefaultConfigurationClasses(), 
						"org.cipango.server.sipapp.SipXmlConfiguration",
						String.class);
				context.setConfigurationClasses(classes);
			}
		}
	}
	
	/**
	 * Sets the WebAppContext.
	 * If <code>converged</code>, then the HTTP session will be {@link ConvergedHttpSession}.
	 * 
	 * @param context The associated WebAppContext
	 * @param converged if <code>true</code>, replace the WebAppContext session handler by
	 *  an instance of {@link org.cipango.server.session.http.SessionHandler}.
	 *  
	 *  @see ConvergedSessionManager
	 *  @see org.cipango.server.session.http.SessionHandler
	 */
	public void setWebAppContext(WebAppContext context, boolean converged)
	{
		setWebAppContext(context);
		if (converged)
			context.setSessionHandler(new org.cipango.server.session.http.SessionHandler(new ConvergedSessionManager()));
	}
	

	@Override
	public void handle(SipMessage message) throws IOException, ServletException
	{
		if (!isAvailable() && message instanceof SipServletRequest)
		{
			SipServletResponse response = ((SipServletRequest) message).createResponse(SipServletResponse.SC_SERVICE_UNAVAILABLE);
			response.addHeader(SipHeader.REASON.asString(), "Application " + getName() + " unavailable");
			response.send();
			return;
		}
		
		ClassLoader oldClassLoader = null;
		Thread currentThread = null;
		
		if (getClassLoader() != null)
		{
			currentThread = Thread.currentThread();
			oldClassLoader = currentThread.getContextClassLoader();
			currentThread.setContextClassLoader(getClassLoader());
		}
		
		try
		{
			super.handle(message);
		}
		finally
		{
			if (getClassLoader() != null)
				currentThread.setContextClassLoader(oldClassLoader);
		}
	}

	
	public ServletContext getServletContext()
	{
		return _sContext;
	}
	
	public void setName(String name)
	{
		_name = name;
		
		if (_name != null)
		{
			try
			{
				MessageDigest md = MessageDigest.getInstance("MD5");
				byte[] bytes = md.digest(name.getBytes(StringUtil.__UTF8_CHARSET));
				int i = 0;
				for (byte b : bytes)
					i =  i * 33 + b;
				_id = StringUtil.toBase62String2(Math.abs(i)).substring(0, 3);
			}
			catch (Exception e)
			{
				LOG.warn("Unable to create ID", e);
			}
		}
	}
	
	@ManagedAttribute(value="Name", readonly=true)
	public String getName()
	{
		return _name;
	}
	
	@ManagedAttribute("Context ID")
	public String getContextId()
	{
		return _id;
	}
	
    public String getDefaultName()
    {
    	String name = _context == null ? null : _context.getContextPath();
		if (name != null && name.startsWith("/"))
			name = name.substring(1);
		return name;
    }
				
	public EventListener[] getEventListeners()
    {
        return _context.getEventListeners();
    }
	

    public void addEventListener(EventListener listener)
    {
    	if (_context != null)
    		_context.addEventListener(listener);
    	
    	if (listener instanceof TimerListener)
            _timerListeners.add((TimerListener) listener);
        if (listener instanceof SipErrorListener)
            _errorListeners.add((SipErrorListener) listener);
        if (listener instanceof SipServletListener)
        	_servletListeners.add((SipServletListener) listener);
        if (listener instanceof ServletContextListener)
        	_contextListeners.add((ServletContextListener) listener);
    	
        if ((listener instanceof SipApplicationSessionAttributeListener)
            || (listener instanceof SipSessionListener)
            || (listener instanceof SipSessionAttributeListener)
            || (listener instanceof SipApplicationSessionListener))
        {
            if (_sessionHandler!=null)
                _sessionHandler.addEventListener(listener);
        }
    }
    
    public void removeEventListener(EventListener listener)
    {
    	if (_context != null)
    		_context.removeEventListener(listener);
    	
    	if (listener instanceof TimerListener)
            _timerListeners.remove((TimerListener) listener);
        if (listener instanceof SipErrorListener)
            _errorListeners.remove((SipErrorListener) listener);
        if (listener instanceof SipServletListener)
        	_servletListeners.remove((SipServletListener) listener);
    	
        if ((listener instanceof SipApplicationSessionAttributeListener)
            || (listener instanceof SipSessionListener)
            || (listener instanceof SipSessionAttributeListener)
            || (listener instanceof SipApplicationSessionListener))
        {
            if (_sessionHandler!=null)
                _sessionHandler.removeEventListener(listener);
        }
    }

    public void setEventListeners(EventListener[] eventListeners)
    {   	
    	if (_context != null)
    		_context.setEventListeners(eventListeners);
    	if (_sessionHandler!=null)
            _sessionHandler.clearEventListeners();
    	
    	_timerListeners.clear();
    	_errorListeners.clear();
    	_servletListeners.clear();

         if (eventListeners!=null)
             for (EventListener listener : eventListeners)
                 addEventListener(listener);
    }
    
    public ClassLoader getClassLoader()
    {
    	if (getWebAppContext() == null)
    		return null;
    	return getWebAppContext().getClassLoader();
    }
    
    public void fire(ApplicationSession applicationSession, List<? extends EventListener> listeners, Method method, Object... args)
    {
		ClassLoader oldClassLoader = null;
		Thread currentThread = null;
		
		if (getClassLoader() != null)
		{
			currentThread = Thread.currentThread();
			oldClassLoader = currentThread.getContextClassLoader();
			currentThread.setContextClassLoader(getClassLoader());
		}
		ApplicationSessionScope scope = null;
		
		if (applicationSession != null)
			scope = applicationSession.getSessionManager().openScope(applicationSession);
		try
		{
			for (EventListener l :listeners)
			{
				try
				{
					method.invoke(l, args);
				}
				catch (Throwable t)
				{
					LOG.warn("Failed to invoke listener " + l, t);
				}
			}
		}
		finally
		{
			if (scope != null)
				scope.close();
			if (getClassLoader() != null)
				currentThread.setContextClassLoader(oldClassLoader);
		}
    }
		
	public int getProxyTimeout()
	{
		return _proxyTimeout;
	}

	public void setProxyTimeout(int proxyTimeout)
	{
		_proxyTimeout = proxyTimeout;
	}
	
    /**
     * The default descriptor is a sip.xml format file that is applied to the context before the standard WEB-INF/sip.xml
     * @return Returns the defaultsDescriptor.
     */
	public String getDefaultsDescriptor()
	{
		return _defaultsDescriptor;
	}

	public void setDefaultsDescriptor(String defaultsDescriptor)
	{
		_defaultsDescriptor = defaultsDescriptor;
	}
	
	 /**
     * The override descriptor is a sip.xml format file that is applied to the context after the standard WEB-INF/sip.xml
     * @return Returns the Override Descriptor.
     */
    public String getOverrideDescriptor()
    {
        if (_overrideDescriptors.size()!=1)
            return null;
        return _overrideDescriptors.get(0);
    }

    /**
     * The override descriptor is a sip.xml format file that is applied to the context after the standard WEB-INF/sip.xml
     * @param overrideDescriptor The overrideDescritpor to set.
     */
    public void setOverrideDescriptor(String overrideDescriptor)
    {
        _overrideDescriptors.clear();
        _overrideDescriptors.add(overrideDescriptor);
    }
	
	public List<String> getOverrideDescriptors()
	{
		return _overrideDescriptors;
	}
		
	@ManagedAttribute(value="Web app context", readonly=true)
	public WebAppContext getWebAppContext()
	{
		return _context;
	}
	
	@ManagedAttribute(value="specification version", readonly=true)
	public int getSpecVersion()
	{
		return _specVersion;
	}
	
	@ManagedAttribute(value="specification version", readonly=true)
	public String getSpecVersionAsString()
	{
		if (_specVersion == VERSION_10)
			return "1.0";
		if (_specVersion == VERSION_11)
			return "1.1";
		return "Unknown";
	}

	public void setSpecVersion(int specVersion)
	{
		_specVersion = specVersion;
	}

	@ManagedAttribute(value="context servlet handler", readonly=true)
	public SipServletHandler getServletHandler()
	{
		return _servletHandler;
	}
	
	public boolean hasSipServlets()
    {
    	SipServletHolder[] holders = getServletHandler().getServlets();
    	return holders != null && holders.length != 0;
    }
	
    public void addDecorator(Decorator decorator)
    {
        _decorators.add(decorator);
        _context.addDecorator(decorator);
    }
    
	public MetaData getMetaData()
	{
		return _metaData;
	}

	public SipFactory getSipFactory()
	{
		return _sipFactory;
	}

	public TimerService getTimerService()
	{
		return _timerService;
	}

	public SipSessionsUtil getSipSessionsUtil()
	{
		return _sipSessionsUtil;
	}
	
	@ManagedAttribute(value="context session handler", readonly=true)
	public SessionHandler getSessionHandler()
	{
		return _sessionHandler;
	}
	
	
	@ManagedAttribute(value="context security handler", readonly=true)
	public SipSecurityHandler<?> getSecurityHandler()
	{
		return _securityHandler;
	}

	public List<TimerListener> getTimerListeners()
	{
		return _timerListeners;
	}
	
	public List<SipErrorListener> getSipErrorListeners()
	{
		return _errorListeners;
	}
	
	@ManagedAttribute
	public Throwable getUnavailableException()
	{
		return _unavailableException;
	}

	@ManagedAttribute
	public boolean isAvailable()
	{
		return _unavailableException == null && (_context == null || _context.isAvailable());
	}
	
	@Override
	public void setServer(SipServer server)
	{
		super.setServer(server);
		_serverListener = new ServerListener();
		server.addLifeCycleListener(_serverListener);
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "@" + getName();
	}
	
	class SContext extends ServletContextProxy
	{
		
		public SContext(ServletContext servletContext)
		{
			super(_context.getServletContext());
		}

		@Override
		public RequestDispatcher getNamedDispatcher(String name)
		{
			if (_servletHandler != null)
            {
                SipServletHolder holder =  _servletHandler.getHolder(name);
                if (holder != null)
                	return new SipDispatcher(SipAppContext.this, holder);
            }
            return super.getNamedDispatcher(name);
		}

		@Override
		public String getServerInfo()
		{
			// FIXME what should be returned ?
			return "Cipango-3.0";
			//return super.getServerInfo();
		}
	}
		
    private class SessionUtil implements SipSessionsUtil
    {

		@Override
		public SipApplicationSession getApplicationSessionById(String id)
		{
			return new ScopedAppSession(getSessionHandler().getSessionManager().getApplicationSession(id));
		}

		@Override
		public SipApplicationSession getApplicationSessionByKey(String key, boolean create)
		{
			if (key == null)
				throw new NullPointerException("Null key");

			SessionManager manager = _sessionHandler.getSessionManager();
			String id = manager.getApplicationSessionIdByKey(key);
			ApplicationSession appSession = manager.getApplicationSession(id);
			if (appSession == null && !create)
				return null;
			if (appSession == null)
				appSession = manager.createApplicationSession(id);
			return new ScopedAppSession(appSession);
		}

		@Override
		public SipSession getCorrespondingSipSession(SipSession session, String headerName)
		{
			// TODO Auto-generated method stub
			return null;
		}

    }
    
    private class Factory implements SipFactory
    {

		@Override
		public Address createAddress(String address) throws ServletParseException
		{
			try
			{
				AddressImpl addr = new AddressImpl(address);
				addr.parse();
				return addr;
			}
			catch (ParseException e)
			{
				throw new ServletParseException(e);
			}
		}

		@Override
		public Address createAddress(URI uri)
		{
			return new AddressImpl(uri);
		}

		@Override
		public Address createAddress(URI uri, String displayName)
		{
			AddressImpl addr = new AddressImpl(uri);
			addr.setDisplayName(displayName);
			return addr;
		}

		@Override
		public SipApplicationSession createApplicationSession()
		{
			return new ScopedAppSession(getSessionHandler().getSessionManager().createApplicationSession());
		}

		@Override
		public SipApplicationSession createApplicationSessionByKey(String key)
		{
			SessionManager manager = _sessionHandler.getSessionManager();
			String id = manager.getApplicationSessionIdByKey(key);
			ApplicationSession appSession = manager.getApplicationSession(id);
			if (appSession == null)
				appSession = manager.createApplicationSession(id);
			return new ScopedAppSession(appSession);
		}

		@Override
		public AuthInfo createAuthInfo()
		{
			return new AuthInfoImpl();
		}

		@Override
		public Parameterable createParameterable(String s) throws ServletParseException
		{
			try
			{
				return new ParameterableImpl(s);
			}
			catch (ParseException e)
			{
				throw new ServletParseException(e);
			}
		}

		@Override
		public SipServletRequest createRequest(SipServletRequest srcRequest, boolean sameCallId)
		{
			SipRequest origRequest = (SipRequest) srcRequest;
			ApplicationSession appSession = origRequest.appSession();
			Address local = (Address) origRequest.getFrom().clone();
			local.setParameter(AddressImpl.TAG, appSession.newUASTag());

			Address remote = (Address) origRequest.to().clone();
			remote.removeParameter(AddressImpl.TAG);

			String callId = null;

			if (sameCallId)
				callId = origRequest.getCallId();
			else
				callId = appSession.getSessionManager().newCallId();

			Session session = appSession.createUacSession(callId, local, remote);
			session.setHandler(getServletHandler().getDefaultServlet());

			SipRequest request = session.getUa().createRequest((SipRequest) srcRequest);
			request.setRoutingDirective(SipApplicationRoutingDirective.CONTINUE, srcRequest);

			return request;
		}

		public SipServletRequest createRequest(SipApplicationSession sipAppSession, String method,
				Address from, Address to)
		{
			ApplicationSession appSession = ((AppSessionIf) sipAppSession).getAppSession();
			
			SipMethod sipMethod = SipMethod.get(method);
			
			if (sipMethod == SipMethod.ACK || sipMethod == SipMethod.CANCEL)
				throw new IllegalArgumentException("Method cannot be ACK nor CANCEL");

			Address local = (Address) from.clone();
			Address remote = (Address) to.clone();

			local.setParameter(AddressImpl.TAG, appSession.newUASTag());
			remote.removeParameter(AddressImpl.TAG);

			String cid = appSession.getSessionManager().newCallId();

			Session session = appSession.createUacSession(cid, local, remote);
			session.setHandler(getServletHandler().getDefaultServlet());

			SipRequest request = (SipRequest) session.createRequest(method);
			request.setRoutingDirective(SipApplicationRoutingDirective.NEW, null);

			return request;
		}

		@Override
		public SipServletRequest createRequest(SipApplicationSession appSession, 
                String method, URI from, URI to) 
        {
            return createRequest(appSession, method, createAddress(from), createAddress(to));
        }
		
		@Override
        public SipServletRequest createRequest(SipApplicationSession appSession,
                String method, String from, String to) throws ServletParseException 
        {
            return createRequest(appSession, method, createAddress(from), createAddress(to));
        }
        

		@Override
		public SipURI createSipURI(String user, String host)
		{
			return new SipURIImpl(user, host, -1);
		}

		@Override
		public URI createURI(String uri) throws ServletParseException
		{
			try
			{
				return URIFactory.parseURI(uri);
			}
			catch (ParseException e)
			{
				throw new ServletParseException(e);
			}
		}
    	
    }
    
    private class TimerServiceImpl implements TimerService
    {
        public ServletTimer createTimer(SipApplicationSession session, long delay, boolean isPersistent, Serializable info) 
        {
        	ApplicationSession appSession = ((AppSessionIf) session).getAppSession();
            return new ScopedTimer(appSession, delay, isPersistent, info);
        }

        public ServletTimer createTimer(SipApplicationSession session, long delay, long period, boolean fixedDelay, boolean isPersistent, Serializable info) 
        {
        	ApplicationSession appSession = ((AppSessionIf) session).getAppSession();
        	return new ScopedTimer(appSession, delay, period, fixedDelay, isPersistent, info);
        }
    }
    
    private class ServerListener extends AbstractLifeCycleListener
    {
		@Override
		public void lifeCycleStarted(LifeCycle event)
		{
			serverStarted();
		}	
    }
	
	public interface Decorator extends org.eclipse.jetty.servlet.ServletContextHandler.Decorator
	{
		void decorateServletHolder(SipServletHolder servlet) throws ServletException;
	}


}
