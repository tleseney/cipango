package org.cipango.sip;

import javax.servlet.sip.SipURI;

import org.junit.Test;
import static junit.framework.Assert.*;

public class SipURIImplTest 
{
	String[] invalids = {
			"http://www.cipango.org", 
			"sip:", 
			"sip::", 
			"sip:@", 
			"sip:alice:@", 
			"sip:alice@:5060"
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
	
	SipURI getURI(String s) throws Exception
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
			SipURI uri = getURI("sip:192.168.2.150");
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
				uri = getURI(uris[i][0]);
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
			try { getURI(invalids[i]); fail("expected invalid " + invalids[i]); } catch (Exception e) { }
		}
	}
}
