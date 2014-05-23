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
package org.cipango.sip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipURI;

import org.junit.Test;

public class AddressTest
{
	@Test
	public void testEquals() throws Exception
	{
		Address a1 = newAddress("<sip:%61lice@bea.com;transport=TCP;lr>;q=0.5");
		Address a2 = newAddress("<sip:alice@BeA.CoM;Transport=tcp;lr>;q=0.6;expires=3601");
		Address a3 = newAddress("<sip:alice@BeA.CoM;Transport=tcp;lr>;q=0.5;expires=3601");
		Address a4 = newAddress("<sip:alice@BeA.CoM;Transport=tcp;lr>;q=0.5;expires=3601;param");
		Address a5 = newAddress("\"display name\" <sip:alice@BeA.CoM;Transport=tcp;lr>;q=0.5;expires=3601;param");
		
		assertFalse(a1.equals(a2));
		assertFalse(a2.equals(a1));
		assertTrue(a1.equals(a3));

		assertTrue(a3.equals(a1));
		assertTrue(a1.equals(a4));
		assertTrue(a4.equals(a1));
		assertTrue(a1.equals(a5));
		assertTrue(a5.equals(a1));
	}

	@Test
	public void testSetValue() throws Exception
	{
		Address a1 = newAddress("<sip:alice@nexcom.fr;transport=UDP>;q=0.5");
		a1.setValue("Bob <sip:bob@nexcom.fr;transport=TCP>");
		assertEquals("Bob <sip:bob@nexcom.fr;transport=TCP>", a1.getValue());
		assertEquals("bob", ((SipURI) a1.getURI()).getUser());
		assertEquals("Bob", a1.getDisplayName());
		assertEquals("Bob <sip:bob@nexcom.fr;transport=TCP>;q=0.5", a1.toString());
		assertEquals("0.5", a1.getParameter("q"));
		try { a1.setValue("aa"); fail(); } catch (Exception e) {};
		
		a1.setValue("Bob <sip:bob@nexcom.fr;transport=TCP>");
		a1.setValue("<sip:carol@nexcom.fr>");
		assertNull(a1.getDisplayName());
	}

	@Test
	public void testTourtuous() throws ServletParseException
	{
		Address a1 = newAddress("sip:vivekg@chair-dnrc.example.com ; tag = 19923n");
		assertEquals("19923n", a1.getParameter("tag"));
		assertEquals("chair-dnrc.example.com", ((SipURI) a1.getURI()).getHost());
	}
	
	@Test
	public void testParam() throws ServletParseException
	{
		Address a1 = newAddress("<sip:user@192.168.1.1>;expires=300;received=\"sip:46.31.211.34:63102;transport=TCP\"");
//		System.out.println(a1);
//		System.out.println(a1.getParameter("received"));
		assertEquals("sip:46.31.211.34:63102;transport=TCP",a1.getParameter("received"));
		assertEquals("300",a1.getParameter("expires"));
	}
	
	@Test
	public void testParam2() throws ServletParseException
	{
		Address a1 = newAddress("<sip:user@192.168.1.1>;received= \"sip:46.31.211.34:63102;transport=TCP\";expires = 300;param2=\"a;2\"");
//		System.out.println(a1);
		//System.out.println(a1.getParameter("received"));
		assertEquals("sip:46.31.211.34:63102;transport=TCP",a1.getParameter("received"));
		assertEquals("300",a1.getParameter("expires"));
		assertEquals("a;2",a1.getParameter("param2"));
	}
	
	@Test
	public void testParam3() throws ServletParseException
	{
		Address a1 = newAddress("<sip:user@192.168.1.1>;expires = 300 ; param1 ; q  = 1");
//		System.out.println(a1);
		assertEquals("300",a1.getParameter("expires"));
		assertEquals("",a1.getParameter("param1"));
		assertEquals("1",a1.getParameter("q"));
	}
	
	@Test
	public void testSetParam() throws ServletParseException
	{
		Address a1 = newAddress("<sip:user@192.168.1.1>");
//		System.out.println(a1);
		a1.setExpires(300);
		a1.setParameter("param1", "");
		a1.setQ(1f);
		assertEquals("300",a1.getParameter("expires"));
		assertEquals("",a1.getParameter("param1"));
		assertEquals("1.0",a1.getParameter("q"));
		assertEquals(1.0f,a1.getQ());
		assertEquals(300,a1.getExpires());
	}
	
