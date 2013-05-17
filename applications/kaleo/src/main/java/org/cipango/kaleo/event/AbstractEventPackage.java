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

package org.cipango.kaleo.event;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;

import org.cipango.kaleo.AbstractResource;
import org.cipango.kaleo.AbstractResourceManager;
import org.cipango.kaleo.Constants;
import org.cipango.kaleo.event.Subscription.Reason;

public abstract class AbstractEventPackage<T extends AbstractResource & EventResource> extends AbstractResourceManager<T> implements EventPackage<T>
{
	private int _minExpires = 60;
	private int _maxExpires = 3600;
	private int _defaultExpires = 3600;
	
	private EventNotifier _eventNotifier = new EventNotifier();
	
	public int getMinExpires()
	{
		return _minExpires; 
	}

	public int getMaxExpires()
	{
		return _maxExpires;
	}

	public int getDefaultExpires()
	{
		return _defaultExpires;
	}
	
	public void setMinExpires(int minExpires)
	{
		_minExpires = minExpires;
	}
	
	public void setMaxExpires(int maxExpires)
	{
		_maxExpires = maxExpires;
	}
	
	public void setDefaultExpires(int defaultExpires)
	{
		_defaultExpires = defaultExpires;
	}	
	
	protected EventResourceListener getEventNotifier()
	{
		return _eventNotifier;
	}
	
	@SuppressWarnings("unchecked")
	public void notify(Subscription subscription)
	{
		try
		{
			SipSession session = subscription.getSession();
			if (session.isValid())
			{
				SipServletRequest notify = session.createRequest(Constants.NOTIFY);
				notify.addHeader(Constants.EVENT, getName());
				
				String s = subscription.getState().getName();
				if (subscription.getState() == Subscription.State.ACTIVE 
						|| subscription.getState() == Subscription.State.PENDING)
					s = s + ";expires=" + ((subscription.getExpirationTime()-System.currentTimeMillis()) / 1000);
				notify.addHeader(Constants.SUBSCRIPTION_STATE, s);
				
				State state;
				if (subscription.isAuthorized())
					state = subscription.getResource().getState();
				else
					state = subscription.getResource().getNeutralState();
				preprocessState(session, state);
				ContentHandler handler = getContentHandler(state.getContentType());
				byte[] b = handler.getBytes(state.getContent());
				
				notify.setContent(b, state.getContentType());
				notify.send();
			}
			else
				_log.warn("Could not send notification to {} for event {} as sip session is invalidated", 
						subscription, getName());
		}
		catch (Exception e) 
		{
			_log.warn("Exception while sending notification {}", e);
		}
	}
	
	protected void preprocessState(SipSession session, State state)
	{	
	}
	
	class EventNotifier implements EventResourceListener
	{

		public void stateChanged(EventResource resource)
		{
			if (_log.isDebugEnabled())
				_log.debug("State changed for {} resource {} ", getName(), resource);
			
			for (Subscription subscription : resource.getSubscriptions())
			{
				if (subscription.isAuthorized())
					AbstractEventPackage.this.notify(subscription);
			}
		}
		
		public void subscriptionExpired(Subscription subscription)
		{
			if (subscription.getState() == Subscription.State.PENDING)
				subscription.setState(Subscription.State.WAITING, Reason.TIMEOUT);
			else
				subscription.setState(Subscription.State.TERMINATED, Reason.TIMEOUT);
			AbstractEventPackage.this.notify(subscription);
		}
		
	}
}
