package org.cipango.client.matcher;

import javax.servlet.sip.SipServletMessage;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class HasHeader extends TypeSafeMatcher<SipServletMessage> 
{
	private String _name;
	
	public HasHeader(String name)
	{
		_name= name;
	}

	@Override
	public void describeTo(Description description)
	{
		description.appendText(" has header " + _name);
	}

	@Override
	public boolean matchesSafely(SipServletMessage message)
	{
		return message.getHeader(_name) != null;
	}

	@Override
	protected void describeMismatchSafely(SipServletMessage item, Description mismatchDescription)
	{
		mismatchDescription.appendText(" got no header " + _name);
	}

}
