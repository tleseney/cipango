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
package org.cipango.kaleo.integration;


import static org.cipango.client.test.matcher.SipMatchers.hasStatus;
import static org.cipango.client.test.matcher.SipMatchers.isSuccess;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.sip.Address;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;

import org.cipango.client.Registration;
import org.cipango.client.SipHeaders;
import org.cipango.client.UserAgent;


public class RegisterTest extends UaTestCase {

	private Registration _registration;
	
	@Override
	public void setUp() throws Exception
	{
		super.setUp();
		_registration = getRegistration(getAlice());
	}
	
	@Override
	public void tearDown() throws Exception
	{
		_registration.unregister(null);
		super.tearDown();
	}
	
	
	public Registration getRegistration(UserAgent ua)
	{
		Registration registration = new Registration((SipURI) ua.getAor().getURI());
		registration.setFactory(ua.getFactory());
		registration.setCredentials(ua.getCredentials());
		registration.setTimeout(ua.getTimeout());
		registration.setOutboundProxy(getAlice().getOutboundProxy());
		return registration;
	}
	
	public void testSimpleRegister() throws Exception
	{
		SipServletRequest register = _registration.createRegister(getAlice().getContact(), 1800);
		SipServletResponse response = getAlice().sendSynchronous(register);
		assertThat(response, isSuccess());
		ListIterator<Address> contacts = response.getAddressHeaders(SipHeaders.CONTACT);
		Address contact = contacts.next();
		assertBetween(1795, 1800, contact.getExpires());
		assertThat(contact.getURI(), is(getAlice().getContact().getURI()));
		assertFalse(contacts.hasNext());
		
		register = _registration.createRegister(null, 0);
		response = 	getAlice().sendSynchronous(register);
		assertThat(response, isSuccess());
		contacts = response.getAddressHeaders(SipHeaders.CONTACT);
		assertFalse(contacts.hasNext());
	}
	
	public void testMultipleContacts() throws Exception
	{
		SipServletRequest register = _registration.createRegister(getAlice().getContact(), 1800);
		Address contact2 = getAlice().getFactory().createAddress("<sip:localhost>");
		contact2.setExpires(1500);
		register.addAddressHeader(SipHeaders.CONTACT, contact2, false);
		SipServletResponse response = getAlice().sendSynchronous(register);
		assertThat(response, isSuccess());
		ListIterator<Address> contacts = response.getAddressHeaders(SipHeaders.CONTACT);
		while (contacts.hasNext()) {
			Address contact = contacts.next();
			if (contact.getExpires() > 1795 && contact.getExpires() <= 1800)
				assertThat(contact.getURI(), is(getAlice().getContact().getURI()));
			else
			{
				assertBetween(contact2.getExpires() -5, contact2.getExpires(), contact.getExpires());
				assertThat(contact.getURI(), is(contact2.getURI()));
			}
		}
		
		contact2.setExpires(-1);
        register = _registration.createRegister(contact2, 0);
        response = getAlice().sendSynchronous(register);
		assertThat(response, isSuccess());
		contacts = response.getAddressHeaders(SipHeaders.CONTACT);
		Address contact = contacts.next();
		assertBetween(1795, 1800, contact.getExpires());
		assertThat(contact.getURI(), is(getAlice().getContact().getURI()));
		assertFalse(contacts.hasNext());
	}
	
	public void testUnregister() throws Exception
	{
		SipServletRequest register = _registration.createRegister(getAlice().getContact(), 1800);
		SipServletResponse response = getAlice().sendSynchronous(register);
		assertThat(response, isSuccess());
		ListIterator<Address> contacts = response.getAddressHeaders(SipHeaders.CONTACT);
		Address contact = contacts.next();
		assertBetween(1795, 1800, contact.getExpires());
		assertThat(contact.getURI(), is(getAlice().getContact().getURI()));
		assertFalse(contacts.hasNext());
		
		register = _registration.createRegister(getAlice().getContact(), 0);
		response = 	getAlice().sendSynchronous(register);
		assertThat(response, isSuccess());
		contacts = response.getAddressHeaders(SipHeaders.CONTACT);
		assertFalse(contacts.hasNext());
	}
		
	public void testLowerCSeq() throws Exception {
		SipServletRequest register1 = _registration.createRegister(getAlice().getContact(), 1800);
		
		SipServletRequest register2 = _registration.createRegister(getAlice().getContact(), 1800);
		SipServletResponse response2 = getAlice().sendSynchronous(register2);
		assertThat(response2, isSuccess());
		
		SipServletResponse response1 = getAlice().sendSynchronous(register1);
		assertThat(response1, hasStatus(SipServletResponse.SC_SERVER_INTERNAL_ERROR));
		assertEquals("Lower CSeq", response1.getReasonPhrase());
	}
	
	
	public void testInvalidWilcard() throws Exception {	
		SipServletRequest register = _registration.createRegister(getAlice().getContact(), 1800);
		register.setHeader(SipHeaders.CONTACT, "*");
		SipServletResponse response = 	getAlice().sendSynchronous(register);
		assertThat(response, hasStatus(SipServletResponse.SC_BAD_REQUEST));
		assertEquals("Invalid wildcard", response.getReasonPhrase());
	}
	
	public void testInvalidWilcard2() throws Exception {	
		SipServletRequest register = _registration.createRegister(getAlice().getContact(), 1800);
		register.addHeader(SipHeaders.CONTACT, "*");
		SipServletResponse response = 	getAlice().sendSynchronous(register);
		assertThat(response, hasStatus(SipServletResponse.SC_BAD_REQUEST));
		assertEquals("Invalid wildcard", response.getReasonPhrase());
	}
	
	public void testMinExpires() throws Exception {	
		SipServletRequest register = _registration.createRegister(getAlice().getContact(), 2);
		SipServletResponse response = 	getAlice().sendSynchronous(register);
		assertThat(response, hasStatus(SipServletResponse.SC_INTERVAL_TOO_BRIEF));
		
		String minExpires = response.getHeader(SipHeaders.MIN_EXPIRES);
        assertNotNull(minExpires);
	}
	

	public void testRequires() throws Exception {	
		SipServletRequest register = _registration.createRegister(getAlice().getContact(), 1800);
		List<String> requires = Arrays.asList("ext1", "ext2");
		
		for (String s : requires)
			register.addHeader(SipHeaders.REQUIRE, s);
		
		SipServletResponse response = 	getAlice().sendSynchronous(register);
		assertThat(response, hasStatus(SipServletResponse.SC_BAD_EXTENSION));
		
		Iterator<String> it = response.getHeaders(SipHeaders.UNSUPPORTED);
		List<String> unsupported = new ArrayList<>();
		while (it.hasNext())
			unsupported.add(it.next());

		assertThat(requires, is(unsupported));

	}
	
	public void testNoContact() throws Exception {	
		SipServletRequest register = _registration.createRegister(getAlice().getContact(), 1800);
		SipServletResponse response = getAlice().sendSynchronous(register);
		assertThat(response, isSuccess());
		
		register = _registration.createRegister(null, 1800);
		register.removeHeader(SipHeaders.CONTACT);
		register.removeHeader(SipHeaders.EXPIRES);
		response = getAlice().sendSynchronous(register);
		assertThat(response, isSuccess());
		ListIterator<Address> contacts = response.getAddressHeaders(SipHeaders.CONTACT);
		Address contact = contacts.next();
		assertBetween(1795, 1800, contact.getExpires());
		assertThat(contact.getURI(), is(getAlice().getContact().getURI()));
		assertFalse(contacts.hasNext());

	}

}
