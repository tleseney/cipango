package org.cipango.sip;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;


public class SipVersionTest 
{
	@Test
	public void testLookup() throws Exception
	{
		ByteBuffer buffer = ByteBuffer.wrap("SIP/2.0 ".getBytes());
		Assert.assertEquals(SipVersion.SIP_2_0, SipVersion.lookAheadGet(buffer));
	}
}
