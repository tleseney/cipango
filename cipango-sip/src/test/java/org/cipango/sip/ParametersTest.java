//========================================================================
//Copyright 2006-2015 NEXCOM Systems
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================
package org.cipango.sip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.cipango.util.StringScanner;
import org.eclipse.jetty.util.TreeTrie;
import org.eclipse.jetty.util.Trie;
import org.junit.Ignore;
import org.junit.Test;

public class ParametersTest 
{
	@Test
	public void testParse() throws Exception
	{
		String s = ";   p1 =   v1;    p2=   \"v2\"  ;  p3; p4=v4";
		Parameters parameters = new Parameters();
		parameters.parseParameters(new StringScanner(s));
		
		assertEquals("v1", parameters.getParameter("p1"));
		assertEquals("v2", parameters.getParameter("p2"));
		assertEquals("", parameters.getParameter("p3"));
		assertEquals("v4", parameters.getParameter("p4"));
		
	}
	
	@Test
	public void testCase() throws Exception
	{
		String s = ";UPPERCASE=v1;MixCase";
		Parameters parameters = new Parameters();
		parameters.parseParameters(new StringScanner(s));
		parameters.setParameter("lowercase", "v2");
		
		assertEquals("v1", parameters.getParameter("UPPERCASE"));
		assertEquals("v1", parameters.getParameter("upperCase"));
		assertEquals("v2", parameters.getParameter("lowercase"));
		assertEquals("v2", parameters.getParameter("LOWERCASE"));
		assertEquals("", parameters.getParameter("MixCase"));
		assertEquals("", parameters.getParameter("mixcase"));

		StringBuilder sb = new StringBuilder();
		parameters.appendParameters(sb);
		assertTrue(sb.toString().contains("UPPERCASE"));
		assertTrue(sb.toString().contains("MixCase"));
		System.out.println(sb);
		
	}
	
	@Ignore("Performance test")
	public void testPerf() throws Exception
	{
		long start = System.currentTimeMillis();
		Object[] array = new Object[1000000];
		
		for (int i = 0; i < array.length; i++)
		{
			//Map map = new HashMap<String, String>(6);
			//Map map = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
			Trie<String> map = new TreeTrie<String>();
			map.put("k1", "v1");
			map.put("k2", "v2");

			//map.put("k3", "v3");
			array[i] = map;
		}
		System.out.println("Took " + (System.currentTimeMillis() - start));
		System.out.println((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000);
	}
}
