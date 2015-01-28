//========================================================================
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

import static org.junit.Assert.assertEquals;

import org.eclipse.jetty.util.Utf8StringBuilder;
import org.junit.Test;

public class SipHeaderTest 
{
	protected SipHeader lookup(String s)
	{
		byte[] b = s.getBytes();
		return SipHeader.lookAheadGet(b, 0, b.length);
	}
	
	@Test
	public void testCompact() 
	{
		//System.out.println(SipHeader.CACHE.keySet().size());
		assertEquals(SipHeader.CALL_ID.toString(), SipHeader.getFormattedName("i"));
		assertEquals(SipHeader.CALL_ID.toString(), SipHeader.getFormattedName("I"));
	}
	
	@Test
	public void testLookup()
	{
		assertEquals(SipHeader.CALL_ID, lookup("Call-ID: "));
		assertEquals(SipHeader.CALL_ID, lookup("call-id : "));
		assertEquals(SipHeader.AUTHORIZATION, lookup("Authorization:"));
	}
	
	@Test
	public void testBuilder()
	{
		long start = System.currentTimeMillis();
		
		Utf8StringBuilder builder = new Utf8StringBuilder();
		//StringBuilder builder = new StringBuilder();
		
		for (int i = 0; i < 1000000; i++)
		{
			builder.append((byte) 'h');
			builder.append((byte) 'e');
			builder.append((byte) 'l');
			builder.append((byte) 'l');
			builder.append((byte) 'o');
			builder.toString();
			//builder.setLength(0);
			builder.reset();
		}
		
		System.out.println(System.currentTimeMillis() - start);
	}
}
