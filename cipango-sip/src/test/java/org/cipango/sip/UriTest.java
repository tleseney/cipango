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
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;

import javax.servlet.sip.URI;

import org.junit.Test;

public class UriTest
{
	public static final String[][] EQUALS = 
	{
		{"tel:+358-555-1234567;postd=pp22", "tel:+358-555-1234567;POSTD=PP22"},
		{"tel:+358-555-1234567;postd=pp22;isub=1411", "tel:+358-555-1234567;POSTD=PP22"},
		{"tel:+(35)8-555-123.4567", "tel:+3585551234567"}
	};
										
	public static final String[][] DIFFERENT = 
	{
		{"tel:+358-555-1234567;postd=pp22", "tel:358-555-1234567;postd=pp22"},
		{"tel:+358-555-1234567;postd=pp22", "fax:+358-555-1234567;postd=pp22"},
		{"tel:+358-555-1234567;postd=pp22", "tel:+358-555-1234567;postd=aaa"},
		{"http://www.nexcom.fr;param1=a", "http://www.nexcom.fr;param1=OtherValue"}
	};
	
	@Test
	public void testEqual() throws Exception 
	{
		for (int i = 0; i < EQUALS.length; i++) 
		{
			URI uri1 = URIFactory.parseURI(EQUALS[i][0]);
			URI uri2 = URIFactory.parseURI(EQUALS[i][1]);
			assertEquals(uri1, uri2);
			assertEquals(uri2, uri1);
		}
	}

	@Test
	public void testDifferent() throws Exception 
	{
		for (int i = 0; i < DIFFERENT.length; i++) 
		{
			URI uri1 = URIFactory.parseURI(DIFFERENT[i][0]);
			URI uri2 = URIFactory.parseURI(DIFFERENT[i][1]);
			assertFalse(uri1.equals(uri2));
			assertFalse(uri2.equals(uri1));
		}
	}

	@Test
	public void testSetParameter() throws Exception 
	{
		URI uri = URIFactory.parseURI("http://www.nexcom.fr;param1=a");
		assertEquals("http://www.nexcom.fr;param1=a", uri.toString());
		uri.setParameter("param2", "b");
		assertEquals("a", uri.getParameter("param1"));
		uri.removeParameter("param1");
		assertEquals("b", uri.getParameter("param2"));
		assertNull(uri.getParameter("param1"));
		assertEquals("http://www.nexcom.fr;param2=b", uri.toString());
	}
}
