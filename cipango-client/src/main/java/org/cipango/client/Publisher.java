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
package org.cipango.client;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

public class Publisher extends Dialog
{
	private String _etag;
	private Address _aor;
	
	public Publisher(Address aor)
	{
		_aor = aor;
	}
	
	public SipServletRequest newUnPublish() throws IOException
	{
		return newPublish((byte[]) null, 0);
	}
	
	public SipServletRequest newPublish(InputStream body, int expires) throws IOException
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		int read;
		byte[] b = new byte[512];
		while ((read = body.read(b)) != -1)
		{
			os.write(b, 0, read);
		}
		return newPublish(os.toByteArray(), expires);
	}
	
	public SipServletRequest newPublish(byte[] body, int expires)
	{
		SipServletRequest request;
		if (_session == null)
			request = createInitialRequest(SipMethods.PUBLISH, _aor, _aor);
		else
			request = createRequest(SipMethods.PUBLISH);
		
		request.setHeader(SipHeaders.EVENT, "presence");
		request.setExpires(expires);
		if (_etag != null)
			request.setHeader(SipHeaders.SIP_IF_MATCH, _etag);
		
		if (body != null)
			try
			{
				request.setContent(body, "application/pidf+xml");
			}
			catch (UnsupportedEncodingException ignore) {}
		return request;
	}
	
	@Override
	public SipServletResponse waitForResponse()
	{
		SipServletResponse response = super.waitForResponse();
		if (response != null)
		{
			String etag = response.getHeader(SipHeaders.SIP_ETAG);
			if (etag != null)
				_etag = etag;
		}
		return response;
	}
	
	public SipServletResponse waitForFinalResponse()
	{
		SipServletResponse response = super.waitForFinalResponse();
		if (response != null)
		{
			String etag = response.getHeader(SipHeaders.SIP_ETAG);
			if (etag != null)
				_etag = etag;
		}
		return response;
	}

	public String getEtag()
	{
		return _etag;
	}

	@Override
	public void start(SipServletRequest request) throws IOException, ServletException
	{
		super.start(request);
		_session.setInvalidateWhenReady(false);
	}
}
