package org.cipango.sip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.servlet.sip.SipURI;

import org.junit.Test;

public class SipURIImplTest 
{
	String[] invalids = {
			"http://www.cipango.org", 
			"sip:", 
			"sip::", 
			"sip:@", 
			"sip:alice:@", 
			"sip:alice@:5060",
			"sip:alice@invalidPort:11111111111111"
	};
	
	String[][] uris = {
			{ "sip:alice@atlanta.com", "false", "alice", null, "atlanta.com", "-1" },
			{ "sip:atlanta.com", "false", null, null, "atlanta.com", "-1" },
			{ "sip:@atlanta.com", "false", null, null, "atlanta.com", "-1" },
			{ "sip:atlanta.com:5060", "false", null, null, "atlanta.com", "5060" },
			{ "sip:alice@atlanta.com:5060", "false", "alice", null, "atlanta.com", "5060" },
			{ "sip:%61lice@atlanta.com", "false", "alice", null, "atlanta.com", "-1" },
			{ "sip:+358-555-1234567;postd=pp22@foo.com;user=phone", "false", "+358-555-1234567;postd=pp22", null, "foo.com", "-1" },
			{ "sip:biloxi.com;sos;transport=tcp;lr", "false", null, null, "biloxi.com", "-1", "sos", "", "transport", "tcp", "lr", "" },
			{ "sip:biloxi.com;name=fran%c3%a7ois;%6c%72", "false", null, null, "biloxi.com", "-1", "name", "françois", "lr", "" }	
	};
	
	static String[][] __equal = 
		{
				{"sip:%61lice@atlanta.com;transport=TCP", "sip:alice@AtlanTa.CoM;Transport=tcp"},
				{"sip:carol@chicago.com", "sip:carol@chicago.com;newparam=5"},
				{"sip:carol@chicago.com", "sip:carol@chicago.com;lr"},
				{"sip:carol@chicago.com;security=on", "sip:carol@chicago.com;newparam=5"},
				{"sip:alice@atlanta.com?subject=project%20x&priority=urgent", "sip:alice@atlanta.com?priority=urgent&subject=project%20x"}
		};
				
		static String[][] __different = 
		{
				{"sip:alice@atlanta.com", "sip:ALICE@atlanta.com"},
				{"sip:bob@biloxi.com", "sip:bob@biloxi.com:5060"},
				{"sip:bob@biloxi.com", "sip:bob@biloxi.com;transport=tcp"},
				{"sip:bob@biloxi.com", "sip:bob@biloxi.com;ttl=255"},
				{"sip:bob@biloxi.com", "sip:bob@biloxi.com;user=phone"},
				{"sip:bob@biloxi.com", "sip:bob@biloxi.com;maddr=192.168.1.2"},
				{"sip:bob@biloxi.com", "sip:bob@biloxi.com;method=INVITE"},
				{"sip:bob@biloxi.com;transport=udp", "sip:bob@biloxi.com;transport=tcp"},
				{"sip:carol@chicago.com;newparam=6", "sip:carol@chicago.com;newparam=5"},
				{"sip:carol@chicago.com", "sip:carol@chicago.com?Subject=next%20meeting"},
				{"sip:carol@chicago.com?Subject=next%20meeting", "sip:carol@chicago.com?Subject=another%20meeting"}
		};
		
	
	SipURI sipURI(String s) throws Exception
	{
		SipURIImpl uri = new SipURIImpl();
		uri.parse(s);
		
		return uri;
	}
	
	@Test
	public void testPerf() throws Exception
	{
		long start = System.currentTimeMillis();
		int n = 500000;
		
		for (int i = 0; i < n; i++)
		{
			SipURI uri = sipURI("sip:192.168.2.150");
			assertEquals(null, uri.getUser());
		}
		
		System.out.println(n * 1000l / (System.currentTimeMillis() - start));
	}
	
