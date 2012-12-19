// ========================================================================
// Copyright 2007-2008 NEXCOM Systems
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
package org.cipango.tests.integration;

import java.net.URL;
import java.util.Iterator;

import javax.servlet.http.HttpSession;
import javax.servlet.sip.ConvergedHttpSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.annotation.SipServlet;

import org.cipango.tests.AbstractServlet;

@SuppressWarnings("serial")
@SipServlet (name="org.cipango.tests.integration.SipApplicationSessionTest")
public class SipApplicationSessionServlet extends AbstractServlet
{
	
	public static final String ENCODED_URL_HEADER = "EncodedUrl";
	
	public void testSetUri(SipServletRequest request) throws Exception
	{
		if ("ACK".equals(request.getMethod()))
			return;
		
		SipServletResponse response = request.createResponse(SipServletResponse.SC_OK);
		if ("INVITE".equals(request.getMethod()))
		{
			Iterator<String> it = request.getHeaders(ENCODED_URL_HEADER);
			while (it.hasNext())
			{
				String encodedUrl = it.next();
				URL url = request.getApplicationSession().encodeURL(new URL(encodedUrl));
				response.addHeader(ENCODED_URL_HEADER, url.toString());
			}
			
		}
		response.send();
	}

	public void testSetUriWithParam(SipServletRequest request) throws Exception
	{
		testSetUri(request);
	}

	public void testSetUri(SipServletResponse response) throws Exception
	{
		@SuppressWarnings("unchecked")
		Iterator<HttpSession> it = (Iterator<HttpSession>) response.getApplicationSession().getSessions("HTTP");
		assertTrue(it.hasNext());
		HttpSession session = it.next();
		assertTrue(session instanceof ConvergedHttpSession);
		assertFalse(it.hasNext());
	}

	public void testSetUriWithParam(SipServletResponse response) throws Exception
	{
		testSetUri(response);
	}
	
	public void testSetUriLate(SipServletRequest request) throws Exception
	{
		testSetUri(request);
	}
	
	public void testSetUriLate(SipServletResponse response) throws Exception
	{
		testSetUri(response);
	}
}
