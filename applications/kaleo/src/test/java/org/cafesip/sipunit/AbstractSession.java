// ========================================================================
// Copyright 2008-2009 NEXCOM Systems
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
package org.cafesip.sipunit;

import static junit.framework.Assert.fail;

import java.util.EventObject;
import java.util.List;

import javax.sip.ResponseEvent;
import javax.sip.TimeoutEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.URI;
import javax.sip.header.HeaderFactory;
import javax.sip.header.SIPETagHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

public class AbstractSession
{

	protected SipPhone _sipPhone;
	private static int __timeout = 5000;

	public AbstractSession(SipPhone phone)
	{
		_sipPhone = phone;
	}

	public AddressFactory getAddressFactory()
	{
		return _sipPhone.getParent().getAddressFactory();
	}

	public HeaderFactory getHeaderFactory()
	{
		return _sipPhone.getParent().getHeaderFactory();
	}

	public MessageFactory getMessageFactory()
	{
		return _sipPhone.getParent().getMessageFactory();
	}

	public Request newRequest(String method, long cseq, String to)
	{
		try
		{
			AddressFactory addressFactory = getAddressFactory();

			List<ViaHeader> viaHeaders = _sipPhone.getViaHeaders();
			URI requestUri = addressFactory.createURI(to);
			Address toAddr = addressFactory.createAddress(requestUri);

			Request request = getMessageFactory().createRequest(
					requestUri,
					method,
					_sipPhone.getParent().getSipProvider().getNewCallId(),
					getHeaderFactory().createCSeqHeader(cseq, method),
					getHeaderFactory().createFromHeader(_sipPhone.getAddress(),
							_sipPhone.generateNewTag()),
					getHeaderFactory().createToHeader(toAddr, null),
					viaHeaders, getHeaderFactory().createMaxForwardsHeader(70));

			return request;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public Response waitResponse(SipTransaction trans)
	{
		EventObject event = _sipPhone.waitResponse(trans, getTimeout());
		if (event == null)
			fail("No response received");

		if (event instanceof TimeoutEvent)
			fail("Received timeout");

		return ((ResponseEvent) event).getResponse();
	}

	public Response sendRequest(Request request, int expectedResponseCode)
	{
		return sendRequest(request, null, null, expectedResponseCode);
	}
	
	public Response sendRequest(Request request, String user, String password,
			int expectedResponseCode)
	{
		SipTransaction trans = _sipPhone.sendRequestWithTransaction(request,
				true, null);

		Response response = waitResponse(trans);
		int status_code = response.getStatusCode();

		while (status_code != expectedResponseCode)
		{
			if (status_code == Response.TRYING)
			{
				response = waitResponse(trans);
				status_code = response.getStatusCode();
			}
			else if ((status_code == Response.UNAUTHORIZED)
					|| (status_code == Response.PROXY_AUTHENTICATION_REQUIRED))
			{
				// modify the request to include user authorization info
				request = _sipPhone.processAuthChallenge(response, request,
						user, password);
				if (request == null)
					return null;

				// clean up last transaction
				_sipPhone.clearTransaction(trans);

				// send the request again
				trans = _sipPhone.sendRequestWithTransaction(request, true,
						null);

				response = waitResponse(trans);
				status_code = response.getStatusCode();
			}
			else
			{
				fail("The status code " + status_code + " "
						+ response.getReasonPhrase()
						+ " was received from the server when expected "
						+ expectedResponseCode);
			}
		}
		
		return response;
	}

	public int getTimeout()
	{
		return __timeout;
	}

}
