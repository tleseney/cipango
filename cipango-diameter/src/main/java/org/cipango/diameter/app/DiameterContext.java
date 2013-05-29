// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
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
package org.cipango.diameter.app;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.cipango.diameter.api.DiameterErrorEvent;
import org.cipango.diameter.api.DiameterErrorListener;
import org.cipango.diameter.api.DiameterListener;
import org.cipango.diameter.api.DiameterServletMessage;
import org.cipango.diameter.node.DiameterAnswer;
import org.cipango.diameter.node.DiameterHandler;
import org.cipango.diameter.node.DiameterMessage;
import org.cipango.diameter.node.DiameterRequest;
import org.cipango.diameter.node.TimeoutHandler;
import org.cipango.server.session.SessionManager.AppSessionIf;
import org.cipango.server.sipapp.SipAppContext;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.webapp.WebAppContext;

public class DiameterContext implements DiameterHandler, TimeoutHandler
{
	private static final Logger LOG = Log.getLogger(DiameterContext.class);
	
	private SipAppContext _defaultContext;
	private Map<String, DiameterAppContext> _diameterListeners = new ConcurrentHashMap<String, DiameterAppContext>();

	private Method _handleMsg;
	private Method _noAnswerReceived;
	
	public DiameterContext()
	{
		try 
		{
		 _handleMsg = DiameterListener.class.getMethod("handle", DiameterServletMessage.class);
		 _noAnswerReceived = DiameterErrorListener.class.getMethod("noAnswerReceived", DiameterErrorEvent.class);
		} 
        catch (NoSuchMethodException e)
        {
            throw new ExceptionInInitializerError(e);
        }
	}
	
	public void addListeners(WebAppContext context, DiameterListener[] listeners, DiameterErrorListener[] errorListeners)
	{
		_diameterListeners.put(context.getContextPath(), new DiameterAppContext(listeners, errorListeners));
		if (_defaultContext == null)
			_defaultContext = context.getBean(SipAppContext.class);
	}
	
	public void addListener(WebAppContext context, DiameterListener listener)
	{
		DiameterAppContext diameterAppContext = _diameterListeners.get(context.getContextPath());
		if (diameterAppContext == null)
		{
			diameterAppContext = new DiameterAppContext();
			_diameterListeners.put(context.getContextPath(), diameterAppContext);
		}
		
		diameterAppContext.addDiameterListener(listener);
		
		if (_defaultContext == null)
			_defaultContext = context.getBean(SipAppContext.class);
	}
	
	public void removeListener(WebAppContext context, DiameterListener listener)
	{
		DiameterAppContext diameterAppContext = _diameterListeners.get(context.getContextPath());
		if (diameterAppContext == null)
			return;
		
		diameterAppContext.removeDiameterListener(listener);
	}
	
	public void addErrorListener(WebAppContext context, DiameterErrorListener listener)
	{
		DiameterAppContext diameterAppContext = _diameterListeners.get(context.getContextPath());
		if (diameterAppContext == null)
		{
			diameterAppContext = new DiameterAppContext();
			_diameterListeners.put(context.getContextPath(), diameterAppContext);
		}
		
		diameterAppContext.addErrorListener(listener);
		
		if (_defaultContext == null)
			_defaultContext = context.getBean(SipAppContext.class);
	}
	
	public void removeErrorListener(WebAppContext context, DiameterErrorListener listener)
	{
		DiameterAppContext diameterAppContext = _diameterListeners.get(context.getContextPath());
		if (diameterAppContext == null)
			return;
		
		diameterAppContext.removeErrorListener(listener);
	}
	
	public void removeListeners(WebAppContext context)
	{
		_diameterListeners.remove(context.getContextPath());
		
		if (_defaultContext == context.getBean(SipAppContext.class))
			_defaultContext = null;
	}
	//TODO init default context
	
	public void handle(DiameterMessage message) throws IOException
	{
		List<DiameterListener> listeners = null;
		SipAppContext context = null;
		if (message instanceof DiameterAnswer)
			context = ((DiameterAnswer) message).getRequest().getContext();
		
		AppSessionIf appSession = (AppSessionIf) message.getApplicationSession();       
        if (context == null && appSession != null)
            context = appSession.getAppSession().getContext();
		
		if (context == null)
			context = _defaultContext;
		
		if (context != null)
		{
			DiameterAppContext ctx = _diameterListeners.get(context.getWebAppContext().getContextPath());
			if (ctx != null)
				listeners = ctx.getDiameterListeners();
		}

		if (listeners != null && !listeners.isEmpty())
			context.fire(appSession == null ? null : appSession.getAppSession(),
					listeners, _handleMsg, message);
		else
			LOG.warn("No diameter listeners for context {} to handle message {}", 
					context == null ? "" : context.getName(), message);	
	}
	
	public void fireNoAnswerReceived(DiameterRequest request, long timeout)
	{
		List<DiameterErrorListener> listeners = null;
		SipAppContext context = request.getContext();
        
		AppSessionIf appSession = (AppSessionIf) request.getApplicationSession();       
        if (context == null && appSession != null)
            context = appSession.getAppSession().getContext();
		
        if (context == null)
        {
            context = _defaultContext;
			LOG.debug("Use default context {} to handle timeout for {}", 
					context == null ? "" : context.getName(), request);	
        }
            
		if (context != null)
		{
			DiameterAppContext ctx = _diameterListeners.get(context.getWebAppContext().getContextPath());
			if (ctx != null)
				listeners = ctx.getErrorListeners();
		}
		
		if (listeners != null && !listeners.isEmpty())
			context.fire(appSession == null ? null : appSession.getAppSession(), 
					listeners, _noAnswerReceived, new DiameterErrorEvent(request, timeout));
        else
            LOG.warn("Could not notify timeout for diameter request {} as no listeners defined for context {}", 
					request, context == null ? "" : context.getName());			
	}
	
}

class DiameterAppContext
{
	private final List<DiameterListener> _diameterListeners = new CopyOnWriteArrayList<>();
	private final List<DiameterErrorListener> _errorListeners = new CopyOnWriteArrayList<>();
	
	public DiameterAppContext()
	{
	}
	
	public DiameterAppContext(DiameterListener[] listeners, DiameterErrorListener[] errorListeners)
	{
		_diameterListeners.addAll(Arrays.asList(listeners));
		_errorListeners.addAll(Arrays.asList(errorListeners));
	}
	
	public void addDiameterListener(DiameterListener l)
	{
		_diameterListeners.add(l);
	}
	
	public void removeDiameterListener(DiameterListener l)
	{
		_diameterListeners.remove(l);
	}
	
	public void addErrorListener(DiameterErrorListener l)
	{
		_errorListeners.add(l);
	}
	
	public void removeErrorListener(DiameterErrorListener l)
	{
		_errorListeners.remove(l);
	}

	public List<DiameterListener> getDiameterListeners()
	{
		return _diameterListeners;
	}

	public List<DiameterErrorListener> getErrorListeners()
	{
		return _errorListeners;
	}
}

