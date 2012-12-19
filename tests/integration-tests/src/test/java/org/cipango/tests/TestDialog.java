// ========================================================================
// Copyright 2012 NEXCOM Systems
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
package org.cipango.tests;
import static org.junit.Assert.*;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.client.Dialog;
import org.junit.Ignore;

/**
 * Extends Dialog class to generate errors on waitForXxx methods if no message has been received.
 *
 */
@Ignore
public class TestDialog extends Dialog
{
	public TestDialog(Dialog dialog)
	{
		setFactory(dialog.getFactory());
		setCredentials(dialog.getCredentials()); 
		setOutboundProxy(dialog.getOutboundProxy());
		setTimeout(dialog.getTimeout());
	}

	@Override
	public SipServletRequest waitForRequest()
	{
		SipServletRequest request = super.waitForRequest();
		if (request == null)
			fail("Could not get request");
		return request;
	}

	@Override
	public SipServletResponse waitForResponse()
	{
		SipServletResponse response = super.waitForResponse();
		if (response == null)
			fail("Could not get response");
		return response;
	}

	@Override
	public SipServletResponse waitForFinalResponse()
	{
		SipServletResponse response = super.waitForFinalResponse();
		if (response == null)
			fail("Could not get final response");
		return response;
	}

//	@Override
//	public SipServletRequest createRequest(String method)
//	{
//		return TestAgent.decorate(super.createRequest(method));
//	}
}