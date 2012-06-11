package org.cipango.sip;

import static junit.framework.Assert.assertEquals;

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
		
		for (int i = 0; i < 10000000; i++)
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
