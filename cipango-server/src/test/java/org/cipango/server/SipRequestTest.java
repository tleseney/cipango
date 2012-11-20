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

package org.cipango.server;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.sip.Address;

import org.cipango.sip.AddressImpl;
import org.junit.Test;

public class SipRequestTest
{

	@Test
	public void testPushRoute() throws Exception
	{	
		SipRequest.setStrictRoutingEnabled(true);
		SipRequest request = (SipRequest) SipMessageTest.getMessage(SipMessageTest.INVITE);
		assertFalse(request.isNextHopStrictRouting());
		
		request.pushRoute(new AddressImpl("<sip:strictRouting@nexcom.fr>", true));
		assertEquals("sip:strictRouting@nexcom.fr", request.getRequestURI().toString());
		assertEquals("<sips:ss2.biloxi.example.com>", request.getTopRoute().toString());
		assertTrue(request.isNextHopStrictRouting());
		
		request.pushRoute(new AddressImpl("<sip:strictRouting-2@nexcom.fr>", true));
		assertEquals("sip:strictRouting-2@nexcom.fr", request.getRequestURI().toString());
		ListIterator<Address> it = request.getAddressHeaders("route");
		List<String> expected = new ArrayList<String>();
		expected.add("<sip:strictRouting@nexcom.fr>");
		expected.add("<sips:ss2.biloxi.example.com>");
		assertAddress(expected, it);
		assertTrue(request.isNextHopStrictRouting());
		
		request.pushRoute(new AddressImpl("<sip:looseRouting@nexcom.fr;lr>", true));
		assertEquals("sips:ss2.biloxi.example.com", request.getRequestURI().toString());
		it = request.getAddressHeaders("route");
		expected = new ArrayList<String>();
		expected.add("<sip:looseRouting@nexcom.fr;lr>");
		expected.add("<sip:strictRouting-2@nexcom.fr>");
		expected.add("<sip:strictRouting@nexcom.fr>");
		assertAddress(expected, it);
		assertFalse(request.isNextHopStrictRouting());
		
		request.pushRoute(new AddressImpl("<sip:looseRouting-2@nexcom.fr;lr>", true));
		assertEquals("sips:ss2.biloxi.example.com", request.getRequestURI().toString());
		it = request.getAddressHeaders("route");
		expected = new ArrayList<String>();
		expected.add("<sip:looseRouting-2@nexcom.fr;lr>");
		expected.add("<sip:looseRouting@nexcom.fr;lr>");
		expected.add("<sip:strictRouting-2@nexcom.fr>");
		expected.add("<sip:strictRouting@nexcom.fr>");
		assertAddress(expected, it);
		assertFalse(request.isNextHopStrictRouting());
		
		/*-----------*/
		request = (SipRequest) SipMessageTest.getMessage(SipMessageTest.INVITE);
		request.pushRoute(new AddressImpl("<sip:looseRouting@nexcom.fr;lr>", true));
		assertEquals("sips:ss2.biloxi.example.com", request.getRequestURI().toString());
		assertEquals("<sip:looseRouting@nexcom.fr;lr>", request.getTopRoute().toString());
		assertFalse(request.isNextHopStrictRouting());
		
		request.pushRoute(new AddressImpl("<sip:strictRouting@nexcom.fr>", true));
		assertEquals("sip:strictRouting@nexcom.fr", request.getRequestURI().toString());
		it = request.getAddressHeaders("route");
		expected = new ArrayList<String>();
		expected.add("<sip:looseRouting@nexcom.fr;lr>");
		expected.add("<sips:ss2.biloxi.example.com>");
		assertAddress(expected, it);
		assertTrue(request.isNextHopStrictRouting());
		
		request.pushRoute(new AddressImpl("<sip:strictRouting-2@nexcom.fr>", true));
		assertEquals("sip:strictRouting-2@nexcom.fr", request.getRequestURI().toString());
		it = request.getAddressHeaders("route");
		expected = new ArrayList<String>();
		expected.add("<sip:strictRouting@nexcom.fr>");
		expected.add("<sip:looseRouting@nexcom.fr;lr>");
		expected.add("<sips:ss2.biloxi.example.com>");
		assertAddress(expected, it);
		assertTrue(request.isNextHopStrictRouting());
		
		SipRequest.setStrictRoutingEnabled(false);
	
	}


	
	protected void assertAddress(List<String> expected, ListIterator<Address> it)
	{
		while (it.hasNext())
		{
			int index = it.nextIndex();
			Address address = (Address) it.next();
			assertEquals(expected.get(index), address.toString());
		}
		assertEquals("Not same number of address", expected.size(), it.nextIndex());
	}

}
