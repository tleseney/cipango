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

package org.cipango.kaleo.location.event;

import java.math.BigInteger;

import org.cipango.kaleo.event.AbstractEventResource;
import org.cipango.kaleo.event.State;
import org.cipango.kaleo.location.Binding;
import org.cipango.kaleo.location.RegistrationListener;
import org.cipango.kaleo.location.event.ContactDocument.Contact;
import org.cipango.kaleo.location.event.ContactDocument.Contact.Event;
import org.cipango.kaleo.location.event.ReginfoDocument.Reginfo;
import org.cipango.kaleo.location.event.RegistrationDocument.Registration;

public class RegResource extends AbstractEventResource implements RegistrationListener
{
	private ReginfoDocument _content;
	private State _state;
	
	public RegResource(String uri, org.cipango.kaleo.location.Registration registration)
	{
		super(uri);
		_content = ReginfoDocument.Factory.newInstance();
		Reginfo reginfo = _content.addNewReginfo();
		reginfo.setVersion(BigInteger.ZERO);
		reginfo.setState(org.cipango.kaleo.location.event.ReginfoDocument.Reginfo.State.FULL);
		Registration reg = reginfo.addNewRegistration();
		reg.setAor(uri);
		reg.setId("123");
		if (registration != null && !registration.getBindings().isEmpty())
		{
			reg.setState(org.cipango.kaleo.location.event.RegistrationDocument.Registration.State.ACTIVE);
			for (Binding binding : registration.getBindings())
			{
				Contact contact = reg.addNewContact();
				contact.setUri(binding.getContact().toString());
				contact.setEvent(Event.REGISTERED);
				contact.setId(String.valueOf(binding.getId()));
				contact.setCallid(binding.getCallId());
				contact.setCseq(BigInteger.valueOf(binding.getCSeq()));
				contact.setExpires(BigInteger.valueOf(binding.getExpires()));
			}
		}
		else
			reg.setState(org.cipango.kaleo.location.event.RegistrationDocument.Registration.State.INIT);
		
		_state = new State(RegEventPackage.REGINFO, _content);
	}

	public State getState() 
	{
		return _state;
	}

	public boolean isDone() 
	{
		return (_content.getReginfo().getRegistrationArray(0).getContactArray().length == 0
				&& !hasSubscribers());
	}

	public void bindingChanged(String aor, Binding binding, Event.Enum event, 
			org.cipango.kaleo.location.event.RegistrationDocument.Registration.State.Enum state)
	{
		
		Contact contactModified = null;
		Registration registration = _content.getReginfo().getRegistrationArray(0);
		registration.setState(state);
		for (Contact contact : registration.getContactArray())
		{
			if (contact.getId().equals(String.valueOf(binding.getId())))
			{
				contactModified = contact;
			}
		}
		if (contactModified == null)
		{
			contactModified = registration.addNewContact();
		}
		contactModified.setUri(binding.getContact().toString());
		contactModified.setEvent(event);
		contactModified.setId(String.valueOf(binding.getId()));
		contactModified.setCallid(binding.getCallId());
		contactModified.setCseq(BigInteger.valueOf(binding.getCSeq()));
		contactModified.setExpires(BigInteger.valueOf(binding.getExpires()));

		fireStateChanged();
		
		
		if (event == Event.DEACTIVATED || event == Event.EXPIRED
				|| event == Event.UNREGISTERED || event == Event.REJECTED)
		{
			for (int i = 0; i < registration.getContactArray().length; i++)
			{
				if (registration.getContactArray(i) == contactModified)
				{
					registration.removeContact(i);
					break;
				}
			}
		}
	}

	public void allBindingsRemoved(String aor)
	{
		Registration registration = _content.getReginfo().getRegistrationArray(0);
		registration.setState(org.cipango.kaleo.location.event.RegistrationDocument.Registration.State.TERMINATED);
		for (Contact contact : registration.getContactArray())
		{
			contact.setEvent(Event.UNREGISTERED);
		}
		fireStateChanged();
		 _content.getReginfo().setVersion(_content.getReginfo().getVersion().add(BigInteger.ONE));
		while(registration.getContactArray().length != 0)
		{
			registration.removeContact(0);
		}
	}

	public State getNeutralState()
	{
		ReginfoDocument document = ReginfoDocument.Factory.newInstance();
		Reginfo reginfo = document.addNewReginfo();
		reginfo.setVersion(BigInteger.ZERO);
		reginfo.setState(org.cipango.kaleo.location.event.ReginfoDocument.Reginfo.State.FULL);
		Registration reg = reginfo.addNewRegistration();
		reg.setAor(getUri());
		reg.setId("123");
		return new State(RegEventPackage.REGINFO, document);
	}
}
