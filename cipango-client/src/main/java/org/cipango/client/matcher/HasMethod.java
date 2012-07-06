package org.cipango.client.matcher;

import javax.servlet.sip.SipServletMessage;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class HasMethod extends TypeSafeMatcher<SipServletMessage> 
{
	private String _method;
	
	public HasMethod(String method)
	{
		_method = method;
	}

	@Override
	public void describeTo(Description description)
	{
		description.appendText(" has method " + _method);
	}

	@Override
	public boolean matchesSafely(SipServletMessage message)
	{
		return message != null && message.getMethod().equals(_method);
	}

	@Override
	protected void describeMismatchSafely(SipServletMessage item, Description mismatchDescription)
	{
		if (item == null)
			mismatchDescription.appendText(" is null");
		else
			mismatchDescription.appendText(" got " + item.getMethod());
	}

}
