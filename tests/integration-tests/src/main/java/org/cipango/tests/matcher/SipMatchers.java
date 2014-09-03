package org.cipango.tests.matcher;

import java.io.IOException;

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletResponse;

import org.cipango.client.test.matcher.HasHeader;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;

/**
 * 
 * Shoud use {@link org.cipango.client.test.matcher.SipMatchers}
 */
@Deprecated
public class SipMatchers
{
	public static void describeErrorFromResponse(SipServletResponse response,
			Description mismatchDescription)
	{
		mismatchDescription.appendText("test case failed on "
				+ response.getStatus() + " " + response.getReasonPhrase() + "\n");
		try
		{
			if (response.getContentLength() > 0)
				mismatchDescription.appendText("Server replied:\n" + new String(response.getRawContent()));
		}
		catch (IOException e)
		{
			mismatchDescription.appendText("\nAlso, could not read response content.");
		}
	}
	
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
