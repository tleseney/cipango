package org.cipango.sip;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;
import java.text.ParseException;
import java.util.Iterator;

import javax.servlet.sip.Address;
import javax.servlet.sip.Parameterable;

import org.junit.Test;

public class ParametrableImplTest
{
	public static final String CONTENT_TYPE = "message/external-body; access-type=\"URL\";expiration=\"Tue, 24 July 2003 09:00:00 GMT\";URL=\"http://app.example.net/calingcard.xml\"";
	
	@Test
	public void testParse() throws ParseException
	{
		String s = "<sip:agb@bell-telephone.com>;tag=a48s";
		Parameterable p = new ParameterableImpl(s);
		assertEquals("<sip:agb@bell-telephone.com>", p.getValue());
		Iterator<String> it = p.getParameterNames();
		assertTrue(it.hasNext());
		assertEquals("tag", it.next());
		assertEquals("a48s", p.getParameter("tag"));
		assertFalse(it.hasNext());
		assertEquals(s, p.toString());
	}
	
	@Test
	public void testParse2() throws Exception
	{
		String s = "<http://wwww.example.com/alice/photo.jpg> ;purpose=icon";
		ParameterableImpl p = new ParameterableImpl(s);
		
		assertEquals("<http://wwww.example.com/alice/photo.jpg>", p.getValue());
		assertEquals("icon", p.getParameter("PURPOSE"));
		
		s = "text/html    ; charset  =  ISO-8859-4";
		p = new ParameterableImpl(s);
		
		assertEquals("text/html", p.getValue());
		assertEquals("ISO-8859-4", p.getParameter("charset"));
		assertEquals("text/html;charset=ISO-8859-4", p.toString());
		
		p = new ParameterableImpl(CONTENT_TYPE);
		
		assertEquals("URL", p.getParameter("access-type"));
		assertEquals("http://app.example.net/calingcard.xml", p.getParameter("URL"));
	}

	@Test
	public void testString() throws Exception
	{
		ParameterableImpl p = new ParameterableImpl(CONTENT_TYPE);

		String s2 = p.toString();
		p = new ParameterableImpl(s2);
		
		String s3 = p.toString();
		
		assertEquals(s2, s3);
	}
	
	@Test
	public void testSerialize() throws Exception
	{
		ParameterableImpl p = new ParameterableImpl(CONTENT_TYPE);
		
		Object o = SipURIImplTest.serializeDeserialize(p);
		assertTrue(p instanceof ParameterableImpl);
		assertEquals(p, o);
	}
	
	@Test
	public void testEquals() throws ParseException
	{
		String s = "\"Alice\" <sip:alice@192.168.2.10:5071>;tag=418793445";
		Address addr = new AddressImpl(s, true);
		addr.removeParameter("p");
		Parameterable p = new ParameterableImpl(addr.toString());
		assertEquals(p, addr);
	}
	
}
