// ========================================================================
// Copyright 2009 NEXCOM Systems
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.sip.header.HeaderFactory;
import javax.sip.header.SIPETagHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

public class PublishSession extends AbstractSession
{	
	

	private String _etag;
	
	public PublishSession(SipPhone phone)
	{
		super(phone);
	}

	public Request newPublish(InputStream body, int expires)
	{
		
		try
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
		catch (Exception e)
		{
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			throw new RuntimeException(e);
		}
	}
	
	public Request newPublish(byte[] body, int expires)
	{
		
		try
		{
			Request publish = newRequest(Request.PUBLISH, 1, _sipPhone.me);
			HeaderFactory hf = getHeaderFactory();
			publish.addHeader(hf.createEventHeader("presence"));
			publish.addHeader(hf.createExpiresHeader(expires));

			publish.setContent(body, hf.createContentTypeHeader("application", "pidf+xml"));
			return publish;
		}
		catch (Exception e)
		{
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			throw new RuntimeException(e);
		}
	}
	
	public Request newUnpublish()
	{
		try
		{
			Request publish = newRequest(Request.PUBLISH, 2, _sipPhone.me);
			HeaderFactory hf = getHeaderFactory();
			publish.addHeader(hf.createEventHeader("presence"));
			publish.addHeader(hf.createExpiresHeader(0));
			if (_etag == null)
				throw new IllegalStateException("No Etag set");
			publish.addHeader(hf.createSIPIfMatchHeader(_etag));
			
			return publish;
		}
		catch (Exception e)
		{
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			throw new RuntimeException(e);
		}
	}
	
	public Response sendRequest(Request request, String user, String password,
			int expectedResponseCode)
	{
		Response response = super.sendRequest(request, user, password, expectedResponseCode);

		if (response != null)
		{
			SIPETagHeader etag = (SIPETagHeader) response.getHeader(SIPETagHeader.NAME);
			if (etag != null)
				_etag = etag.getETag();
		}
		return response;
	}
}
