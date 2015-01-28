/========================================================================
//Copyright 2006-2015 NEXCOM Systems
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================
package org.cipango.sip;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Test;

public class FieldsTest 
{
	@Test
	public void testCase() throws Exception
	{
		SipFields fields = new SipFields();
		fields.add(SipHeader.ACCEPT.toString().toUpperCase(), "v1");
		fields.add(SipHeader.ALLOW.toString().toLowerCase(), "v2", true);
		fields.add(SipHeader.ACCEPT_ENCODING.toString(), (Object) "v3", true);
		
		assertEquals("v1", fields.getString(SipHeader.ACCEPT.toString().toUpperCase()));
		assertEquals("v1", fields.getString(SipHeader.ACCEPT));
		assertEquals("v2", fields.get(SipHeader.ALLOW));
		assertEquals("v2", fields.getField(SipHeader.ALLOW.toString().toUpperCase()).getValue());
		assertEquals("v3", fields.getString(SipHeader.ACCEPT_ENCODING));
		assertEquals("v3", fields.getString(SipHeader.ACCEPT_ENCODING.toString().toLowerCase()));

		assertTrue(fields.getField(SipHeader.ACCEPT.toString().toUpperCase()).getHeader() == SipHeader.ACCEPT);	
		
		Iterator<String> it = fields.getNames();
		assertEquals(SipHeader.ACCEPT.toString(), it.next());
		assertEquals(SipHeader.ALLOW.toString(), it.next());
		assertEquals(SipHeader.ACCEPT_ENCODING.toString(), it.next());
		assertFalse(it.hasNext());
	}
	
	@Test
	public void testCompact() throws Exception
	{
		SipFields fields = new SipFields();
		fields.add(SipHeader.CALL_ID.toString(), "ndaksdj@192.0.2.1");
		fields.add("s", "My subject");
				
		assertEquals("My subject", fields.getString("s"));
		assertEquals("My subject", fields.getString(SipHeader.SUBJECT));
		
		assertEquals("ndaksdj@192.0.2.1", fields.getString(SipHeader.CALL_ID));
		assertEquals("ndaksdj@192.0.2.1", fields.getString("i"));
		
	}
}
