package org.cipango.sip;

import org.eclipse.jetty.util.BufferUtil;
import org.junit.Test;
import static junit.framework.Assert.*;

public class SipMethodTest 
{
	protected SipMethod lookup(String s)
	{
		return SipMethod.lookAheadGet(BufferUtil.toBuffer(s));
	}
	
	@Test
	public void testLookup()
	{
		assertEquals(SipMethod.INVITE, lookup("INVITE "));
		assertEquals(SipMethod.ACK, lookup("ACK "));
		assertEquals(SipMethod.BYE, lookup("BYE "));
		
		assertEquals(SipMethod.REGISTER, lookup("REGISTER "));	
		assertNotSame(SipMethod.REGISTER, lookup("REGISTER--- "));	
		
	}
}
