// ========================================================================
// Copyright 2008-2012 NEXCOM Systems
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

package org.cipango.server.session;

import java.io.IOException;
import java.util.EventListener;

import javax.servlet.ServletException;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipServletResponse;

import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.handler.SipHandlerWrapper;
import org.cipango.server.sipapp.SipAppContext;
import org.cipango.server.transaction.ServerTransaction;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class SessionHandler extends SipHandlerWrapper
{
	public static final String APP_ID = "appid";
	private static final Logger LOG = Log.getLogger(SessionHandler.class);
	private final SessionManager _sessionManager;
	
	public SessionHandler(SipAppContext context)
	{
		_sessionManager = new SessionManager(context);
		addBean(_sessionManager);
	}
	
	
	@Override
	protected void doStart() throws Exception
	{
		_sessionManager.start();
		super.doStart();
	}
	
	public void handle(SipMessage message) throws IOException, ServletException 
	{
		SipRequest request = null;
		Session session = null;
		if (message.isRequest())
		{
			request = (SipRequest) message;
									
			if (request.isInitial())
			{
				ApplicationSession appSession = _sessionManager.createApplicationSession();
				session = appSession.createSession(request);
			}
			else
			{
				String appId = request.getParameter(APP_ID);
				if (appId == null)
				{
					String tag = request.getToTag();
					int i = tag.indexOf('-');
					if (i != -1)
						appId = tag.substring(0,  i);
				}
				if (appId == null)
				{
					notFound(request, "No Application Session Identifier");
					return;
				}
				
				ApplicationSession	appSession = _sessionManager.getApplicationSession(appId);
				
				if (appSession == null)
				{
					notFound(request, "No Application Session");
					return;
				}
				
				session = appSession.getSession(request);
			}
			
			if (session == null)
			{
				notFound(request, "No SIP Session");
				return;
			}
			session.accessed();
			request.setSession(session);
			
			if (!request.isInitial() && session.isUA())
				session.getUa().handleRequest(request);
			
		}

// FIXME		if (!request.isHandled())
		_handler.handle(message);
		
		if (request != null && !request.isInitial() && session.isProxy()  && !request.isCancel())
		{
			Proxy proxy = request.getProxy();
			proxy.proxyTo(request.getRequestURI());
		}
	}
	
	protected void notFound(SipRequest request, String reason)
	{
		if (!request.isAck())
			try
			{
				// In this case, there is no session, so could not use response.send()
				SipResponse response = (SipResponse) request.createResponse(SipServletResponse.SC_CALL_LEG_DONE, reason);
				// FIXME to tag
				((ServerTransaction) response.getTransaction()).send(response);
			}
			catch (Exception e)
			{
				LOG.ignore(e);
			}
	}
	
	protected String getApplicationId(String s)
	{
		return s.substring(s.lastIndexOf('-'));
	}

	public SessionManager getSessionManager()
	{
		return _sessionManager;
	}
	
    public void addEventListener(EventListener listener)
    {
        _sessionManager.addEventListener(listener);
    }
    

    public void removeEventListener(EventListener listener)
    {
        _sessionManager.removeEventListener(listener);
    }

    public void clearEventListeners()
    {
        _sessionManager.clearEventListeners();
    }
	
}
