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

package org.cipango.kaleo.location;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.sip.URI;

import org.cipango.kaleo.AbstractResource;
import org.cipango.kaleo.location.event.ContactDocument.Contact.Event;
import org.cipango.kaleo.location.event.RegistrationDocument.Registration.State;
import org.eclipse.jetty.util.LazyList;

public class Registration extends AbstractResource
{
	private List<Binding> _bindings = new ArrayList<Binding>(1);
	private Object _listeners; //LazyList<RegistrationListener>
	
	public Registration(String aor)
	{
		super(aor);
	}
	
	public void addBinding(Binding binding)
	{
		synchronized (_bindings)
		{
			_bindings.add(binding);
		}
		fireBindingChanged(binding, Event.REGISTERED);
	}
	
	public void updateBinding(Binding binding, URI contact, String callId, int cseq, long expirationTime)
	{
		Event.Enum event;
		if (binding.getExpirationTime() < expirationTime)
			event = Event.REFRESHED;
		else
			event = Event.SHORTENED;
		binding.update(contact, callId, cseq, expirationTime);
		fireBindingChanged(binding, event);
	}
	
	public List<Binding> getBindings()
	{
		return _bindings;
	}
	
	public void removeBinding(Binding binding)
	{
		synchronized (_bindings)
		{
			binding.update(binding.getContact(), binding.getCallId(), binding.getCSeq(), System.currentTimeMillis());
			_bindings.remove(binding);
		}
		fireBindingChanged(binding, Event.UNREGISTERED);
	}
	
	public void removeAllBindings()
	{
		synchronized (_bindings)
		{
			_bindings.clear();
		}
		for (int i = 0; i < LazyList.size(_listeners); i++)
			((RegistrationListener) LazyList.get(_listeners, i)).allBindingsRemoved(getUri());

	}
	
	public boolean isDone()
	{
		return _bindings.isEmpty();
	}
	
	public long nextTimeout()
	{
		if (_bindings.size() == 0)
			return -1;
		long time = Long.MAX_VALUE;
		
		for (Binding binding : _bindings)
		{
			if (binding.getExpirationTime() < time)
				time = binding.getExpirationTime();
		}
		return time;
	}
	
	
	private void fireBindingChanged(Binding binding, Event.Enum event)
	{
		State.Enum state;
		if (_bindings.isEmpty())
			state = State.TERMINATED;
		else
			state = State.ACTIVE;
		for (int i = 0; i < LazyList.size(_listeners); i++)
			((RegistrationListener) LazyList.get(_listeners, i)).bindingChanged(getUri(), binding, event, state);
	}
	
	public void doTimeout(long time)
	{
		List<Binding> expired = new ArrayList<Binding>();
		
		synchronized (_bindings)
		{
			Iterator<Binding> it = _bindings.iterator();
			while (it.hasNext())
			{
				Binding binding = it.next();
				if (binding.getExpirationTime() <= time)
				{
					it.remove();
					expired.add(binding);
				}
			}
		}
	}
	
	public void addListener(RegistrationListener l)
	{
		if (!LazyList.contains(_listeners, l) && l != null)
			_listeners = LazyList.add(_listeners, l);	
	}
	
	public void removeListener(RegistrationListener l)
	{
		_listeners = LazyList.remove(_listeners, l);	
	}
}
