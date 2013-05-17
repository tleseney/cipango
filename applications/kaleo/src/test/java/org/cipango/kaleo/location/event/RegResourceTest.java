// ========================================================================
// Copyright 2009 NEXCOM Systems
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

import java.util.Iterator;

import javax.servlet.sip.URI;

import junit.framework.TestCase;

import org.cipango.kaleo.location.Binding;
import org.cipango.kaleo.location.Registration;
import org.cipango.kaleo.location.event.ContactDocument.Contact;
import org.cipango.kaleo.location.event.ContactDocument.Contact.Event;
import org.cipango.kaleo.location.event.ReginfoDocument.Reginfo;
import org.cipango.kaleo.location.event.RegistrationDocument.Registration.State;

public class RegResourceTest extends TestCase
{

	public void testGetState() throws Exception
	{
		long now = System.currentTimeMillis();
		String aor = "sip:alice@cipango.org";
		RegResource regResource = new RegResource(aor, null);
		//System.out.println(regResource.getState().getContent());
		Reginfo reginfo = ((ReginfoDocument) regResource.getState().getContent()).getReginfo();
		assertEquals(aor, reginfo.getRegistrationArray(0).getAor());
		assertEquals(State.INIT, reginfo.getRegistrationArray(0).getState());
		assertEquals(0, reginfo.getRegistrationArray(0).getContactArray().length);
		
		Registration registration = new Registration(aor);
		registration.addListener(regResource);	
		Binding binding1 = new Binding(new UriImpl("sip:alice@localhost"), "123@localhost", 1, now + 100000);
		registration.addBinding(binding1);
		
		//System.out.println(regResource.getState().getContent());
		assertEquals(1, reginfo.getRegistrationArray(0).getContactArray().length);
		assertEquals(State.ACTIVE, reginfo.getRegistrationArray(0).getState());
		Contact contact = reginfo.getRegistrationArray(0).getContactArray(0);
		assertEquals(Event.REGISTERED, contact.getEvent());
		assertEquals("sip:alice@localhost", contact.getUri());
		//assertTrue(contact.getExpires().intValue() > 98 && contact.getExpires().intValue() <= 100);
		
		registration.addBinding(new Binding(new UriImpl("sip:alice@localhost:5070"), "567@localhost", 1, now + 200000));
		assertEquals(2, reginfo.getRegistrationArray(0).getContactArray().length);
		assertEquals(State.ACTIVE, reginfo.getRegistrationArray(0).getState());
		assertEquals(Event.REGISTERED, reginfo.getRegistrationArray(0).getContactArray(1).getEvent());
		assertEquals("sip:alice@localhost", reginfo.getRegistrationArray(0).getContactArray(0).getUri());
		assertEquals("sip:alice@localhost:5070", reginfo.getRegistrationArray(0).getContactArray(1).getUri());
		
		registration.updateBinding(binding1, new UriImpl("sip:alice@newContact"), "123@localhost", 1, now + 200000);
		assertEquals(2, reginfo.getRegistrationArray(0).getContactArray().length);
		assertEquals(State.ACTIVE, reginfo.getRegistrationArray(0).getState());
		assertEquals(Event.REFRESHED, reginfo.getRegistrationArray(0).getContactArray(0).getEvent());
		assertEquals(contact.getId(), reginfo.getRegistrationArray(0).getContactArray(0).getId());
		assertEquals("sip:alice@newContact", reginfo.getRegistrationArray(0).getContactArray(0).getUri());
		
		registration.removeBinding(binding1);
		assertEquals(1, reginfo.getRegistrationArray(0).getContactArray().length);
		assertEquals(State.ACTIVE, reginfo.getRegistrationArray(0).getState());
		
		
		registration.addBinding(binding1);
		registration.removeAllBindings();
		assertEquals(0, reginfo.getRegistrationArray(0).getContactArray().length);
		assertEquals(State.TERMINATED, reginfo.getRegistrationArray(0).getState());
	}
	
	static class UriImpl implements URI
	{
		private String _uri;
		
		public UriImpl(String uri)
		{
			_uri = uri;
		}

		public String getParameter(String arg0)
		{
			return null;
		}

		public Iterator<String> getParameterNames()
		{
			return null;
		}

		public String getScheme()
		{
			return null;
		}

		public boolean isSipURI()
		{
			return true;
		}

		public void removeParameter(String arg0)
		{
		}

		public void setParameter(String arg0, String arg1)
		{
		}
		public URI clone()
		{
			return null;
		}
		public String toString()
		{
			return _uri;
		}
	}
	
	
}
