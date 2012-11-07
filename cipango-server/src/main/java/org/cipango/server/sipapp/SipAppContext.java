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
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventListener;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.AuthInfo;
import javax.servlet.sip.Parameterable;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionAttributeListener;
import javax.servlet.sip.SipApplicationSessionListener;
import javax.servlet.sip.SipErrorListener;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletListener;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSessionAttributeListener;
import javax.servlet.sip.SipSessionListener;
import javax.servlet.sip.SipSessionsUtil;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TimerListener;
import javax.servlet.sip.TimerService;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;

import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.handler.AbstractSipHandler;
import org.cipango.server.servlet.SipDispatcher;
import org.cipango.server.servlet.SipServletHandler;
import org.cipango.server.servlet.SipServletHolder;
import org.cipango.server.session.AppSessionIf;
import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.Session;
import org.cipango.server.session.SessionHandler;
import org.cipango.sip.AddressImpl;
import org.cipango.sip.ParameterableImpl;
import org.cipango.sip.SipMethod;
import org.cipango.sip.SipURIImpl;
import org.cipango.sip.URIFactory;
import org.cipango.sip.URIImpl;
import org.eclipse.jetty.util.ArrayUtil;
import org.eclipse.jetty.util.LazyList;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.webapp.WebAppContext;

public class SipAppContext extends AbstractSipHandler 
{
	private static final Logger LOG = Log.getLogger(SipAppContext.class);
	
	public static final int VERSION_10 = 10;
	public static final int VERSION_11 = 11;
    
	public final static String[] EXTENSIONS = { "MESSAGE", "INFO", "SUBSCRIBE", "NOTIFY", "UPDATE", "PUBLISH", "REFER",  "100rel" };
	
	public static final String[] SUPPORTED_RFC = new String[] {
		"2976", // The SIP INFO Method
		"3261", // SIP: Session Initiation Protocol
		"3262", // Reliability of Provisional Responses
		"3265", // (SIP)-Specific Event Notification. 
		"3311", // (SIP) UPDATE Method
		"3327", // SIP) Extension Header Field for Registering Non-Adjacent Contacts (Path header)
		"3428", // SIP Extension for Instant Messaging  
		"3515", // SIP Refer Method
		"3903", // SIP Extension for Event State Publication
		"5658", // Addressing Record-Route Issues in SIP
		"6026"	// Correct Transaction Handling for 2xx Responses to Session Initiation Protocol (SIP) INVITE Requests
	};
	
	public static final String EXTERNAL_INTERFACES = "org.cipango.externalOutboundInterfaces";
	public final static String SIP_DEFAULTS_XML="org/cipango/server/sipapp/sipdefault.xml";
	
	private String _name;
	private String _defaultsDescriptor=SIP_DEFAULTS_XML;
	private final List<String> _overrideDescriptors = new ArrayList<String>();
	
  

	private int _sessionTimeout = -1;
    private int _proxyTimeout = -1;
	


	private SessionHandler _sessionHandler;
	private SipServletHandler _servletHandler;
	private int _specVersion;
	
	private WebAppContext _context;
	private ServletContext _sContext;
	private MetaData _metaData;
	
    private TimerListener[] _timerListeners = new TimerListener[0];
    private SipApplicationSessionListener[] _appSessionListeners = new SipApplicationSessionListener[0];
    private SipErrorListener[] _errorListeners = new SipErrorListener[0];
    private SipApplicationSessionAttributeListener[] _appSessionAttributeListeners = new SipApplicationSessionAttributeListener[0];
    private SipSessionListener[] _sessionListeners = new SipSessionListener[0];
    private SipSessionAttributeListener[] _sessionAttributeListeners = new SipSessionAttributeListener[0];
    private SipServletListener[] _servletListeners = new SipServletListener[0];
    
    private final SipFactory _sipFactory;
    private TimerService _timerService;
    private SipSessionsUtil _sipSessionsUtil = new SessionUtil();
    
    protected final List<Decorator> _decorators= new ArrayList<Decorator>();
    private Method _sipApplicationKeyMethod;
	
	public SipAppContext()
	{
		_sessionHandler = new SessionHandler(this);
		_servletHandler = new SipServletHandler();
		_metaData = new MetaData();
		_sipFactory = new Factory();
	}
	

	@Override
	protected void doStart() throws Exception
	{
		
		_sessionHandler.setHandler(_servletHandler);
		_sessionHandler.start();
		super.doStart();
	}
	
