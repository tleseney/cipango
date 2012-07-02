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
import javax.servlet.sip.SipFactory;

import org.jmock.Mockery;
import org.junit.Test;

public class UserAgentTest 
{
	SipClient _client;
    Mockery _context = new Mockery();
    
	@Test
	public void testConstructor() throws ParseException
	{
		final SipProfile profile = _context.mock(SipProfile.class);
		UserAgent ua = new UserAgent(profile);
		assertThat(ua.getFactory(), is(nullValue()));
		assertThat(ua.getContact(), is(nullValue()));
		assertThat(ua.getProfile(), is(sameInstance(profile)));
	}
	
	@Test
	public void testConstructorWithNoURI()
	{
		UserAgent ua = new UserAgent(null);
		assertThat(ua.getFactory(), is(nullValue()));
		assertThat(ua.getContact(), is(nullValue()));
		assertThat(ua.getProfile(), is(nullValue()));
	}
	
	@Test
	public void testSetFactory() throws ParseException
	{
		final SipProfile profile = _context.mock(SipProfile.class);
		final SipFactory factory = _context.mock(SipFactory.class);
		UserAgent ua = new UserAgent(profile);
		ua.setFactory(factory);
		assertThat(ua.getFactory(), is(sameInstance(factory)));
	}
	
	@Test
	public void testSetCredentials() throws ParseException
	{
		final SipProfile profile = _context.mock(SipProfile.class);
		final Address contact = _context.mock(Address.class);
		UserAgent ua = new UserAgent(profile);
		ua.setContact(contact);
		assertThat(ua.getContact(), is(sameInstance(contact)));
	}
	
	
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
