// ========================================================================
// Copyright 2011-2012 NEXCOM Systems
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
package org.cipango.server.security;

import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServletResponse;

import org.cipango.server.SipHandler;
import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.handler.SipHandlerWrapper;
import org.cipango.server.security.SipAuthenticator.AuthConfiguration;
import org.cipango.server.servlet.SipServletHolder;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject("SIP security handler")
public abstract class SipSecurityHandler<T> extends SipHandlerWrapper implements SipHandler, AuthConfiguration
{
	public enum IdentityAssertionScheme
	{
		P_ASSERTED_IDENTITY("P-Asserted-Identity"),
		IDENTITY("Identity");
		
		private String _name;
		
		private IdentityAssertionScheme(String name)
		{
			_name = name;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public static IdentityAssertionScheme getByName(String name)
		{
			for (IdentityAssertionScheme scheme : values())
				if (scheme.getName().equalsIgnoreCase(name))
					return scheme;
			return null;
		}
	}
	
	
	private static final Logger LOG = Log.getLogger(SipSecurityHandler.class);
	
    private SipAuthenticator.Factory _authenticatorFactory=new DefaultAuthenticatorFactory();
	private String _realmName;
	private String _authMethod;
    private LoginService _loginService;
    private boolean _discoveredLoginService;
    private final Map<String,String> _initParameters=new HashMap<String,String>();
    private SipAuthenticator _authenticator;
    private IdentityService _identityService;
    private IdentityAssertionScheme _identityAssertionScheme;
    private boolean _identityAssertionRequired;
    
        
    @Override
    protected void doStart()
        throws Exception
    {
        // copy security init parameters
        ContextHandler.Context context =ContextHandler.getCurrentContext();
        if (context!=null)
        {
			Enumeration<String> names=context.getInitParameterNames();
            while (names!=null && names.hasMoreElements())
            {
                String name =names.nextElement();
                if (name.startsWith("org.eclipse.jetty.security.") &&
                        getInitParameter(name)==null)
                    setInitParameter(name,context.getInitParameter(name));
            }
        }
        
        // complicated resolution of login and identity service to handle
        // many different ways these can be constructed and injected.
        
        if (_loginService==null)
        {
            _loginService=findLoginService();
            _discoveredLoginService=true;
        }
        
        if (_identityService==null)
        {
            if (_loginService!=null)
                _identityService=_loginService.getIdentityService();

            if (_identityService==null)
                _identityService=findIdentityService();
            
            if (_identityService==null && _realmName!=null)
                _identityService=new DefaultIdentityService();
        }
        
        if (_loginService!=null)
        {
            if (_loginService.getIdentityService()==null)
                _loginService.setIdentityService(_identityService);
            else if (_loginService.getIdentityService()!=_identityService)
                throw new IllegalStateException("LoginService has different IdentityService to "+this);
        }
        
        if (_authenticator==null && _authenticatorFactory!=null && _identityService!=null)
        {
            _authenticator=_authenticatorFactory.getAuthenticator(getServer(),ContextHandler.getCurrentContext(),this, _identityService, _loginService);
            if (_authenticator!=null)
                _authMethod=_authenticator.getAuthMethod();
        }

        if (_authenticator==null)
        {
            if (_realmName!=null)
            {
                LOG.warn("No ServerAuthentication for "+this);
                throw new IllegalStateException("No ServerAuthentication");
            }
        }
        else
        {
            _authenticator.setAuthConfiguration(this);
            if (_authenticator instanceof LifeCycle)
                ((LifeCycle)_authenticator).start();
        }

        super.doStart();
    }

    @Override
    protected void doStop() throws Exception
    {
    	if (_discoveredLoginService)
        {
            removeBean(_loginService);
            _loginService = null;
        }
    	
        super.doStop();
        
        
    }
	
    /* ------------------------------------------------------------ */
    /** Get the loginService.
     * @return the loginService
     */
    @ManagedAttribute(name="Login service", readonly=true)
    public LoginService getLoginService()
    {
        return _loginService;
    }

    /* ------------------------------------------------------------ */
    /** Set the loginService.
     * @param loginService the loginService to set
     */
    public void setLoginService(LoginService loginService)
    {
        if (isStarted())
            throw new IllegalStateException("Started");
        updateBean(_loginService,loginService);
        _loginService = loginService;
    }

