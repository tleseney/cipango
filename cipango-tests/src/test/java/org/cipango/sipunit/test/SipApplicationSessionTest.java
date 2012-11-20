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
package org.cipango.sipunit.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.cipango.sipunit.test.matcher.SipMatchers.*;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.client.Call;
import org.cipango.client.SipMethods;
import org.cipango.sipunit.UaTestCase;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;

public class SipApplicationSessionTest extends UaTestCase
{
	public static final String ENCODED_URL_HEADER = "EncodedUrl";

	@Test
	public void testSetUri() throws Throwable 
	{
		String baseUri = getHttpBaseUrl() + "/SipApplicationSession";
		testSetUri(baseUri);
		testSetUri(baseUri + "?param=1");
	}

	@Test
	public void testSetUriWithParam() throws Throwable 
	{
		String baseUri = getHttpBaseUrl() + "/SipApplicationSession";
		testSetUri(baseUri);
		testSetUri(baseUri + "?param=1");
	}
	
	/**
	 * <pre>
	 * Alice                         AS
	 *   | INVITE urlToEncode         |
	 *   |--------------------------->|
	 *   |             200 urlEncoded |
	 *   |<---------------------------|
	 *   | ACK                        |
	 *   |--------------------------->|
	 *   | GET urlEncoded             |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
	 *   |                       INFO |
	 *   |<---------------------------|
	 *   | 200                        |
	 *   |--------------------------->|
	 *   | GET url with cookie        |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
	 *   |                        BYE |
	 *   |<---------------------------|
	 *   | 200                        |
	 *   |--------------------------->|
	 * </pre>
	 */
	public void testSetUri(String urlToEncode) throws Throwable 
	{
		Call call = (Call) _ua.customize(new Call());
		
		SipServletRequest request = call.createInitialInvite(
				_ua.getFactory().createURI(getFrom()),
				_ua.getFactory().createURI(getTo()));
		request.addHeader(ENCODED_URL_HEADER, urlToEncode);
		call.start(request);

		SipServletResponse response = call.waitForResponse(); 
        assertThat(response, isSuccess());
		call.createAck().send();

        String encodedUrl = response.getHeader(ENCODED_URL_HEADER).toString();

        HttpClient client = new HttpClient();
		client.start();
		 
		ContentExchange exchange = new ContentExchange(true);
		exchange.setURL(encodedUrl);	 
		client.send(exchange);
		assertThat(exchange.waitForDone(), is(HttpExchange.STATUS_COMPLETED));
		assertThat(exchange.getResponseStatus(), is(HttpStatus.OK_200));

		request = call.waitForRequest();
		assertThat(request.getMethod(), is(equalTo(SipMethods.INFO)));
		_ua.createResponse(request, SipServletResponse.SC_OK).send();
		
		String cookie = getCookie(exchange);
		exchange = new ContentExchange(true);

		exchange.setURL(urlToEncode);
		exchange.addRequestHeader(HttpHeaders.COOKIE, cookie);
		client.send(exchange);
		assertThat(exchange.waitForDone(), is(HttpExchange.STATUS_COMPLETED));
		assertThat(exchange.getResponseStatus(), is(HttpStatus.OK_200));
		
		request = call.waitForRequest();
		assertThat(request.getMethod(), is(equalTo(SipMethods.BYE)));
		_ua.createResponse(request, SipServletResponse.SC_OK).send();
		
		checkForFailure();
	}
	
	private String getCookie(ContentExchange exchange)
	{
		String cookie = exchange.getResponseFields().getStringField(HttpHeaders.SET_COOKIE);
		String[] s = cookie.split(";");
		for (String string : s)
		{
			if (string.toUpperCase().startsWith("JSESSIONID"))
				return string;
		}
		return null;
	}

}
