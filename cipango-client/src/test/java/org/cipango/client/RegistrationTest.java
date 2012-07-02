package org.cipango.client;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.sip.SipURIImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RegistrationTest 
{
	SipClient _client;
	
	@Before
	public void start() throws Exception
	{
		_client = new SipClient("127.0.0.1", 5060);
		_client.start();
	}
	
	@After
	public void stop() throws Exception
	{
		_client.stop();
	}
	
	@Test
	public void testRegistration() throws Exception
	{
		Registration registration = new Registration(new SipURIImpl("sip:alice@127.0.0.1"));
		
		SipServletRequest register = registration.createRegister(_client.getContact(), 3600);
		register.addHeader("X-biduke", "");
		
		SipServletResponse response = _client.waitforResponse(register);
		assertThat(response, isSuccess());
		
	}
}