    /**
     * @return the realmName
     */
    @ManagedAttribute(name="Realm name", readonly=true)
    public String getRealmName()
    {
        return _realmName;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param realmName the realmName to set
     * @throws IllegalStateException if the SecurityHandler is running
     */
    public void setRealmName(String realmName)
    {
        if (isRunning())
            throw new IllegalStateException("running");
        _realmName = realmName;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return the authMethod
     */
    @ManagedAttribute(name="Authentication method", readonly=true)
    public String getAuthMethod()
    {
        return _authMethod;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param authMethod the authMethod to set
     * @throws IllegalStateException if the SecurityHandler is running
     */
    public void setAuthMethod(String authMethod)
    {
        if (isRunning())
            throw new IllegalStateException("running");
        _authMethod = authMethod;
    }
    
    /* ------------------------------------------------------------ */
    protected LoginService findLoginService()
    {
        Collection<LoginService> list = getServer().getBeans(LoginService.class);
        
        for (LoginService service : list)
            if (service.getName()!=null && service.getName().equals(getRealmName()))
                return service;
        if (list.size() == 1)
            return list.iterator().next();
        return null;
    }
    
    protected IdentityService findIdentityService()
    {
        return getServer().getBean(IdentityService.class);
    }
    
    protected abstract T prepareConstraintInfo(SipServletHolder holder, SipRequest request);

    /* ------------------------------------------------------------ */
    protected abstract boolean checkUserDataPermissions(SipServletHolder holder, SipRequest request, T constraintInfo) throws IOException;

    /* ------------------------------------------------------------ */
    protected abstract boolean isAuthMandatory(SipRequest baseRequest, T constraintInfo);
    
    protected abstract boolean isProxyMode(SipRequest baseRequest, T constraintInfo);

    /* ------------------------------------------------------------ */
    protected abstract boolean checkSipResourcePermissions(SipServletHolder holder, SipRequest request, T constraintInfo,
                                                           UserIdentity userIdentity) throws IOException;



	public void handle(SipMessage message) throws IOException, ServletException
	{
		if (message instanceof SipRequest && _authenticator != null 
				&& !((SipRequest) message).isAck()
				&& !((SipRequest) message).isCancel())
		{
			SipRequest request = (SipRequest) message;
			SipServletHolder holder = request.getHandler();
			if (holder == null)
			{
				// a 404 Not found will sent by servlet handler
				LOG.debug("No holder for session " + request.session());
			}
			else
			{	
				T constraintInfo = prepareConstraintInfo(holder, request);
				
				LOG.debug("Got constraint: {} for holder {}", constraintInfo, holder.getName());
				
				// Check data constraints
	            if (!checkUserDataPermissions(holder, request, constraintInfo))
	            {
	                if (!request.isHandled())
	                {
	                	SipServletResponse response = request.createResponse(SipServletResponse.SC_FORBIDDEN);
	                    response.send();
	                }
	                return;
	            }
	            
	            // is Auth mandatory?
	            boolean isAuthMandatory = isAuthMandatory(request, constraintInfo);
	            
	            if (isAuthMandatory)
	            {
		            UserIdentity user = _authenticator.authenticate(request, isProxyMode(request, constraintInfo), isAuthMandatory);
		            request.setUserIdentity(user);
		            if (user == null)
		            	return;
		            
		            boolean authorized=checkSipResourcePermissions(holder, request, constraintInfo, user);
	                if (!authorized)
	                {
	                	SipServletResponse response = request.createResponse(SipServletResponse.SC_FORBIDDEN, "!role");
	                    response.send();
	                    return;
	                }
	            }
			}
		}
		
		getHandler().handle(message);	
	}
	
    /* ------------------------------------------------------------ */
    public String getInitParameter(String key)
    {
        return _initParameters.get(key);
    }
    
    /* ------------------------------------------------------------ */
    public Set<String> getInitParameterNames()
    {
        return _initParameters.keySet();
    }
    
    /* ------------------------------------------------------------ */
    /** Set an initialization parameter.
     * @param key
     * @param value
     * @return previous value
     * @throws IllegalStateException if the SecurityHandler is running
     */
    public String setInitParameter(String key, String value)
    {
        if (isRunning())
            throw new IllegalStateException("running");
        return _initParameters.put(key,value);
    }

	public IdentityService getIdentityService()
	{
		return _identityService;
	}
	
    /** Set the identityService.
     * @param identityService the identityService to set
     */
    public void setIdentityService(IdentityService identityService)
    {
        if (isStarted())
            throw new IllegalStateException("Started");
        _identityService = identityService;
    }
    
    @ManagedAttribute(value="SIP Authenticator", readonly=true)
    public SipAuthenticator getAuthenticator()
    {
        return _authenticator;
    }

    /* ------------------------------------------------------------ */
    /** Set the authenticator.
     * @param authenticator
     * @throws IllegalStateException if the SecurityHandler is running
     */
    public void setAuthenticator(SipAuthenticator authenticator)
    {
        if (isStarted())
            throw new IllegalStateException("Started");
        _authenticator = authenticator;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return the authenticatorFactory
     */
    public SipAuthenticator.Factory getAuthenticatorFactory()
    {
        return _authenticatorFactory;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param authenticatorFactory the authenticatorFactory to set
     * @throws IllegalStateException if the SecurityHandler is running
     */
    public void setAuthenticatorFactory(SipAuthenticator.Factory authenticatorFactory)
    {
        if (isRunning())
            throw new IllegalStateException("running");
        _authenticatorFactory = authenticatorFactory;
    }

	public IdentityAssertionScheme getIdentityAssertionScheme()
	{
		return _identityAssertionScheme;
	}

	public void setIdentityAssertionScheme(IdentityAssertionScheme identityAssertionScheme)
	{
		_identityAssertionScheme = identityAssertionScheme;
	}

	public boolean isIdentityAssertionRequired()
	{
		return _identityAssertionRequired;
	}

	public void setIdentityAssertionRequired(boolean identityAssertionRequired)
	{
		_identityAssertionRequired = identityAssertionRequired;
	}

}
