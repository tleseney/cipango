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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cipango.sip.AddressImpl;
import org.junit.Test;

public class B2bHelperTest
{
	@Test
	public void testMergeContact() throws Exception
	{ 
		AddressImpl destination = new AddressImpl("<sip:127.0.0.1:5060>", true);
		AddressImpl source = new AddressImpl("Bob <sip:bob@127.0.0.22:5070;transport=UDP;ttl=1>;p=2", true);

		B2bHelper.mergeContact(source, destination);
		assertEquals("Bob <sip:bob@127.0.0.1:5060;transport=UDP>;p=2", destination.toString());
	}

	@Test
	public void testAddMapHeaders() throws Exception
	{ 
		SipRequest request = (SipRequest) SipMessageTest.getMessage(SipMessageTest.INVITE);
		String tag = request.getFrom().getParameter("tag");
		Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
		headerMap.put("X-test", Arrays.asList("val1", "val2"));
		headerMap.put("From", Arrays.asList("\"Alice\" <sip:alice@cipango.voip>;tag=123"));
		headerMap.put("To", Arrays.asList("\"Bob\" <sip:bob@cipango.voip>;tag=123"));
		headerMap.put("Route", Arrays.asList("<sip:cipango.voip;lr>"));
		headerMap.put("Accept", Arrays.asList("text/plain"));
		headerMap.put("m", Arrays.asList("Alice <sip:alice@127.0.0.22:5070;transport=UDP>;p=2")); // Contact compact header
		new B2bHelper().addHeaders(request, headerMap);
		
		System.out.println(request);
		
		Iterator<String> it = request.getHeaders("X-test");
		assertEquals("val1", it.next());
		assertEquals("val2", it.next());
		assertFalse(it.hasNext());
		
		assertEquals("Alice <sip:alice@cipango.voip>;tag=" + tag, request.getFrom().toString());
		assertEquals("Bob <sip:bob@cipango.voip>", request.getTo().toString());
		
		it = request.getHeaders("Accept");
		assertEquals("text/plain", it.next());
		assertFalse(it.hasNext());
		
		it = request.getHeaders("Route");
		assertEquals("<sip:cipango.voip;lr>", it.next());
		assertFalse(it.hasNext());
		
		assertEquals("Alice <sip:alice@127.0.0.1:5060;transport=UDP>;p=2", request.getHeader("Contact"));
		
		headerMap.clear();
		headerMap.put("Via", Arrays.asList("SIP/2.0/UDP invalid:5061;branch=z9hG4bdes"));
		try
		{
			new B2bHelper().addHeaders(request, headerMap);
			fail("System header added");
		}
		catch (IllegalArgumentException e) {}
	}
}
