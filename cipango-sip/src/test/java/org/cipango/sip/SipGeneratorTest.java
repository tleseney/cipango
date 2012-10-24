package org.cipango.sip;

import static org.junit.Assert.*;
import java.nio.ByteBuffer;

import org.eclipse.jetty.util.StringUtil;
import org.junit.Before;
import org.junit.Test;

public class SipGeneratorTest
{
	private SipGenerator _generator;
	private ByteBuffer _buffer;
	
	@Before
	public void setUp()
	{
		_generator = new SipGenerator();
		_buffer = ByteBuffer.allocate(4096);
	}

	@Test
	public void testMergeHeaders() throws Exception
	{
		SipFields fields = new SipFields();
		fields.add("Accept", "INVITE");
		fields.add("Accept", "ACK");
		
		_generator.generateRequest(_buffer, "INVITE", new SipURIImpl("sip:cipango.org"), fields, null);
		String s = toString();
		//System.out.println("**" + s + "**");
		assertEquals("INVITE sip:cipango.org SIP/2.0\r\n"
				+ "Accept: INVITE, ACK\r\n\r\n", s);
		
	}
	
	@Test
	public void testNonMergeHeaders() throws Exception
	{
		SipFields fields = new SipFields();
		fields.add("Route", "<sip:cipango.org;lr>");
		fields.add("Route", "<sip:cipango.org:5062;lr>");
		
		_generator.generateRequest(_buffer, "INVITE", new SipURIImpl("sip:cipango.org"), fields, null);
		String s = toString();
		//System.out.println("**" + s + "**");
		assertEquals("INVITE sip:cipango.org SIP/2.0\r\n"
				+ "Route: <sip:cipango.org;lr>\r\n"
				+ "Route: <sip:cipango.org:5062;lr>\r\n\r\n", s);
		
	}
	
	public String toString()
	{
		return new String(_buffer.array(), 0, _buffer.position(), StringUtil.__UTF8_CHARSET);
	}
}
