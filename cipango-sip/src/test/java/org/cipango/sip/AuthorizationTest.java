// ========================================================================
// Copyright 2007-2012 NEXCOM Systems
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AuthorizationTest
{
	static String rfc2617 = "Digest username=\"Mufasa\"," + "realm=\"testrealm@host.com\","
			+ "nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\"," + "uri=\"/dir/index.html\","
			+ "qop=auth," + "nc=00000001," + "cnonce=\"0a4f113b\","
			+ "response=\"6629fae49393a05397450978507c4ef1\","
			+ "opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"";

	@Test
	public void testDigestAuthenticator()
	{
		Authorization ah = new Authorization(rfc2617);
		String res = new DigestAuthenticator().calculateResponse(ah, "Circle Of Life", "GET");
		assertEquals("6629fae49393a05397450978507c4ef1", res);
	}

	@Test
	public void testGetParameters()
	{
		Authorization ah = new Authorization(rfc2617);
		assertEquals("6629fae49393a05397450978507c4ef1", ah.getResponse());
		assertEquals("Mufasa", ah.getUsername());
		assertEquals("testrealm@host.com", ah.getRealm());
		assertEquals("dcd98b7102dd2f0e8b11d0f600bfb0c093", ah.getNonce());
		assertEquals("/dir/index.html", ah.getUri());
		assertEquals("auth", ah.getQop());
		assertEquals("00000001", ah.getNonceCount());
		assertEquals("0a4f113b", ah.getCNonce());
		assertEquals("5ccc069c403ebaf9f0171e9517f40e41", ah.getOpaque());
		
	}

	@Test
	public void testToString()
	{
		Authorization ah = new Authorization(rfc2617);
		ah = new Authorization(ah.toString());
		assertEquals("6629fae49393a05397450978507c4ef1", ah.getResponse());
		assertEquals("Mufasa", ah.getUsername());
		assertEquals("testrealm@host.com", ah.getRealm());
		assertEquals("dcd98b7102dd2f0e8b11d0f600bfb0c093", ah.getNonce());
		assertEquals("/dir/index.html", ah.getUri());
		assertEquals("auth", ah.getQop());
		assertEquals("00000001", ah.getNonceCount());
		assertEquals("0a4f113b", ah.getCNonce());
		assertEquals("5ccc069c403ebaf9f0171e9517f40e41", ah.getOpaque());
		
		String s = ah.toString();
		assertTrue(s.contains("nc=00000001"));
		assertTrue(s.contains("response=\"6629fae49393a05397450978507c4ef1\""));
		//System.out.println(ah);
		
	}

	@Test
	public void testGenerateResponse()
	{
		Authenticate authenticate = new Authenticate(AuthenticateTest.RFC2617);
		Authorization ah = new Authorization(authenticate, "Mufasa", "Circle Of Life", "/dir/index.html", "GET");
		ah.setParameter(Authorization.Param.CNONCE.toString(), "0a4f113b");
		assertEquals("6629fae49393a05397450978507c4ef1", ah.getCalculatedResponse("Circle Of Life", "GET"));
	}
}
