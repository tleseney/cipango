package org.cipango.client.matcher;

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletResponse;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;

public class SipMatchers
{
	
	@Factory
	public static <T> Matcher<SipServletResponse> isSuccess()
	{
		return new IsSuccess();
	}

	@Factory
	public static <T> Matcher<SipServletMessage> hasHeader(String name)
	{
		return new HasHeader(name);
	}
	
	@Factory
	public static <T> Matcher<SipServletResponse> hasStatus(int status)
	{
		return new HasStatus(status);
	}
}
