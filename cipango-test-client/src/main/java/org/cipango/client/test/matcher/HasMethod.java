// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================
package org.cipango.client.test.matcher;

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
