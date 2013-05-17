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
package org.cipango.kaleo.sipunit;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.sip.header.CSeqHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.MinExpiresHeader;
import javax.sip.header.UnsupportedHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.cafesip.sipunit.RegisterSession;


public class RegisterTest extends UaTestCase {

	
	@SuppressWarnings("unchecked")
	public void testSimpleRegister() throws Exception
	{
		RegisterSession session = new RegisterSession(getAlicePhone());
		Request request = session.createRegister(null, 1800);
		Response response = 
			session.sendRegistrationMessage(request, Response.OK);
		ListIterator<ContactHeader> contacts = response.getHeaders(ContactHeader.NAME);
		ContactHeader contact = contacts.next();
		assertBetween(1795, 1800, contact.getExpires());
		assertEquals(getAlicePhone().getContactInfo().getContactHeader().getAddress(), contact.getAddress());
		assertFalse(contacts.hasNext());
		

        request = session.createRegister(null, 0);
		response = 	session.sendRegistrationMessage(request, Response.OK);
		contacts = response.getHeaders(ContactHeader.NAME);
		assertFalse(contacts.hasNext());
	}
	
	protected ContactHeader newContact(String address) throws ParseException
	{
		return __headerFactory.createContactHeader(__addressFactory.createAddress(address));
	}
	
	@SuppressWarnings("unchecked")
	public void testMultipleContacts() throws Exception
	{
		RegisterSession session =  new RegisterSession(getAlicePhone());
		Request request = session.createRegister(null, 1800);
		ContactHeader contact2 = newContact("<sip:localhost>");
		contact2.setExpires(1500);
		request.addHeader(contact2);
		Response response = 
			session.sendRegistrationMessage(request, Response.OK);
		ListIterator<ContactHeader> contacts = response.getHeaders(ContactHeader.NAME);
		while (contacts.hasNext()) {
			ContactHeader contact = (ContactHeader) contacts.next();
			if (contact.getExpires() > 1795 && contact.getExpires() <= 1800)
				assertEquals(getAlicePhone().getContactInfo().getContactHeader().getAddress(), contact.getAddress());
			else
			{
				assertBetween(contact2.getExpires() -5, contact2.getExpires(), contact.getExpires());
				assertEquals(contact2.getAddress(), contact.getAddress());	
			}
		}
		
        request = session.createRegister(contact2.getAddress().toString(), 0);
		response = 	session.sendRegistrationMessage(request, Response.OK);
		contacts = response.getHeaders(ContactHeader.NAME);
		ContactHeader contact = contacts.next();
		assertBetween(1780, 1800, contact.getExpires());
		assertEquals(getAlicePhone().getContactInfo().getContactHeader().getAddress(), contact.getAddress());
		assertFalse(contacts.hasNext());
	}
	
	public void testWilcard() throws Exception
	{
		RegisterSession session =  new RegisterSession(getAlicePhone());
		Request request = session.createRegister(null, 1800);
		ContactHeader contact2 = newContact("<sip:localhost>");
		contact2.setExpires(1500);
		request.addHeader(contact2);
		Response response = 
			session.sendRegistrationMessage(request, Response.OK);
		
        request = session.createRegister("*", 0);
		response = 	session.sendRegistrationMessage(request, Response.OK);
		assertFalse(response.getHeaders(ContactHeader.NAME).hasNext());
	}
		
	public void testLowerCSeq() throws Exception {
		RegisterSession session =  new RegisterSession(getAlicePhone());
		session.register(1800);
		
		Request request = session.createRegister(null, 0);
		CSeqHeader cseq = (CSeqHeader) request.getHeader(CSeqHeader.NAME);
		cseq.setSeqNumber(cseq.getSeqNumber() - 2);
		Response response = session.sendRegistrationMessage(request, Response.SERVER_INTERNAL_ERROR);
		assertEquals("Lower CSeq", response.getReasonPhrase());
	}
	
	
	public void testInvalidWilcard() throws Exception {	
		RegisterSession session =  new RegisterSession(getAlicePhone());
		Request request = session.createRegister("*", 1800);
		Response response = 
			session.sendRegistrationMessage(request, Response.BAD_REQUEST);
		assertEquals("Invalid wildcard", response.getReasonPhrase());
	}
	
	public void testInvalidWilcard2() throws Exception {	
		RegisterSession session =  new RegisterSession(getAlicePhone());
		Request request = session.createRegister(null, 0);
		ContactHeader contact = __headerFactory.createContactHeader();
		contact.setWildCard();
		request.addHeader(contact);
		Response response = 
			session.sendRegistrationMessage(request, Response.BAD_REQUEST);
		assertEquals("Invalid wildcard", response.getReasonPhrase());
	}
	
	public void testMinExpires() throws Exception {	
		RegisterSession session =  new RegisterSession(getAlicePhone());
		Request request = session.createRegister(null, 2);
		Response response = session.sendRegistrationMessage(request, Response.INTERVAL_TOO_BRIEF);
		MinExpiresHeader minExpiresHeader = (MinExpiresHeader) response.getHeader(MinExpiresHeader.NAME);
        assertNotNull(minExpiresHeader);
	}
	

	public void testRequires() throws Exception {	
		RegisterSession session =  new RegisterSession(getAlicePhone());
		Request request = session.createRegister(null, 1800);
		request.addHeader(__headerFactory.createRequireHeader("ext1"));
		request.addHeader(__headerFactory.createRequireHeader("ext2"));
		Response response = 
			session.sendRegistrationMessage(request, Response.BAD_EXTENSION);

		assertHeaderContains(response, UnsupportedHeader.NAME, "ext1");
		assertHeaderContains(response, UnsupportedHeader.NAME, "ext2");

	}
	
	public void testNoContact() throws Exception {	
		RegisterSession session =  new RegisterSession(getAlicePhone());
		session.register(1800);
		List<String> expectedContacts = new ArrayList<String>();
		expectedContacts.add(getAlicePhone().getContactInfo().getURI());
		sendRegisterNoContact(session, expectedContacts);
	}
	
	@SuppressWarnings("unchecked")
	protected void sendRegisterNoContact(RegisterSession session, List<String> expectedContacts) throws Exception
	{
		Request request = session.createRegister(null, 1800);
		request.removeHeader(ContactHeader.NAME);
		request.removeHeader(ExpiresHeader.NAME);
		Response response = 
			session.sendRegistrationMessage(request, Response.OK);
		
		List<String> noParams = new ArrayList<String>();
		Iterator<String> it1 = expectedContacts.iterator();
		while (it1.hasNext()) {
			String uri = (String) it1.next();
			int index = uri.indexOf(';');
			if (index != -1)
				uri = uri.substring(0, index);
			noParams.add(uri);
		}
		
		Iterator<ContactHeader> it = response.getHeaders(ContactHeader.NAME);
		while (it.hasNext()) {
			String uri = it.next().getAddress().getURI().toString();
			int index = uri.indexOf(';');
			if (index != -1)
				uri = uri.substring(0, index);
			if (noParams.contains(uri.toString()))
				noParams.remove(uri.toString());
			else
				fail("Unexpected contact: " + uri + " found");
		}
		assertTrue("Expected contacts: " + expectedContacts + " not found", noParams.isEmpty());
	}

}
