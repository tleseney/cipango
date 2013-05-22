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
package org.cipango.client.test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.sip.SipServletMessage;
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
	private final Map<String, String> _extraHeaders;
	
	public TestDialog(Dialog dialog, Map<String, String> extraHeaders)
	{
		setFactory(dialog.getFactory());
		setCredentials(dialog.getCredentials()); 
		setOutboundProxy(dialog.getOutboundProxy());
		setTimeout(dialog.getTimeout());
		_extraHeaders = (extraHeaders == null) ? new HashMap<String, String>() : extraHeaders;
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

	public <T extends SipServletMessage> T decorate(T message)
	{
		if (message == null)
			return null;
				
		for (Map.Entry<String, String> entry : _extraHeaders.entrySet())
			message.addHeader(entry.getKey(), entry.getValue());
		return message;
	}
	
	@Override
	public SipServletRequest createRequest(String method)
	{
		return decorate(super.createRequest(method));
	}
}
