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
		mismatchDescription.appendText(" got status " + item.getStatus() + " " + item.getReasonPhrase());
	}

}
