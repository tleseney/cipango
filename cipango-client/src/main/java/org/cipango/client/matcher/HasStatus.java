package org.cipango.client.matcher;

import javax.servlet.sip.SipServletResponse;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class HasStatus extends TypeSafeMatcher<SipServletResponse> 
{
	private int _status;
	
	public HasStatus(int status)
	{
		_status = status;
	}

	@Override
	public void describeTo(Description description)
	{
		description.appendText(" has status " + _status);
	}

	@Override
	public boolean matchesSafely(SipServletResponse response)
	{
		return response.getStatus() == _status;
	}

	@Override
	protected void describeMismatchSafely(SipServletResponse item, Description mismatchDescription)
	{
		mismatchDescription.appendText(" got " + item.getStatus());
	}

}
