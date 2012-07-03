package org.cipango.client.matcher;

import javax.servlet.sip.SipServletResponse;

import org.cipango.sip.SipStatus;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class IsSuccess extends TypeSafeMatcher<SipServletResponse> 
{

	@Override
	public void describeTo(Description description)
	{
		description.appendText(" is a success response");
	}

	@Override
	public boolean matchesSafely(SipServletResponse response)
	{
		return SipStatus.isSuccess(response.getStatus());
	}

	@Override
	protected void describeMismatchSafely(SipServletResponse item, Description mismatchDescription)
	{
		mismatchDescription.appendText(" got status " + item.getStatus());
	}

}
