// ========================================================================
// Copyright 2011-2012 NEXCOM Systems
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
package org.cipango.server.security.authentication;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Iterator;

import javax.servlet.sip.SipServletResponse;

import org.cipango.server.SipRequest;
import org.cipango.server.security.SipAuthenticator;
import org.cipango.sip.Authenticate;
import org.cipango.sip.Authorization;
import org.cipango.sip.SipHeader;
import org.cipango.sip.labs.HexString;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.security.Constraint;

public class DigestAuthenticator implements SipAuthenticator
{

	private static final Logger LOG = Log.getLogger(DigestAuthenticator.class);
	
	private AuthConfiguration _authConfiguration;
	protected long nonceValidity = 5000;
	protected long nonceSecret = System.currentTimeMillis();
	

	
	public UserIdentity authenticate(SipRequest request, boolean proxyMode, boolean authMandatory)
	{
		try
		{
			Authorization authorization = getAuthorization(request, proxyMode);
			if (authorization == null)
			{
				LOG.debug("No authorization header found, send challenge");
				send401(request, proxyMode, false);
			}
			else if (!checkNonce(authorization.getNonce()))
			{
				LOG.debug("Request is stale, nonce is too old");
				send401(request, proxyMode, true);
			}
			else
			{
				authorization.setMethod(request.getMethod());
				UserIdentity user = _authConfiguration.getLoginService().login(authorization.getUsername(), authorization);
				if (user == null)
				{
					LOG.debug("Authentication failed or user unknown");
					SipServletResponse response = request.createResponse(SipServletResponse.SC_FORBIDDEN);
					response.send();
				}
				else
					LOG.debug("Authentication succeed for {}", user);
				return user;
			}
		}
		catch (Exception e)
		{
			LOG.warn("Failed to authenticate", e);
		}

		return null;
	}

	public void setAuthConfiguration(AuthConfiguration configuration)
	{
		if (configuration.getLoginService() == null)
			throw new IllegalStateException("No LoginService for " + this + " in " + configuration);
		_authConfiguration = configuration;
	}
	
	private void send401(SipRequest request, boolean proxyMode, boolean stale) throws IOException
	{
		SipServletResponse response;
		Authenticate authenticate = new Authenticate("Digest",
				_authConfiguration.getRealmName(), newNonce(), stale, "md5");
		if (proxyMode)
		{
			response = request.createResponse(SipServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
			response.setHeader(SipHeader.PROXY_AUTHENTICATE.asString(), authenticate.toString());
		}
		else
		{
			response = request.createResponse(SipServletResponse.SC_UNAUTHORIZED);
			response.setHeader(SipHeader.WWW_AUTHENTICATE.asString(), authenticate.toString());
		}
		response.send();
	}
	
	private Authorization getAuthorization(SipRequest request, boolean proxyMode)
	{
		Iterator<String> it = request.getFields().getValues(getAuthHeaderName(proxyMode).asString());
		while (it.hasNext())
		{
			Authorization authorization = (Authorization) new Authorization(it.next());
			if (_authConfiguration.getRealmName().equals(authorization.getRealm()))
				return authorization;
		}
		return null;
	}
	
	private SipHeader getAuthHeaderName(boolean proxyMode)
	{
		if (proxyMode)
			return SipHeader.PROXY_AUTHORIZATION;
		else
			return SipHeader.AUTHORIZATION;
	}
	
	public synchronized boolean checkNonce(String nonce) {
		byte[] b = HexString.fromHexString(nonce);
		if (b.length != 24) {
			return false;
		}
		long time = 0;
		long secret = nonceSecret;

		byte[] b2 = new byte[16];
		System.arraycopy(b, 0, b2, 0, 8);

		for (int i = 0; i < 8; i++) {
			b2[8 + i] = (byte) (secret & 0xff);
			secret = secret >> 8;
			time = (time << 8) + (b[7 - i] & 0xff);
		}
		
		byte[] hash = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(b2, 0, 16);
			hash = md.digest();
		} catch (Exception e) {
			LOG.warn("Unexpected error", e);
		}
		for (int i = 0; i < 16; i++) {
			if (b[i + 8] != hash[i]) {
				return false;
			}
		}
		
		if (System.currentTimeMillis() - time < nonceValidity) 
			return true;
		return false;
	}
	
	protected synchronized String newNonce() {
		long time = System.currentTimeMillis();
		long secret = nonceSecret;
		byte[] nonce = new byte[24];

		for (int i = 0; i < 8; i++) {
			nonce[i] = (byte) (time & 0xff);
			time = time >> 8;
			nonce[8 + i] = (byte) (secret & 0xff);
			secret = secret >> 8;
		}

		byte[] hash = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.reset();
			md.update(nonce, 0, 16);
			hash = md.digest();
		} catch (Exception e) {
			LOG.warn("Unexpected error", e);
		}

		for (int i = 0; i < hash.length; i++) {
			nonce[8 + i] = hash[i];
		}
		return HexString.toHexString(nonce);
	}

	public String getAuthMethod()
	{
		return Constraint.__DIGEST_AUTH;
	}



}