	public void setWebAppContext(WebAppContext context)
	{
		_context = context;
		context.addBean(this);
		_sContext = new SContext(_context.getServletContext());
		WebAppContextListener l = new WebAppContextListener();
		context.addLifeCycleListener(l);
		
		// Ensure that lifeCycleStarting is call even if context is starting.
		if (context.isStarting())
			l.lifeCycleStarting(context);
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
	
    public String getDefaultName()
    {
    	String name = _context.getContextPath();
		if (name != null && name.startsWith("/"))
			name = name.substring(1);
		return name;
    }
		
	public SipApplicationSessionListener[] getSipApplicationSessionListeners()
	{
		return _appSessionListeners;
	}

	public TimerListener[] getTimerListeners()
	{
		return _timerListeners;
	}

	public SipErrorListener[] getSipErrorListeners()
	{
		return _errorListeners;
	}

	public SipApplicationSessionAttributeListener[] getSipApplicationSessionAttributeListeners()
	{
		return _appSessionAttributeListeners;
	}

	public SipSessionListener[] getSipSessionListeners()
	{
		return _sessionListeners;
	}

	public SipSessionAttributeListener[] getSessionAttributeListeners()
	{
		return _sessionAttributeListeners;
	}
	
	public SipServletHandler getSipServletHandler()
	{
		return _servletHandler;
	}
	
    /** 
     * Add EventListener
     * Conveniance method that calls {@link #setEventListeners(EventListener[])}
     * @param listener
     */
    public void addEventListener(EventListener listener)
    {
        setEventListeners((EventListener[])ArrayUtil.addToArray(getEventListeners(), listener, EventListener.class));
    }
	
	public EventListener[] getEventListeners()
    {
        return _context.getEventListeners();
    }

    public void setEventListeners(EventListener[] eventListeners)
    {   	
        _context.setEventListeners(eventListeners);
        
        Object timerListeners = null;
        Object appSessionListeners = null;
        Object errorListeners = null;
        Object appSessionAttributeListeners = null;
        Object sessionListeners = null;
        Object sessionAttributesListeners = null;
        Object servletListeners = null;
        
        for (int i = 0; eventListeners != null && i < eventListeners.length; i++)
        {
            EventListener listener = eventListeners[i];
            if (listener instanceof TimerListener)
                timerListeners = LazyList.add(timerListeners, listener);
            if (listener instanceof SipApplicationSessionListener)
                appSessionListeners = LazyList.add(appSessionListeners, listener);
            if (listener instanceof SipErrorListener)
                errorListeners = LazyList.add(errorListeners, listener);
            if (listener instanceof SipApplicationSessionAttributeListener)
            	appSessionAttributeListeners = LazyList.add(appSessionAttributeListeners, listener);
            if (listener instanceof SipSessionListener)
            	sessionListeners = LazyList.add(sessionListeners, listener);
            if (listener instanceof SipSessionAttributeListener)
            	sessionAttributesListeners = LazyList.add(sessionAttributesListeners, listener);
            if (listener instanceof SipServletListener)
            	servletListeners = LazyList.add(servletListeners, listener);
        }
        _timerListeners = (TimerListener[]) 
            LazyList.toArray(timerListeners, TimerListener.class);
        _appSessionListeners = (SipApplicationSessionListener[]) 
            LazyList.toArray(appSessionListeners, SipApplicationSessionListener.class);
        _errorListeners = (SipErrorListener[])
            LazyList.toArray(errorListeners, SipErrorListener.class);
        _appSessionAttributeListeners = (SipApplicationSessionAttributeListener[])
        	LazyList.toArray(appSessionAttributeListeners, SipApplicationSessionAttributeListener.class);
        _sessionListeners = (SipSessionListener[])
        	LazyList.toArray(sessionListeners, SipSessionListener.class);
        _sessionAttributeListeners = (SipSessionAttributeListener[])
        	LazyList.toArray(sessionAttributesListeners, SipSessionAttributeListener.class);
        _servletListeners = (SipServletListener[])
        	LazyList.toArray(servletListeners, SipServletListener.class);
    }
	
	public void handle(SipMessage message) throws IOException, ServletException 
	{
		_sessionHandler.handle(message);
		// TODO Auto-generated method stub
	}
	
	public int getSessionTimeout()
	{
		return _sessionTimeout;
	}

	public void setSessionTimeout(int sessionTimeout)
	{
		_sessionTimeout = sessionTimeout;
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

	public List<String> getOverrideDescriptors()
	{
		return _overrideDescriptors;
	}
		
	public WebAppContext getWebAppContext()
	{
		return _context;
	}
	
	public int getSpecVersion()
	{
		return _specVersion;
	}

	public void setSpecVersion(int specVersion)
	{
		_specVersion = specVersion;
	}

	public SipServletHandler getServletHandler()
	{
		return _servletHandler;
	}
	
	public boolean hasSipServlets()
    {
    	SipServletHolder[] holders = getSipServletHandler().getServlets();
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
	
	private class WebAppContextListener implements LifeCycle.Listener
	{

		@SuppressWarnings("deprecation")
		@Override
		public void lifeCycleStarting(LifeCycle event)
		{
		    _context.setAttribute(SipServlet.PRACK_SUPPORTED, Boolean.TRUE);
		    _context.setAttribute(SipServlet.SIP_FACTORY, getSipFactory());
		    _context.setAttribute(SipServlet.TIMER_SERVICE, getTimerService());
		    _context.setAttribute(SipServlet.SIP_SESSIONS_UTIL, getSipSessionsUtil());
		    _context.setAttribute(SipServlet.SUPPORTED, Collections.unmodifiableList(Arrays.asList(EXTENSIONS)));
		    _context.setAttribute(SipServlet.SUPPORTED_RFCs, Collections.unmodifiableList(Arrays.asList(SUPPORTED_RFC)));
		}

		@Override
		public void lifeCycleStarted(LifeCycle event)
		{
			try
			{
	//	        if (_sipSecurityHandler!=null)
	//	        {
	//	        	_sipSecurityHandler.setHandler(_servletHandler);	            
	//	            _sipSecurityHandler.start(); // FIXME when should it be started
	//	        }
				_metaData.resolve(SipAppContext.this);
				_sessionHandler.setHandler(_servletHandler);
				      		
				_servletHandler.start();
				
				for (Decorator decorator : _decorators)
	    	    {
	    	        if(getSipServletHandler().getServlets()!=null)
	    	            for (SipServletHolder holder:getSipServletHandler().getServlets())
	    	                decorator.decorateServletHolder(holder);
	    	    } 

				
				if (!_context.isAvailable())
		    	{
		    		if (_name == null)
						_name = getDefaultName();
	//				Events.fire(Events.DEPLOY_FAIL, 
	//	        			"Unable to deploy application " + getName()
	//	        			+ ": " + _context.getUnavailableException().getMessage());
		    	}
		    	else if (hasSipServlets())
		    	{
	//	    		getServer().applicationStarted(this);
		    	}
			}
			catch (Exception e) 
			{
				LOG.warn("Failed to start SipAppContext " + getName(), e);
				_context.setAvailable(false);
			}
			
		}

		@Override
		public void lifeCycleFailure(LifeCycle event, Throwable cause)
		{
			// TODO Auto-generated method stub
			
		}

		@Override

		public void lifeCycleStopping(LifeCycle event)
		{
			try
			{
				if (hasSipServlets() && _context.isAvailable())
	//				getServer().applicationStopped(this);
				
						
	//			if (_sipSecurityHandler != null)
	//				_sipSecurityHandler.stop();
					
				_servletHandler.stop();
			}
			catch (Exception e) 
			{
				LOG.warn("Failed to stop SipAppContext " + getName(), e);
			}
		}

		@Override
		public void lifeCycleStopped(LifeCycle event)
		{
			// TODO Auto-generated method stub
			
		}
		
	}
	
    class SessionUtil implements SipSessionsUtil
    {

		@Override
		public SipApplicationSession getApplicationSessionById(String arg0)
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SipApplicationSession getApplicationSessionByKey(String arg0, boolean arg1)
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SipSession getCorrespondingSipSession(SipSession arg0, String arg1)
		{
			// TODO Auto-generated method stub
			return null;
		}

    }
    
    class Factory implements SipFactory
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
			return _sessionHandler.getSessionManager().createApplicationSession();
		}

		@Override
		public SipApplicationSession createApplicationSessionByKey(String arg0)
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public AuthInfo createAuthInfo()
		{
			// TODO Auto-generated method stub
			return null;
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
//        	SipRequest origRequest = (SipRequest) srcRequest;
//        	ApplicationSession appSession = origRequest.appSession();
//        	Address local = (Address) origRequest.getFrom().clone();
//        	local.setParameter(AddressImpl.TAG, appSession.newUASTag());
//        	
//        	Address remote = (Address) origRequest.to().clone();
//        	remote.removeParameter(AddressImpl.TAG);
//        	
//        	String callId = null;
//        	
//        	if (sameCallId)
//        		callId = origRequest.getCallId();
//        	else
//        		callId = appSession.getSessionManager().newCallId();
//        	            
//            Session session = appSession.createUacSession(callId, local, remote);
//            session.setHandler(getSipServletHandler().getDefaultServlet());
//
// FIXME           SipRequest request = session.getUa().createRequest((SipRequest) srcRequest);
//            request.setRoutingDirective(SipApplicationRoutingDirective.CONTINUE, srcRequest);
//            
//            return request;
			return null;
        }

		public SipServletRequest createRequest(SipApplicationSession sipAppSession, String method,
				Address from, Address to)
		{
			ApplicationSession appSession = ((AppSessionIf) sipAppSession).getAppSession();
			
			SipMethod sipMethod = SipMethod.lookAheadGet(ByteBuffer.wrap(method.getBytes()));
			
			if (sipMethod == SipMethod.ACK || sipMethod == SipMethod.CANCEL)
				throw new IllegalArgumentException("Method cannot be ACK nor CANCEL");

			Address local = (Address) from.clone();
			Address remote = (Address) to.clone();

			local.setParameter(AddressImpl.TAG, appSession.newUASTag());
			remote.removeParameter(AddressImpl.TAG);

			String cid = appSession.getSessionManager().newCallId();

			Session session = appSession.createUacSession(cid, local, remote);
			session.setHandler(getSipServletHandler().getDefaultServlet());

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
	
	 public interface Decorator extends org.eclipse.jetty.servlet.ServletContextHandler.Decorator
    {
		 void decorateServletHolder(SipServletHolder servlet) throws ServletException;
    }

	public Method getSipApplicationKeyMethod()
	{
		return _sipApplicationKeyMethod;
	}

	public void setSipApplicationKeyMethod(Method sipApplicationKeyMethod)
	{
		_sipApplicationKeyMethod = sipApplicationKeyMethod;
	}
}
