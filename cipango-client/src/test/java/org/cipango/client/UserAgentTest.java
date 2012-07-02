package org.cipango.client;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UserAgentTest 
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
	public void testRegistration()
	{
		UserAgent alice = _client.createUserAgent();
		alice.register(3600);
		
	}
}
