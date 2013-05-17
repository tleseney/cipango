package org.cipango.kaleo.location.event;

import java.io.ByteArrayInputStream;

import org.cipango.kaleo.location.RegistrationListener;
import org.cipango.kaleo.location.event.ContactDocument.Contact;
import org.cipango.kaleo.location.event.RegistrationDocument.Registration;


import junit.framework.TestCase;

public class ReginfoTest extends TestCase
{
	public void testParseReginfo() throws Exception
	{
		ReginfoDocument doc = ReginfoDocument.Factory.parse(getClass().getResourceAsStream("/reginfo.xml"));
		
		assertEquals("sip:user@example.com", doc.getReginfo().getRegistrationArray(0).getAor());
	}
	
	public void testGenerate() throws Exception
	{
		ReginfoHandler handler = new ReginfoHandler();
		ReginfoDocument doc = ReginfoDocument.Factory.newInstance();
		
		Registration registration = doc.addNewReginfo().addNewRegistration();
		registration.setAor("sip:test@cipango.org");
		registration.setState(Registration.State.ACTIVE);
		registration.setId("lkd4");
		
		Contact contact = registration.addNewContact();
		contact.setId("hdg0");
		contact.setState(Contact.State.ACTIVE);
		contact.setUri("sip:127.0.0.1");
		
		byte[] b = handler.getBytes(doc);
		
		ReginfoDocument doc2 = ReginfoDocument.Factory.parse(new ByteArrayInputStream(b));
		
		assertEquals("sip:127.0.0.1", doc2.getReginfo().getRegistrationArray(0).getContactArray(0).getUri());
	}

}