	@Test
	public void parse() throws Exception
	{
		SipURI uri = null;
		for (int i = 0; i < uris.length; i++)
		{
			try 
			{
				uri = sipURI(uris[i][0]);
			}
			catch (Exception e)
			{
				fail(uris[i][0] + ": " + e.getMessage());
			}
			assertEquals(uris[i][0], Boolean.parseBoolean(uris[i][1]), uri.isSecure());
			assertEquals(uris[i][0], uris[i][2], uri.getUser());
			assertEquals(uris[i][0], uris[i][3], uri.getUserPassword());
			assertEquals(uris[i][0], uris[i][4], uri.getHost());
			assertEquals(uris[i][0], Integer.parseInt(uris[i][5]), uri.getPort());
			
			if (uris[i].length > 5)
			{
				for (int j = 6; j < uris[i].length-1; j=j+2)
				{
					assertEquals(uris[i][0], uris[i][j+1], uri.getParameter(uris[i][j]));
				}
			}
		}
	}
	
	
	@Test
	public void testInvalids() throws Exception
	{
		for (int i = 0; i < invalids.length; i++)
		{
			try { sipURI(invalids[i]); fail("expected invalid " + invalids[i]); } catch (Exception e) { }
		}
	}
	

	@Test
	public void testEqual() throws Exception 
	{
		for (int i = 0; i < __equal.length; i++) 
		{
			SipURI uri1 = sipURI(__equal[i][0]);
			SipURI uri2 = sipURI(__equal[i][1]);
			assertEquals(uri1, uri2);
			assertEquals(uri2, uri1);
		}
	}

	@Test
	public void testDifferent() throws Exception 
	{
		for (int i = 8; i < __different.length; i++) 
		{
			SipURI uri1 = sipURI(__different[i][0]);
			SipURI uri2 = sipURI(__different[i][1]);
			assertFalse(__different[i][0], uri1.equals(uri2));
			assertFalse(__different[i][1], uri2.equals(uri1));
		}
	}
	
	@Test
	public void testNoValueParam() throws Exception
	{
		SipURI uri = sipURI("sip:biloxi.com");
		uri.setLrParam(true);
		uri.setParameter("custom", "");
		assertEquals("sip:biloxi.com;lr;custom", uri.toString());
	}
	
	@Test
	public void testEscapeParamName() throws Exception
	{
		SipURI uri = sipURI("sip:biloxi.com");
		uri.setParameter("escapeName?", "1");
		//System.out.println(uri.toString());
		assertEquals("sip:biloxi.com;escapeName%3f=1", uri.toString());
		assertEquals(uri, sipURI(uri.toString()));
	}
	
	@Test
	public void testEscapeParamValue() throws Exception
	{
		SipURI uri = sipURI("sip:biloxi.com");
		uri.setParameter("name", "françois");
		//System.out.println(uri.toString());
		assertEquals("sip:biloxi.com;name=fran%c3%a7ois", uri.toString());
		assertEquals(uri, sipURI(uri.toString()));
	}
	
	@Test
	public void testEscapeHeaderName() throws Exception
	{
		SipURI uri = sipURI("sip:biloxi.com");
		uri.setHeader("escapeName@", "1");
		//System.out.println(uri.toString());
		assertEquals("sip:biloxi.com?escapeName%40=1", uri.toString());
		assertEquals(uri, sipURI(uri.toString()));
	}
	
	@Test
	public void testEscapeHeaderValue() throws Exception
	{
		SipURI uri = sipURI("sip:biloxi.com");
		uri.setHeader("subject", "My call");
		//System.out.println(uri.toString());
		assertEquals("sip:biloxi.com?subject=My%20call", uri.toString());
		assertEquals(uri, sipURI(uri.toString()));
	}

	@Test
	public void testToString() throws Exception
	{
		SipURI uri = null;
		SipURI uri2 = null;
		for (int i = 0; i < uris.length; i++)
		{
			uri = sipURI(uris[i][0]);
			try 
			{
				uri2 = sipURI(uri.toString());
			}
			catch (Exception e)
			{
				fail(uris[i][0] + ": " + e.getMessage());
			}
			assertEquals(uris[i][0], uri, uri2);

		}
	}
	
	@Test
	public void testSerialize() throws Exception
	{
		for (int i = 0; i < uris.length; i++) 
		{
			SipURI uri1 = sipURI(uris[i][0]);
			
			Object o = serializeDeserialize(uri1);
			assertTrue(o instanceof SipURI);
			assertEquals(uri1, o);
			assertEquals(o, uri1);
		}
	}
	
	public static Object serializeDeserialize(Object o) throws IOException, ClassNotFoundException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		return ois.readObject();
	}
	
}
