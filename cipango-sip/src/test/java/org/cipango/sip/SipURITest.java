// ========================================================================
// Copyright 2007-2008 NEXCOM Systems
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

import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipURI;

import org.junit.Test;

public class SipURITest
{
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
	

	@Test
	public void testSipUser() throws Exception
	{
		SipURI uri = sipURI("sip:user@host.com:3261");
		assertEquals("user", uri.getUser());
		assertEquals("host.com", uri.getHost());
	}

	@Test
	public void testPassword() throws Exception
	{
		SipURI uri = sipURI("sip:user:passwd@host.com");
		assertEquals("passwd", uri.getUserPassword());
	}

	@Test
	public void testSipHost() throws Exception
	{
		assertEquals("192.168.1.1", sipURI("sip:user@192.168.1.1:3261").getHost());
		
		assertEquals("host-1.com", sipURI("sip:user@host-1.com:3261").getHost());
		assertEquals("[::1]", sipURI("sip:user@[::1]:5060").getHost());
// FIXME tmp		try {sipURI("sip:user@space here:5060"); fail(); } catch (ServletParseException e) {}
		// FIXME tmp		try {sipURI("sip:user@plus+here:5060"); fail(); } catch (ServletParseException e) {}
		SipURI uri = sipURI("sip:1234@foo.com");
		uri.setHost("::1");
		assertEquals("[::1]", uri.getHost());
		assertEquals("sip:1234@[::1]", uri.toString());
		
		uri = sipURI("sip:vivekg@chair-dnrc.example.com;transport=TCP");
		assertEquals("chair-dnrc.example.com", uri.getHost());
		assertEquals("TCP", uri.getParameter("transport"));
	}

	@Test
	public void testParam() throws Exception
	{
		SipURI uri = sipURI("sip:1234@foo.com;user=phone;lr");
		assertEquals(true, uri.getLrParam());
		assertEquals("phone", uri.getUserParam());
		assertNull(uri.getParameter("unknown"));
		assertNull(uri.getParameter("transport"));
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("user", "phone");
		params.put("lr", "");
		Iterator<String> it = uri.getParameterNames();
		while (it.hasNext())
		{
			String name = (String) it.next();
			assertTrue(params.containsKey(name));
			assertEquals(params.get(name), uri.getParameter(name));
			params.remove(name);
		}
		assertTrue(params.isEmpty());
	}

	@Test
	public void testHeader() throws Exception
	{
		SipURI uri = sipURI("sip:1234@foo.com?Subject=nexcom");
		assertEquals("nexcom", uri.getHeader("Subject"));
		assertNull(uri.getHeader("unknown"));
	}
	
	private SipURI sipURI(String s) throws Exception
	{
		return new SipURIImpl(s);
	}
	
