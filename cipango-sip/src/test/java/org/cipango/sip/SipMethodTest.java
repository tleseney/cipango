package org.cipango.sip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.util.BufferUtil;
import org.junit.Test;

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
		
		assertNull(lookup("REGI"));	
		
		for (SipMethod method : SipMethod.values())
			assertEquals(method, lookup(method.toString() + " "));	
	}
	
	@Test
	public void testPerf()
	{
		long iterations = 40000;

		testSipMethodBuffer(iterations);
		testEquals(iterations);
		testEnum(iterations);
		testSipMethod(iterations);
	}
	
	private void testEnum(long iterations)
	{
		long start = System.currentTimeMillis();
		List<String> methods2 = new ArrayList<String>();
		for (SipMethod method : SipMethod.values())
		{
			methods2.add(method.toString() + " sip:bob@cipango.org SIP/2.0");
		}
		for (int i = 0; i < iterations; i++)
		{
			for (String method : methods2)
			{
				method = method.substring(0, method.indexOf(' '));
				SipMethod m = SipMethod.valueOf(method);
				if (m == null)
					System.out.println("Error for " + method);
			}
		}
		System.out.println("Enum Took " + (System.currentTimeMillis() - start) + "ms");
	}
	
	private void testEquals(long iterations)
	{
		long start = System.currentTimeMillis();
		List<String> methods = new ArrayList<String>();
		for (SipMethod method : SipMethod.values())
			methods.add((method.toString() + " sip:bob@cipango.org SIP/2.0"));
		
		for (int i = 0; i < iterations; i++)
		{
			for (String method : methods)
			{
				method = method.substring(0, method.indexOf(' '));
				if ("INVITE".equals(method)) {}
				else if ("ACK".equals(method)) {}
				else if ("BYE".equals(method)) {}
				else if ("CANCEL".equals(method)) {}
				else if ("INFO".equals(method)) {}
				else if ("MESSAGE".equals(method)) {}
				else if ("NOTIFY".equals(method)) {}
				else if ("OPTIONS".equals(method)) {}
				else if ("PRACK".equals(method)) {}
				else if ("PUBLISH".equals(method)) {}
				else if ("REFER".equals(method)) {}
				else if ("REGISTER".equals(method)) {}
				else if ("SUBSCRIBE".equals(method)) {}
				else if ("UPDATE".equals(method)) {}
				else
					System.out.println("Error for " + method);
			}
		}
		System.out.println("Equals took " + (System.currentTimeMillis() - start) + "ms");
	}
	
	private void testSipMethod(long iterations)
	{
		long start = System.currentTimeMillis();
		List<String> methods = new ArrayList<String>();
		for (SipMethod method : SipMethod.values())
			methods.add((method.toString() + " sip:bob@cipango.org SIP/2.0"));
		
		for (int i = 0; i < iterations; i++)
		{
			for (String method : methods)
			{
				SipMethod m = lookup(method);	
				if (m == null)
					System.out.println("Error for " + method);
			}
		}
		System.out.println("SipMethod took " + (System.currentTimeMillis() - start) + "ms");
	}
	
	private void testSipMethodBuffer(long iterations)
	{
		long start = System.currentTimeMillis();
		List<ByteBuffer> methods = new ArrayList<ByteBuffer>();
		for (SipMethod method : SipMethod.values())
			methods.add(BufferUtil.toBuffer(method.toString() + " sip:bob@cipango.org SIP/2.0"));
		
		for (int i = 0; i < iterations; i++)
		{
			for (ByteBuffer method : methods)
			{
				SipMethod m = SipMethod.lookAheadGet(method);	
				if (m == null)
					System.out.println("Error for " + method);
			}
		}
		System.out.println("SipMethod with ByteBuffer took " + (System.currentTimeMillis() - start) + "ms");
	}
}
