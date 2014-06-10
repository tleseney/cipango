package org.cipango.sip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.text.ParseException;

import javax.servlet.sip.Address;
import javax.servlet.sip.Parameterable;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import org.junit.Test;

public class AddressImplTest 
{
	String[][] addrs = {
			{ "\"A. G. Bell\" <sip:agb@bell-telephone.com> ;tag=a48s", "A. G. Bell", "tag" ,"a48s" }, 
			{ "sip:+12125551212@server.phone2net.com;tag=887s", null, "tag", "887s" },
			{ "Anonymous <sip:c8oqz84zk7z@privacy.org>;tag=hyh8", "Anonymous", "tag", "hyh8" },
			{ "Bob    <   sip:bob@biloxi.com>   ;   tag   =    ezq32lmpF     ;", "Bob", "tag", "ezq32lmpF"}
	};
	
	private Address getAddress(String s) throws ParseException
	{
		AddressImpl addr = new AddressImpl(s);
		addr.parse();
		return addr;
	}
	
	@Test
	public void testWildCard() throws Exception
	{
		Address address = getAddress("*");
		assertTrue(address.isWildcard());
		assertEquals("*", address.toString());
		assertEquals(address, address.clone());
	}
	
	@Test 
	public void testUri() throws Exception
	{
		Address address = getAddress(addrs[3][0]);
		URI uri = address.getURI();
		assertTrue(uri.isSipURI());
		
		SipURI sipUri = (SipURI) uri;
		assertEquals("bob", sipUri.getUser());
		assertEquals("biloxi.com", sipUri.getHost());
		
		address = getAddress(addrs[1][0]);
		assertEquals("+12125551212", ((SipURI) address.getURI()).getUser());
		assertNull(((SipURI) address.getURI()).getParameter("tag"));
	}
	
	@Test 
	public void testParse() throws Exception
	{
		for (int i = 0; i < addrs.length; i++)
		{
			Address address = getAddress(addrs[i][0]);
			assertEquals(addrs[i][1], address.getDisplayName());
			
			if (addrs[i].length > 1)
			{
				for (int j = 2; j < addrs[i].length-1; j=j+2)
				{
					assertEquals(addrs[i][0], addrs[i][j+1], address.getParameter(addrs[i][j]));
				}
			}
		}
	}
	
	@Test
	public void testPerf() throws Exception
	{
		for (int i = 0; i < 10000; i++)
		{
			Address address = getAddress("Alice <sip:alice@atlanta.com:5060>;");
			assertEquals("Alice",address.getDisplayName());
		}
		
		long start = System.currentTimeMillis();
		int n = 1000000;
		
		for (int i = 0; i < n; i++)
		{
			//Address address = getAddress("Alice <sip:alice@atlanta.com:5060>;tag=toto");
			//assertEquals("Alice",address.getDisplayName());
			assertEquals("887s", new AddressImpl("sip:+12125551212@server.phone2net.com;tag=887s").getTag2());
		}
		
		System.out.println(1000l*n / (System.currentTimeMillis() - start));
		
		System.out.println(new AddressImpl("sip:+12125551212@server.phone2net.com;tag=887s").getTag2());
	}
	
	@Test
	public void testSerialize() throws Exception
	{
		for (int i = 0; i < addrs.length; i++) 
		{
			AddressImpl address = new AddressImpl(addrs[i][0]);
			address.parse();
			
			Object o = SipURIImplTest.serializeDeserialize(address);
			assertTrue(o instanceof Address);
			assertEquals(address, o);
			assertEquals(o, address);
		}
	}
	
	@Test
	public void testClone() throws Exception
	{
		Address p = new AddressImpl(addrs[0][0], true);
		p.setParameter("tag2", "tag1");
		Address p2 = (Address) p.clone();
		assertEquals(p, p2);
		p2.setParameter("tag2", "newTag");
		//System.out.println(p2);
		//System.out.println(p);
		assertNotEquals(p, p2);
	}
	
	/**
	 * Ensure that same instance is retrieved for the URI.
	 */
	@Test
	public void testSerialize2() throws Exception
	{
		for (int i = 0; i < addrs.length; i++) 
		{
			AddressImpl address = new AddressImpl(addrs[i][0]);
			address.parse();
			
			Address2 address2 = new Address2(address);
			
			Address2 serialize = (Address2) SipURIImplTest.serializeDeserialize(address2);
			
			assertSame(serialize._addr.getURI(), serialize._uri);
			
		}
	}
	
	private static class Address2 implements Serializable
	{
		private static final long serialVersionUID = 1L;
		private Address _addr;
		private URI _uri;
		
		public Address2(Address address)
		{
			_addr = address;
			_uri = address.getURI();
		}
	}
}