	/*public void testPerf() throws Exception
	{
		String[] uris = {"sip:carol@chicago.com;user=phone;lr", 
						"sip:carol@chicago.com",
						"sip:carol:aa@chicago.com;lr?subject=toto"};
		int nbTest = 20000;
		
		for (int j = 0; j < uris.length;j++)
		{
			System.out.println("Perf on uri " + uris[j]);
			long start = System.nanoTime();
			for (int i = 0; i <nbTest; i++) 
			{
				new SipURIImpl(uris[j]).toString();
			}
			long time2 = (System.nanoTime() - start);
			System.out.println("SipURI2 Took: " + time2/1000000 + "ms");
				
			start = System.nanoTime();
			for (int i = 0; i <nbTest; i++) 
			{
				new SipURIImpl(uris[j]).toString();
			}
			long time1 = (System.nanoTime() - start);
			System.out.println("SIPURI1 Took: " + time1/1000000 + "ms");
			System.out.println("Increase perf: " + ((time1 - time2)*100/time1) + "%");
			
			start = System.nanoTime();
			for (int i = 0; i <nbTest; i++) 
			{
				SipURI uri = new SipURIImpl(uris[j]);
				uri.setLrParam(true);
				uri.toString();
			}
			time2 = (System.nanoTime() - start);
			System.out.println("SipURI2 Took (with modif): " + time2/1000000 + "ms");
	
			start = System.nanoTime();
			for (int i = 0; i <nbTest; i++) 
			{
				SipURI uri = new SipURIImpl(uris[j]);
				uri.setLrParam(true);
				uri.toString();
			}
			time1 = (System.nanoTime() - start);
			System.out.println("SipURI1 Took (with modif): " + time1/1000000 + "ms");
			System.out.println("Increase perf: " + ((time1 - time2)*100/time1) + "%\n");
			
		}
	}*/

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
		for (int i = 0; i < __different.length; i++) 
		{
			SipURI uri1 = sipURI(__different[i][0]);
			SipURI uri2 = sipURI(__different[i][1]);
			assertFalse(uri1.equals(uri2));
			assertFalse(uri2.equals(uri1));
		}
	}

	@Test
	public void testClone() throws Exception 
	{
		SipURI uri = sipURI("sip:user@host.com:1234;foo=bar");
		SipURI clone = (SipURI) uri.clone();
		
		assertEquals(uri.toString(), clone.toString());
		uri.setLrParam(true);
		assertFalse(clone.getLrParam());
		assertTrue(uri.getLrParam());
	}

	@Test
	public void testEscape() throws Exception
	{
		SipURI uri = sipURI("sip:+1234@example.com;user=phone;f%3Doo=%22bar%22?Subject=hello%20world");		
		assertEquals("\"bar\"", uri.getParameter("f=oo"));
		assertEquals("hello world", uri.getHeader("Subject"));
		
		uri = sipURI("sip:inside@example.com;lr");		
		SipURI uri2 = sipURI("sip:middle@example.com");
		uri2.setHeader("To", uri.toString());
		SipURI uri3 = sipURI("sip:outside@example.com");
		uri3.setHeader("From", uri2.toString());
		System.out.println(uri3);
		
		SipURI uri3b = sipURI(uri3.toString());
		assertEquals(uri3.toString(), uri3b.toString());
		SipURI uri2b = sipURI(uri3b.getHeader("From"));
		assertEquals(uri2.toString(), uri2b.toString());
		SipURI uri1b = sipURI(uri2b.getHeader("To"));
		assertEquals(uri.toString(), uri1b.toString());

	}

	@Test
	public void testHttp() throws Exception 
	{
		SipURI uri = sipURI("sip:mrf;voicexml=http://foo.bar.com/vxml/play.jsp%3Fuser%3Dsip:foo%40bar.com");
		assertEquals("http://foo.bar.com/vxml/play.jsp?user=sip:foo@bar.com", uri.getParameter("voicexml"));
	}
	
	public static void main(String[] args) throws ServletParseException, ParseException
	{
		boolean impl2 = "SipURIImpl2".equals(args[0]);
		long initStat = System.nanoTime();
		System.out.println("Use SipURIImpl2:" + impl2);
		Runtime r = Runtime.getRuntime();
		System.out.println("Total memory: " + r.totalMemory()/1048576 + "Mo");
		System.out.println("Free memory:  " + r.freeMemory()/1048576 + "Mo");
		System.out.println("Used memory:  " + (r.totalMemory() - r.freeMemory())/1048576 + "Mo");
		System.out.println("Max memory:   " + r.maxMemory()/1048576 + "Mo\n");
		
		String[] uris = {"sip:carol@chicago.com;user=phone;lr", 
				"sip:carol@chicago.com",
				"sip:carol:aa@chicago.com;lr?subject=toto"};
		int nbTest = 200000;
		SipURI[] parsed = new SipURI[nbTest];
		
		for (int j = 0; j < uris.length;j++)
		{
			System.out.println("Perf on uri " + uris[j]);
			long start = System.nanoTime();
			for (int i = 0; i <nbTest; i++) 
			{
				if (impl2)
					new SipURIImpl(uris[j]).toString();
				else
					new SipURIImpl(uris[j]).toString();
			}
			long time2 = (System.nanoTime() - start);
			System.out.println("SipURI2 Took: " + time2/1000000 + "ms");
							
			start = System.nanoTime();
			for (int i = 0; i <nbTest; i++) 
			{
				SipURI uri;
				if (impl2)
					uri = new SipURIImpl(uris[j]);
				else
					uri = new SipURIImpl(uris[j]);
				parsed[i] = uri;
				uri.setLrParam(true);
				uri.toString();
			}
			time2 = (System.nanoTime() - start);
			System.out.println("SipURI2 Took (with modif): " + time2/1000000 + "ms");	

			System.out.println("Total memory: " + r.totalMemory()/1048576 + "Mo");
			System.out.println("Free memory:  " + r.freeMemory()/1048576 + "Mo");
			System.out.println("Used memory:  " + (r.totalMemory() - r.freeMemory())/1048576 + "Mo");
			System.out.println("Max memory:   " + r.maxMemory()/1048576 + "Mo\n");
		}
		System.out.println("Test Took: " + (System.nanoTime() - initStat)/1000000000 + "s");	
	}
}


