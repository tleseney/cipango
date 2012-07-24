package org.cipango.sipunit.test.matcher;

import javax.servlet.sip.SipServletResponse;

import org.hamcrest.Description;

public class HasStatus extends org.cipango.client.matcher.HasStatus 
{
	public HasStatus(int status)
	{
		super(status);
	}

	@Override
	protected void describeMismatchSafely(SipServletResponse item, Description mismatchDescription)
	{
		SipMatchers.describeErrorFromResponse(item, mismatchDescription);
	}
}
