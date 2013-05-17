// ========================================================================
// Copyright 2008-2009 NEXCOM Systems
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

package org.cipango.kaleo.sip.presence;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

import org.cipango.kaleo.Constants;
import org.cipango.kaleo.URIUtil;
import org.cipango.kaleo.event.ContentHandler;
import org.cipango.kaleo.event.Subscription;
import org.cipango.kaleo.event.Subscription.Reason;
import org.cipango.kaleo.event.Subscription.State;
import org.cipango.kaleo.presence.PresenceEventPackage;
import org.cipango.kaleo.presence.Presentity;
import org.cipango.kaleo.presence.SoftState;
import org.cipango.kaleo.presence.policy.PolicyManager;
import org.cipango.kaleo.presence.policy.PolicyManager.SubHandling;
import org.cipango.kaleo.presence.watcherinfo.WatcherInfoEventPackage;
import org.cipango.kaleo.presence.watcherinfo.WatcherResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PresenceServlet extends SipServlet
{
	private static final long serialVersionUID = 1L;

	private final Logger _log = LoggerFactory.getLogger(PresenceServlet.class);
	
	private PresenceEventPackage _presence;
	private WatcherInfoEventPackage _watcherInfo;
	private PolicyManager _policyManager;
	//private Notifier<Presentity> _notifier;
	
	public void init()
	{
		_presence = (PresenceEventPackage) getServletContext().getAttribute(PresenceEventPackage.class.getName());
		_watcherInfo = (WatcherInfoEventPackage) getServletContext().getAttribute(WatcherInfoEventPackage.class.getName());
		_policyManager = (PolicyManager) getServletContext().getAttribute(PolicyManager.class.getName());
	}
	
	protected void doPublish(SipServletRequest publish) throws ServletException, IOException
	{
		Presentity presentity = null;
		try
		{
			String event = publish.getHeader(Constants.EVENT);
	
			if (event == null || !event.equals(_presence.getName()))
			{
				SipServletResponse response = publish.createResponse(SipServletResponse.SC_BAD_EVENT);
				response.addHeader(Constants.ALLOW_EVENTS, _presence.getName());
				response.send();
				return;
			}
	
			String uri = URIUtil.toCanonical(publish.getRequestURI());
	
			int expires = publish.getExpires();
			if (expires != -1)
			{
				if(expires != 0)
				{
					if (expires < _presence.getMinStateExpires())
					{
						SipServletResponse response = publish.createResponse(SipServletResponse.SC_INTERVAL_TOO_BRIEF);
						response.addHeader(Constants.MIN_EXPIRES, Integer.toString(_presence.getMinStateExpires()));
						response.send();
						return;
					}
					else if (expires > _presence.getMaxStateExpires())
						expires = _presence.getMaxStateExpires();
				}
			}
			else
			{
				expires = _presence.getDefaultStateExpires();
			}
	
			String contentType = publish.getContentType();
	
			List<String> supported = _presence.getSupportedContentTypes();
			if (contentType != null && !(supported.contains(contentType)))
			{
				SipServletResponse response = publish.createResponse(SipServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
				for (String s : supported)
					response.addHeader(Constants.ACCEPT, s);
				response.send();
				return;
			}
	
			byte[] raw = publish.getRawContent();
			Object content = null;
	
			if (raw != null)
			{
				if (contentType == null)
				{
					SipServletResponse response = publish.createResponse(SipServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
					for (String s : supported)
						response.addHeader(Constants.ACCEPT, s);
					response.send();
					return;
				}
				ContentHandler<?> handler = _presence.getContentHandler(contentType);		
				try
				{
					content = handler.getContent(raw);
				}
				catch (Exception e)
				{
					if (_log.isDebugEnabled())
						_log.debug("invalid content {}", e);
					
					publish.createResponse(SipServletResponse.SC_BAD_REQUEST).send();
					return;
				}
			}
			String etag = publish.getHeader(Constants.SIP_IF_MATCH);
			long now = System.currentTimeMillis();
	
			presentity = _presence.get(uri);
		

			if (etag == null)
			{
				if (content == null)
				{
					publish.createResponse(SipServletResponse.SC_BAD_REQUEST).send();
					return;
				}
				SoftState state = presentity.addState(contentType, content, now + expires*1000);
				
				if (_log.isDebugEnabled())
					_log.debug("added state {} to presentity {}", state.getETag(), presentity);
				
				SipServletResponse response = publish.createResponse(SipServletResponse.SC_OK);
				response.setExpires(expires);
				response.setHeader(Constants.SIP_ETAG, state.getETag());
				response.send();
			}
			else
			{
				SoftState state = presentity.getState(etag);
				if (state == null)
				{
					publish.createResponse(SipServletResponse.SC_CONDITIONAL_REQUEST_FAILED).send();
					return;
				}
				if (expires == 0)
				{
					if (_log.isDebugEnabled())
						_log.debug("removed state {} from presentity {}", state.getETag(), presentity);
					
					presentity.removeState(etag);
				}
				else
				{
					if (content != null)
					{
						presentity.modifyState(state, contentType, content, now + expires*1000);
						
						if (_log.isDebugEnabled())
							_log.debug("modified state {} (new etag {}) from presentity {}", 
									new Object[] {etag, state.getETag(), presentity});
					}
					else
					{
						presentity.refreshState(state, now + expires*1000);
						if (_log.isDebugEnabled())
							_log.debug("refreshed state {} (new etag {}) from presentity {}", 
									new Object[] {etag, state.getETag(), presentity});
					}
				}
				SipServletResponse response = publish.createResponse(SipServletResponse.SC_OK);
				response.setExpires(expires);
				response.setHeader(Constants.SIP_ETAG, state.getETag());
				response.send();
			}
		}
		finally
		{
			if (presentity != null)
				_presence.put(presentity);
			publish.getApplicationSession().invalidate();
		}
	}

	protected void doSubscribe(SipServletRequest subscribe) throws ServletException, IOException
	{
		String event = subscribe.getHeader(Constants.EVENT);
		
		if (_presence.getName().equals(event))
			doPresenceSubscribe(subscribe);
		else if (_watcherInfo.getName().equals(event))
			doWatcherInfoSubscribe(subscribe);
		else
		{
			SipServletResponse response = subscribe.createResponse(SipServletResponse.SC_BAD_EVENT);
			response.addHeader(Constants.ALLOW_EVENTS, _presence.getName());
			response.send();
			response.getApplicationSession().invalidate();
			return;
		}		
	}
	
	protected void doPresenceSubscribe(SipServletRequest subscribe) throws ServletException, IOException
	{	
		if(!checkAcceptHeader(subscribe, false))
			return;
				
		int expires = subscribe.getExpires();
		if (expires != -1)
		{
			if (expires != 0)
			{
				if (expires < _presence.getMinExpires())
				{
					SipServletResponse response = subscribe.createResponse(SipServletResponse.SC_INTERVAL_TOO_BRIEF);
					response.addHeader(Constants.MIN_EXPIRES, Integer.toString(_presence.getMinExpires()));
					response.send();
					response.getApplicationSession().invalidate();
					return;
				}
				else if (expires > _presence.getMaxExpires())
				{
					expires = _presence.getMaxExpires();
				}
			}
		}
		else 
		{
			expires = _presence.getDefaultExpires();
		}
		
		SipSession session = subscribe.getSession();
		String uri = null;
		
		if (subscribe.isInitial())
		{
			uri = URIUtil.toCanonical(subscribe.getRequestURI());
		}
		else
		{
			uri = (String) session.getAttribute(Constants.SUBSCRIPTION_ATTRIBUTE);
			if (uri == null)
			{
				subscribe.createResponse(SipServletResponse.SC_CALL_LEG_DONE).send();
				subscribe.getApplicationSession().invalidate();
				return;
			}
		}
				
		Presentity presentity = _presence.get(uri);
		
		String subscriberUri = null;
		if (subscribe.getAddressHeader(Constants.P_ASSERTED_IDENTITY) != null)
			subscriberUri = URIUtil.toCanonical(subscribe.getAddressHeader(Constants.P_ASSERTED_IDENTITY).getURI());
		else
			subscriberUri = URIUtil.toCanonical(subscribe.getFrom().getURI());
		
		try
		{			
			SubHandling subHandling = _policyManager.getPolicy(subscriberUri, presentity);
			if (subHandling == SubHandling.BLOCK)
			{
				_log.debug("Reject presence subscription from {} to {} due to policy", subscriberUri, presentity.getUri());
				SipServletResponse response = subscribe.createResponse(SipServletResponse.SC_FORBIDDEN);
				response.send();
				return;
			}
			
			Subscription subscription = null;
			
			if (expires == 0)
			{
				subscription = presentity.removeSubscription(session.getId());
				
				if (subscription == null)
				{
					subscription = new Subscription(presentity, session, -1, subscriberUri);
					subscription.addListener(_watcherInfo.getSubscriptionListener());
				}
				else
				{
					subscription.setExpirationTime(System.currentTimeMillis());
					if (_log.isDebugEnabled())
						_log.debug("removed presence subscription {} to presentity {}", 
							subscription.getSession().getId(), presentity.getUri());
				}	
				if (subHandling == SubHandling.CONFIRM)
					subscription.setState(Subscription.State.WAITING, Reason.TIMEOUT, subHandling == SubHandling.ALLOW);
				else
					subscription.setState(Subscription.State.TERMINATED, Reason.TIMEOUT, subHandling == SubHandling.ALLOW);
			}
			else
			{
				long now = System.currentTimeMillis();
				
				subscription = presentity.getSubscription(session.getId());
		
				if (subscription == null)
				{
					subscription = new Subscription(presentity, session, now + expires*1000, subscriberUri);
					subscription.addListener(_watcherInfo.getSubscriptionListener());
					presentity.addSubscription(subscription);
					
					switch (subHandling)
					{
					case ALLOW:
						subscription.setState(State.ACTIVE, Reason.SUBSCRIBE, true);
						break;
					case CONFIRM:
						subscription.setState(State.PENDING, Reason.SUBSCRIBE, false);
						break;
					case POLITE_BLOCK:
						subscription.setState(State.ACTIVE, Reason.SUBSCRIBE, false);
						break;
					default:
						break;
					}
					
					session.setAttribute(Constants.SUBSCRIPTION_ATTRIBUTE, uri);
					
					if (_log.isDebugEnabled())
						_log.debug("added presence subscription {} to presentity {}", 
								subscription.getId(), presentity.getUri());
				}
				else 
				{
					subscription.setExpirationTime(now + expires * 1000);
					
					if (_log.isDebugEnabled())
						_log.debug("refreshed presence subscription {} to presentity {}",
								subscription.getSession().getId(), presentity.getUri());
				}
			}
			
			int code = (subscription.getState() != Subscription.State.PENDING) ? 
					SipServletResponse.SC_OK : SipServletResponse.SC_ACCEPTED;
			
			SipServletResponse response = subscribe.createResponse(code);
			response.setExpires(expires);
			response.send();
			
			// Ensure 200/SUBSCRIBE is received before NOTIFY
			try { Thread.sleep(50); } catch (Exception e) {}
			_presence.notify(subscription);
		}
		finally
		{
			_presence.put(presentity);
		}
	}
	
	protected void doWatcherInfoSubscribe(SipServletRequest subscribe) throws ServletException, IOException
	{	
		if(!checkAcceptHeader(subscribe, true))
			return;
				
		int expires = subscribe.getExpires();
		if (expires != -1)
		{
			if (expires != 0)
			{
				if (expires < _watcherInfo.getMinExpires())
				{
					SipServletResponse response = subscribe.createResponse(SipServletResponse.SC_INTERVAL_TOO_BRIEF);
					response.addHeader(Constants.MIN_EXPIRES, Integer.toString(_presence.getMinExpires()));
					response.send();
					response.getApplicationSession().invalidate();
					return;
				}
				else if (expires > _watcherInfo.getMaxExpires())
				{
					expires = _watcherInfo.getMaxExpires();
				}
			}
		}
		else 
		{
			expires = _watcherInfo.getDefaultExpires();
		}
		
		SipSession session = subscribe.getSession();
		String uri = null;
		
		if (subscribe.isInitial())
		{
			uri = URIUtil.toCanonical(subscribe.getRequestURI());
		}
		else
		{
			uri = (String) session.getAttribute(Constants.SUBSCRIPTION_ATTRIBUTE);
			if (uri == null)
			{
				subscribe.createResponse(SipServletResponse.SC_CALL_LEG_DONE).send();
				subscribe.getApplicationSession().invalidate();
				return;
			}
		}
				
		WatcherResource resource = _watcherInfo.get(uri);
		
		String subscriberUri = null;
		if (subscribe.getAddressHeader(Constants.P_ASSERTED_IDENTITY) != null)
			subscriberUri = URIUtil.toCanonical(subscribe.getAddressHeader(Constants.P_ASSERTED_IDENTITY).getURI());
		else
			subscriberUri = URIUtil.toCanonical(subscribe.getFrom().getURI());
		
		try
		{
			Subscription subscription = null;
			
			if (expires == 0)
			{
				subscription = resource.removeSubscription(session.getId());
				
				if (subscription == null)
					subscription = new Subscription(resource, session, -1, subscriberUri);
				else
				{
					subscription.setExpirationTime(System.currentTimeMillis());
					if (_log.isDebugEnabled())
						_log.debug("removed presence.winfo subscription {} to resource {}", 
							subscription.getSession().getId(), resource.getUri());
				}	
				subscription.setState(Subscription.State.TERMINATED, Reason.TIMEOUT);
			}
			else
			{
				long now = System.currentTimeMillis();
				
				subscription = resource.getSubscription(session.getId());
		
				if (subscription == null)
				{
					subscription = new Subscription(resource, session, now + expires*1000, subscriberUri);
					subscription.setState(State.ACTIVE, Reason.SUBSCRIBE);
					resource.addSubscription(subscription);
					
					session.setAttribute(Constants.SUBSCRIPTION_ATTRIBUTE, uri);
					
					if (_log.isDebugEnabled())
						_log.debug("added presence.winfo subscription {} to resource {}", 
								subscription.getSession().getId(), resource.getUri());
				}
				else 
				{
					subscription.setExpirationTime(now + expires * 1000);
					
					if (_log.isDebugEnabled())
						_log.debug("refreshed presence.winfo subscription {} to resource {}",
								subscription.getSession().getId(), resource.getUri());
				}
			}
			
			int code = (subscription.getState() != Subscription.State.PENDING) ? 
					SipServletResponse.SC_OK : SipServletResponse.SC_ACCEPTED;
			
			SipServletResponse response = subscribe.createResponse(code);
			response.setExpires(expires);
			response.send();
				
			_watcherInfo.notify(subscription);
		}
		finally
		{
			_watcherInfo.put(resource);
		}
	}
	
	private boolean checkAcceptHeader(SipServletRequest subscribe, boolean winfo) throws IOException
	{
		
		List<String> supported;
		if (winfo)
			supported = _watcherInfo.getSupportedContentTypes();
		else
			supported = _presence.getSupportedContentTypes();
		
		Iterator<String> it = subscribe.getHeaders(Constants.ACCEPT);
		if (!it.hasNext())
			return true;
		while (it.hasNext())
		{
			String contentType = it.next();
			if (contentType.equals("") ||  supported.contains(contentType))
				return true;
		}
		SipServletResponse response = subscribe.createResponse(SipServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
		for (String s : supported)
			response.addHeader(Constants.ACCEPT, s);

		response.send();
		response.getApplicationSession().invalidate();
		return false;
	}
}
