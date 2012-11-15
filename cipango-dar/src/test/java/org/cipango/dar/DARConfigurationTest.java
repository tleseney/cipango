// ========================================================================
// Copyright 2007-2011 NEXCOM Systems
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

package org.cipango.dar;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import javax.servlet.sip.ar.SipApplicationRoutingRegion;

import org.junit.Test;

public class DARConfigurationTest
{
	@Test
	public void testConfig() throws Exception
	{
		DARConfiguration config = new DARConfiguration(getClass().getResource("/org/cipango/dar/dar.properties"));
		DefaultApplicationRouter dar = new DefaultApplicationRouter();
		
		config.configure(dar);
		
		RouterInfo[] infos = dar.getRouterInfos().get("REGISTER");
		assertNull(infos);
		
		infos = dar.getRouterInfos().get("INVITE");
		assertNotNull(infos);
		assertEquals(2, infos.length);
		
		assertEquals("OriginatingCallWaiting", infos[0].getName());
		assertEquals("CallForwarding", infos[1].getName());
		
		assertEquals("DAR:From", infos[0].getIdentity());
		assertEquals("DAR:To", infos[1].getIdentity());
	
		infos = dar.getRouterInfos().get("MESSAGE");
		assertNotNull(infos);
		assertEquals(1, infos.length);
		assertEquals("IM", infos[0].getName());
		assertEquals(SipApplicationRoutingRegion.TERMINATING_REGION, infos[0].getRegion());
	}
}
