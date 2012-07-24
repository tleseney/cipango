package org.cipango.sipunit.test.matcher;

import javax.servlet.sip.SipServletResponse;

import org.hamcrest.Description;

public class IsSuccess extends org.cipango.client.matcher.IsSuccess 
{
	@Override
	protected void describeMismatchSafely(SipServletResponse item, Description mismatchDescription)
	{
		SipMatchers.describeErrorFromResponse(item, mismatchDescription);
	}
}
