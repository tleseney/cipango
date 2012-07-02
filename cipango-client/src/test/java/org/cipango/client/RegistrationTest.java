// ========================================================================
// Copyright 2011 NEXCOM Systems
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

package org.cipango.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.text.ParseException;

import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipURI;

import org.cipango.sip.SipHeader;
import org.jmock.Mockery;
import org.junit.Test;

public class RegistrationTest 
{
	SipClient _client;
    Mockery _context = new Mockery();

	@Test
	public void testConstructor() throws ParseException
	{
		final SipURI uri = _context.mock(SipURI.class);
		Registration r = new Registration(uri);
		assertThat(r.getFactory(), is(nullValue()));
		assertThat(r.getCredentials(), is(nullValue()));
		assertThat(r.getURI(), is(sameInstance(uri)));
	}
	
	@Test
	public void testConstructorWithNoURI()
	{
		Registration r = new Registration(null);
		assertThat(r.getFactory(), is(nullValue()));
		assertThat(r.getCredentials(), is(nullValue()));
		assertThat(r.getURI(), is(nullValue()));
	}
	
	@Test
	public void testSetFactory() throws ParseException
	{
		final SipFactory factory = _context.mock(SipFactory.class);
		final SipURI uri = _context.mock(SipURI.class);
		Registration r = new Registration(uri);
		r.setFactory(factory);
		assertThat(r.getFactory(), is(sameInstance(factory)));
	}

	@Test
	public void testSetCredentials() throws ParseException
	{
		final Credentials credentials = _context.mock(Credentials.class);
		final SipURI uri = _context.mock(SipURI.class);
		Registration r = new Registration(uri);
		r.setCredentials(credentials);
		assertThat(r.getCredentials(), is(sameInstance(credentials)));
	}

	@Test
	public void testCreateRegister() throws ServletParseException
	{
//		SipServletRequest request = r.createRegister(contact, 3600);
//		
//		assertThat((SipURI) generatedContactAddr.getURI(), equalTo(contact));
//		assertThat(generatedContactAddr.getExpires(), is(3600));
	}

	@Test(expected=NullPointerException.class) 
	public void testCreateRegisterWithNoFactory()
	{
		final SipURI uri = _context.mock(SipURI.class, "uri");
		final SipURI contact = _context.mock(SipURI.class, "contact");
		Registration r = new Registration(uri);
		SipServletRequest request = r.createRegister(contact, 3600);
	}
	
	//  The following is to be moved in separate project.
	
//	@Before
//	public void start() throws Exception
//	{
//		_client = new SipClient("127.0.0.1", 5060);
//		_client.start();
//	}
//	
//	@After
//	public void stop() throws Exception
//	{
//		_client.stop();
//	}
//	
//	@Test
//	public void testRegistration() throws Exception
//	{
//		Registration registration = new Registration(new SipURIImpl("sip:alice@127.0.0.1"));
//		
//		SipServletRequest register = registration.createRegister(_client.getContact(), 3600);
//		register.addHeader("X-biduke", "");
//		
//		SipServletResponse response = _client.waitforResponse(register);
//		assertThat(response, isSuccess());
//		
//	}
}
