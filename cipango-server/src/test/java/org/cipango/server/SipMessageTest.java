package org.cipango.server;

import static junit.framework.Assert.*;

import org.cipango.sip.AddressImpl;
import org.cipango.sip.SipHeader;
import org.cipango.sip.SipMethod;
import org.junit.Test;

public class SipMessageTest
{
	@Test
	public void testCompactHeaders() throws Exception
	{
		SipRequest request = new SipRequest();
		request.setMethod(SipMethod.REGISTER, SipMethod.REGISTER.asString());
		request._fields.add(SipHeader.CALL_ID.asString(), "aa");
		assertEquals("aa", request.getCallId());
		assertEquals("aa", request.getHeader("i"));
		
		request.addHeader("a", "<sip:accept-contact>");
		assertEquals("<sip:accept-contact>", request.getHeader(SipHeader.ACCEPT_CONTACT.asString()));
		assertEquals("<sip:accept-contact>", request.getHeader("a"));
		assertTrue(request.getHeaders("a").hasNext());
		
		AddressImpl addr = new AddressImpl("<sip:contact;lr>");
		addr.parse();
		request.addAddressHeader("m", addr, false);
		assertEquals(addr, request.getAddressHeader(SipHeader.CONTACT.asString()));
		assertEquals(addr, request.getAddressHeader("m"));
		assertTrue(request.getAddressHeaders("m").hasNext());
		
		request.removeHeader("m");
		assertNull(request.getAddressHeader(SipHeader.CONTACT.asString()));
		assertNull(request.getAddressHeader("m"));
		assertFalse(request.getAddressHeaders("m").hasNext());
		
		request.setAddressHeader("m", addr);
		assertEquals(addr, request.getAddressHeader(SipHeader.CONTACT.asString()));
		assertEquals(addr, request.getAddressHeader("m"));
		
		request.setHeader("s", "call");
		assertEquals("call", request.getHeader(SipHeader.SUBJECT.asString()));
		assertEquals("call", request.getHeader("s"));
		
	}
}
