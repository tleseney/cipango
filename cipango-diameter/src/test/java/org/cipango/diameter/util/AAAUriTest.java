// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
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
package org.cipango.diameter.util;



import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class AAAUriTest
{
	@Test
	public void testParse()
	{
		AAAUri uri = new AAAUri("aaa://cipango.org:3869");
		assertEquals(3869, uri.getPort());
		assertEquals("cipango.org", uri.getFQDN());
		assertEquals("aaa://cipango.org:3869", uri.toString());
		assertEquals(uri, new AAAUri(uri.toString()));
		
		uri = new AAAUri("aaa://[8c9a:a296:9000:203:ba00:40d1:f5df]:13869");
		assertEquals(13869, uri.getPort());
		assertEquals("[8c9a:a296:9000:203:ba00:40d1:f5df]", uri.getFQDN());
		assertEquals(uri, new AAAUri(uri.toString()));
		assertEquals(uri, new AAAUri("8c9a:a296:9000:203:ba00:40d1:f5df", 13869));
				
		try { new AAAUri("sip:cipango.org"); fail(); } catch (IllegalArgumentException e) {}
		try { new AAAUri("aaa://[cipango.org"); fail(); } catch (IllegalArgumentException e) {}
		try { new AAAUri("aaa://cipango.org:aa"); fail(); } catch (IllegalArgumentException e) {}
	}
	
	@Test
	public void testParameters()
	{
		AAAUri uri = new AAAUri("aaa://host.example.com;transport=tcp");
		assertEquals(-1, uri.getPort());
		assertEquals("host.example.com", uri.getFQDN());
		assertEquals(uri, new AAAUri(uri.toString()));
		

	}
	
	
}
