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
