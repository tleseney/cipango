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

import javax.servlet.sip.SipSession;

import org.eclipse.jetty.util.LazyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Subscription 
{
	public enum State
	{
		INIT("init"), ACTIVE("active"), PENDING("pending"), WAITING("waiting"), TERMINATED("terminated");
		
		private String _name;
		
		private State(String name) { _name = name; }
		public String getName() { return _name; }
	}
	
	public enum Reason 
	{
		DEACTIVATED("deactivated"), PROBATION("probation"), REJECTED("rejected"),
		TIMEOUT("timeout"), GIVEUP("giveup"), NORESOURCE("noresource"),
		SUBSCRIBE("subscribe"), APPROVED("approved");
		
		private String _name;
		
		private Reason(String name) { _name = name; }
		public String getName() { return _name; }
	}
	
	private static Logger __log = LoggerFactory.getLogger(Subscription.class);
	
	private EventResource _resource;
	
	private SipSession _session;
	private State _state = State.INIT;
	private Reason _reason;
	private long _expirationTime;
	private String _subscriberUri;
	private boolean _authorised = true;
	private Object _listeners; //LazyList<SubscriptionListener>
	
	public Subscription(EventResource resource, SipSession session, long expirationTime) 
	{
		_resource = resource;
		_session = session;
		_expirationTime = expirationTime;
	}
	
	public Subscription(EventResource resource, SipSession session, long expirationTime, String subscriberUri) 
	{
		_resource = resource;
		_session = session;
		_expirationTime = expirationTime;
		_subscriberUri = subscriberUri;
	}
	
	public void setExpirationTime(long expirationTime)
	{
		_expirationTime = expirationTime;
	}
	
	public long getExpirationTime()
	{
		return _expirationTime;
	}
	
	public String getId()
	{
		if (_session == null)
			return String.valueOf(hashCode());
		return _session.getId();
	}
	
	public EventResource getResource() 
	{
		return _resource;
	}
	
	public SipSession getSession() 
	{
		return _session;
	}
	
	public State getState() 
	{
		return _state;
	}
	
	public void setState(State state, Reason reason, boolean authorised) 
	{
		State previousState = _state;
		boolean previousAuthorised = _authorised;
		_authorised = authorised;
		_state = state;
		_reason = reason;
		if (previousState != state || previousAuthorised != authorised)
		{
			if (__log.isDebugEnabled())
			{
				if (previousState != state)
					__log.debug("State changed from {} to {} for " + this, previousState, state);
				if (previousAuthorised != authorised)
					__log.debug("Authorization changed from {} to {} for " + this, previousAuthorised, authorised);
			}
			
			for (int i = 0; i < LazyList.size(_listeners); i++)
				((SubscriptionListener) LazyList.get(_listeners, i)).subscriptionStateChanged(this, previousState, state);
		}
	}
	
	public void setState(State state, Reason reason) 
	{
		setState(state, reason, isAuthorized());
	}
	
	public Reason getReason()
	{
		return _reason;
	}
	
	public String getUri()
	{
		return _subscriberUri;
	}
		
	public String toString()
	{
		return _resource.getUri() + "/" + getId();
	}
	public void addListener(SubscriptionListener l)
	{
		if (!LazyList.contains(_listeners, l) && l != null)
			_listeners = LazyList.add(_listeners, l);	
	}
	
	public void removeListener(SubscriptionListener l)
	{
		_listeners = LazyList.remove(_listeners, l);	
	}

	public boolean isAuthorized()
	{
		return _authorised;
	}
}
