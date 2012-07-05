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

import static org.cipango.client.Constants.ALICE_URI;
import static org.cipango.client.Constants.EXAMPLE_URI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.text.ParseException;

import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;

import org.cipango.sip.AddressImpl;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class UserAgentTest 
{
	Mockery _context = new JUnit4Mockery();
	Address _aor;
	SipFactory _factory;
    
	@Before
	public void setup()
	{
		_aor = new AddressImpl(ALICE_URI);
		_factory = _context.mock(SipFactory.class);
	}
	
	@Test
	public void testConstructor() throws ParseException
	{
		UserAgent ua = new UserAgent(_aor);
		assertThat(ua.getFactory(), is(nullValue()));
		assertThat(ua.getContact(), is(nullValue()));
		assertThat(ua.getAor(), is(sameInstance(_aor)));
	}
	
	@Test
	public void testConstructorWithNoURI()
	{
		UserAgent ua = new UserAgent(null);
		assertThat(ua.getFactory(), is(nullValue()));
		assertThat(ua.getContact(), is(nullValue()));
		assertThat(ua.getAor(), is(nullValue()));
	}
	
	@Test
	public void testSetFactory() throws ParseException
	{
		UserAgent ua = new UserAgent(_aor);
		ua.setFactory(_factory);
		assertThat(ua.getFactory(), is(sameInstance(_factory)));
	}
	
	@Test
	public void testSetContact() throws ParseException
	{
		final Address contact = _context.mock(Address.class);
		UserAgent ua = new UserAgent(_aor);
		ua.setContact(contact);
		assertThat(ua.getContact(), is(sameInstance(contact)));
	}

	@Test
	public void testCreateRequest() throws ServletParseException
	{
		final SipApplicationSession appSession = _context.mock(SipApplicationSession.class);
		final SipServletRequest request = _context.mock(SipServletRequest.class);
		final Address addr = _context.mock(Address.class);
		
		_context.checking(new Expectations() {{
	        oneOf(_factory).createApplicationSession(); will(returnValue(appSession));
	        oneOf(_factory).createAddress(EXAMPLE_URI.toString()); will(returnValue(addr));
			// TODO: The following is not famous, but matching addresses is not possible for now...
	        oneOf(_factory).createRequest(with(appSession), with(SipMethods.OPTIONS), with(any(Address.class)), with(any(Address.class))); will(returnValue(request));
	    }});
		
		UserAgent ua = new UserAgent(_aor);
		ua.setFactory(_factory);
		SipServletRequest req = ua.createRequest(SipMethods.OPTIONS, EXAMPLE_URI.toString());

		assertThat(req, is(sameInstance(request)));
	}
	
	
	// To be moved in another project or test.
	
//	SipClient _client;
	
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
//	public void testRegistration()
//	{
//		UserAgent alice = _client.createUserAgent();
//		alice.register(3600);
//	}
//
//	@Test
//	public void testSendRequest() throws IOException, ParseException
//	{
//		UserAgent alice = _client.createUserAgent();
//		alice.sendRequest(SipMethod.OPTIONS.asString(), new SipURIImpl("sip:bob@127.0.0.1:5070"), null);
//	}
//
//	@Test
//	public void testSendRequestWithListener() throws IOException, ParseException
//	{
//		UserAgent alice = _client.createUserAgent();
//
//		// TODO
//	}
//
//	@Test
//	public void testWaitForResponse() throws IOException, InterruptedException, ParseException
//	{
//		UserAgent alice = _client.createUserAgent();
//		SipServletResponse r = null;
//		
//		r = alice.waitForResponse(SipMethod.OPTIONS.asString(), new SipURIImpl("sip:bob@127.0.0.1:5070"));
//		assert r != null;
//	}
//
//	@Test
//	public void testWaitForResponseWithCode() throws IOException, InterruptedException, ParseException
//	{
//		UserAgent alice = _client.createUserAgent();
//		SipServletResponse r = null;
//		
//		r = alice.waitForResponse(SipMethod.OPTIONS.asString(), new SipURIImpl("sip:bob@127.0.0.1:5070"), SipStatus.OK.getCode());
//		assert r != null;
//		assert r.getStatus() == SipStatus.OK.getCode();
//	}
}