	@Test
	public void testGetParameterNames() throws ServletParseException
	{
		Address a1 = newAddress("<sip:user@192.168.1.1>");
//		System.out.println(a1);
		Iterator<String> it = a1.getParameterNames();
		assertFalse(it.hasNext());
		Set<Map.Entry<String, String>> set = a1.getParameters();
		assertTrue(set.isEmpty());
		
		a1.setParameter("tag", "ads");
		System.out.println(a1);
		it = a1.getParameterNames();
		set = a1.getParameters();
		assertEquals(1, set.size());
		Map.Entry<String, String> entry = set.iterator().next();
		assertEquals("tag", entry.getKey());
		assertEquals("ads", entry.getValue());
		
		assertTrue(it.hasNext());
		assertEquals("tag",it.next());
		assertFalse(it.hasNext());
		it.remove();
		
		
		assertNull(a1.getParameter("tag"));
		
		a1.setExpires(100);
		it = a1.getParameterNames();
		assertTrue(it.hasNext());
		assertEquals("expires",it.next());
		assertFalse(it.hasNext());
		set = a1.getParameters();
		assertEquals(1, set.size());
		entry = set.iterator().next();
		assertEquals("expires", entry.getKey());
		assertEquals("100", entry.getValue());
	}
	
	@Test
	public void testParamQuoteUtf8() throws ServletParseException
	{
		Address a1 = newAddress("<sip:user@192.168.1.1>;param1=\"éàô\"");
		System.out.println(a1);
		assertEquals("éàô",a1.getParameter("param1"));
		assertEquals("<sip:user@192.168.1.1>;param1=\"éàô\"",a1.toString());
	}
	
	@Test
	public void testParamEscapeQuote() throws ServletParseException
	{
		Address a1 = newAddress("<sip:user@192.168.1.1>;param1=\"foo \\\" bar\"");
		System.out.println(a1);
		assertEquals("foo \" bar",a1.getParameter("param1"));
		assertEquals("<sip:user@192.168.1.1>;param1=\"foo \\\" bar\"",a1.toString());
	}
	
	@Test (expected = ServletParseException.class)
	public void testInvalidParam() throws ServletParseException
	{
		newAddress("<sip:user@192.168.1.1:45841;transport=tcp>;expires =\"quoteNotTerminated");
	}
	
	@Test (expected = ServletParseException.class)
	public void testInvalidParam2() throws ServletParseException
	{
		newAddress("<sip:user@192.168.1.1:45841;transport=tcp>;invalidN?ame");
	}
	
	@Test
	public void testInvalidAddr() throws ServletParseException
	{
		Address address = newAddress("Missing, Quotes <sip:a.g.bell@example.com>;tag=43");
		//System.out.println(address);
		// modify the address to ensure that original string is no more cached 
		// FIXME the test should pass without this call
		address.setParameter("tag", "43"); 
		assertEquals("Missing, Quotes", address.getDisplayName());
		assertEquals("\"Missing, Quotes\" <sip:a.g.bell@example.com>;tag=43", address.toString());
	}
	
	@Test
	public void testToString() throws ServletParseException
	{
		Address address = newAddress("<sip:alice@cipango.org>");
		address.getURI().setParameter("lr", "");
		assertTrue(address.getURI() instanceof Modifiable);
		assertEquals("<sip:alice@cipango.org;lr>", address.toString());
		
		address = newAddress("<tel:+332960000>");
		address.getURI().setParameter("user", "phone");
		assertTrue(address.getURI() instanceof Modifiable);
		assertEquals("<tel:+332960000;user=phone>", address.toString());
		
		address = newAddress("<http://www.cipango.org>");
		address.getURI().setParameter("jsessionid", "1");
		assertTrue(address.getURI() instanceof Modifiable);
		assertEquals("<http://www.cipango.org;jsessionid=1>", address.toString());
	}
	
	
	private Address newAddress(String string) throws ServletParseException
	{
		try
		{
			AddressImpl address = new AddressImpl(string);
			address.parse();
			return address;
		}
		catch (ParseException e)
		{
			throw new ServletParseException(e);
		}
	}
	
}
