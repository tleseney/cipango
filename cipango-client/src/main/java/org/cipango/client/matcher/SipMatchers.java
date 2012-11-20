package org.cipango.client.matcher;

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletResponse;

import org.cipango.client.SipMethods;
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
	
	@Factory
	public static <T> Matcher<SipServletMessage> hasMethod(String method)
	{
		return new HasMethod(method);
	}
	
	@Factory
	public static <T> Matcher<SipServletMessage> isInvite()
	{
		return new HasMethod(SipMethods.INVITE);
	}
	
	@Factory
	public static <T> Matcher<SipServletMessage> isBye()
	{
		return new HasMethod(SipMethods.BYE);
	}
	
	@Factory
	public static <T> Matcher<SipServletMessage> isAck()
	{
		return new HasMethod(SipMethods.ACK);
	}
	
	@Factory
	public static <T> Matcher<SipServletMessage> isCancel()
	{
		return new HasMethod(SipMethods.CANCEL);
	}
}
