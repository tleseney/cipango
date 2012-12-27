// ========================================================================
// Copyright 2008-2012 NEXCOM Systems
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
package org.cipango.sip;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.jetty.util.TypeUtil;

public class DigestAuthenticator
{
	private MessageDigest md;
	public static final String AUTH = "auth";

	public DigestAuthenticator()
	{
		try
		{
			md = MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new RuntimeException(e);
		}
	}

	public String calculateResponse(Authorization authorization,
			String password, String method)
	{
		String user = authorization.getUsername();
		String realm = authorization.getRealm();
		String uri = authorization.getUri();
		String qop = authorization.getQop();
		String nc = authorization.getNonceCount();
		String nonce = authorization.getNonce();
		String cnonce = authorization.getCNonce();

		String a1 = user + ":" + realm + ":" + password;
		String a2 = method + ":" + uri;

		if (qop != null)
		{
			if (!qop.equals(AUTH)) { throw new IllegalArgumentException(
					"Invalid qop: " + qop); }
			if (nc == null || cnonce == null) { throw new IllegalArgumentException(
					"Invalid Authorization header: " + nc + "/" + cnonce); }
			return KD(H(a1), nonce + ":" + nc + ":" + cnonce + ":" + qop + ":"
					+ H(a2));

		}
		else
		{
			return KD(H(a1), nonce + ":" + H(a2));
		}
	}

	private String KD(String secret, String data)
	{
		return H(secret + ":" + data);
	}

	private synchronized String H(String s)
	{
		md.reset();
		try
		{
			md.update(s.getBytes("ISO-8859-1"));
			return TypeUtil.toString(md.digest(), 16);
		}
		catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException(e);
		}
	}

}
