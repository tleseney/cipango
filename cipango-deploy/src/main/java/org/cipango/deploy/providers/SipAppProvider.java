// ========================================================================
// Copyright 2010-2012 NEXCOM Systems
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
package org.cipango.deploy.providers;

import org.cipango.server.session.SessionManager;
import org.cipango.server.sipapp.SipAppContext;
import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.deploy.providers.WebAppProvider;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;

public class SipAppProvider extends WebAppProvider
{
	
	private String _defaultsSipDescriptor;
	private String _sessionManagerClass;
	
	@Override
    public ContextHandler createContextHandler(final App app) throws Exception
    {
        WebAppContext context = (WebAppContext) super.createContextHandler(app);
                
        SipAppContext sipAppContext = new SipAppContext();
        sipAppContext.setWebAppContext(context, true);
        context.addBean(sipAppContext);
        if (_defaultsSipDescriptor != null)
        	sipAppContext.setDefaultsDescriptor(_defaultsSipDescriptor);
        if (_sessionManagerClass != null)
        {
        	try
        	{
        		Class<?> clazz = Class.forName(_sessionManagerClass);
        		if (!SessionManager.class.isAssignableFrom(clazz))
        			throw new IllegalArgumentException("Class " + _sessionManagerClass + " is does not extends "
        					+ SessionManager.class.getName());
        		SessionManager sessionManager = (SessionManager) clazz.newInstance();
        		sipAppContext.getSessionHandler().setSessionManager(sessionManager);
        	}
        	catch (Throwable e)
        	{
        		throw new IllegalArgumentException("Failed to use class " + _sessionManagerClass + " as session manager", e);
        	}
        }
        return context; 
    }

	public String getDefaultsSipDescriptor()
	{
		return _defaultsSipDescriptor;
	}

	public void setDefaultsSipDescriptor(String defaultsSipDescriptor)
	{
		_defaultsSipDescriptor = defaultsSipDescriptor;
	}

	public String getSessionManagerClass()
	{
		return _sessionManagerClass;
	}

	public void setSessionManagerClass(String sessionManagerClass)
	{
		_sessionManagerClass = sessionManagerClass;
	}
}
