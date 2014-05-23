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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AuthenticateTest
{
	public static final String RFC2617 = "Digest "
                 + "realm=\"testrealm@host.com\", "
                 + "qop=\"auth,auth-int\", "
                 + "nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\", "
                 + "opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"";
	
	public static final String RFC2617_NOQUOTE = "Digest "
        + "realm=testrealm@host.com, "
        + "qop=auth, "
        + "nonce=dcd98b7102dd2f0e8b11d0f600bfb0c093, "
        + "opaque=5ccc069c403ebaf9f0171e9517f40e41," 
        + "stale=true";
	
	public static final String UNKNOW_PARAM = "Digest "
        + "realm=testrealm@host.com, "
        + "unknown=auth, "
        + "nonce=dcd98b7102dd2f0e8b11d0f600bfb0c093, "
        + "opaque=5ccc069c403ebaf9f0171e9517f40e41," 
        + "stale=true";



	@Test
	public void testParameters()
	{
		Authenticate authenticate = new Authenticate(RFC2617);
		assertEquals("testrealm@host.com", authenticate.getRealm());
		assertEquals("auth,auth-int", authenticate.getQop());
		assertEquals("dcd98b7102dd2f0e8b11d0f600bfb0c093", authenticate.getNonce());
		assertEquals("5ccc069c403ebaf9f0171e9517f40e41", authenticate.getOpaque());
		assertFalse(authenticate.isStale());
		assertEquals("Digest", authenticate.getScheme());
	}

	@Test
	public void testToString()
	{
		Authenticate authenticate = new Authenticate(RFC2617);
		authenticate = new Authenticate(authenticate.toString());
		assertEquals("testrealm@host.com", authenticate.getRealm());
		assertEquals("auth,auth-int", authenticate.getQop());
		assertEquals("dcd98b7102dd2f0e8b11d0f600bfb0c093", authenticate.getNonce());
		assertEquals("5ccc069c403ebaf9f0171e9517f40e41", authenticate.getOpaque());
		assertFalse(authenticate.isStale());
		assertEquals("Digest", authenticate.getScheme());
	}

	@Test
	public void testNoQuore()
	{
		Authenticate authenticate = new Authenticate(RFC2617_NOQUOTE);
		assertEquals("testrealm@host.com", authenticate.getRealm());
		assertEquals("auth", authenticate.getQop());
		assertEquals("dcd98b7102dd2f0e8b11d0f600bfb0c093", authenticate.getNonce());
		assertEquals("5ccc069c403ebaf9f0171e9517f40e41", authenticate.getOpaque());
		assertTrue(authenticate.isStale());
		assertEquals("Digest", authenticate.getScheme());	
	}

	@Test
	public void testUnknowmParam()
	{
		Authenticate authenticate = new Authenticate(UNKNOW_PARAM);
		assertEquals("auth", authenticate.getParameter("unknown"));
		assertEquals("testrealm@host.com", authenticate.getParameter("realm"));
		authenticate = new Authenticate(authenticate.toString());
		assertEquals("auth", authenticate.getParameter("unknown"));
	}
}
