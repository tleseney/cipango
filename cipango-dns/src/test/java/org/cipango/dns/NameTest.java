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
package org.cipango.dns;

import static org.junit.Assert.*;
import org.junit.Test;


public class NameTest
{

	@Test
	public void testParsing()
	{
		Name name = new Name("cipango.org");
		assertEquals("cipango.org", name.toString());
		assertEquals("cipango", name.getLabel());
		assertEquals("org", name.getChild().getLabel());
		assertFalse(name.getChild().hasChild());
	}
	
	@Test
	public void testEquals()
	{
		assertEquals(new Name("cipango.org"), new Name("cipango.org"));
		assertEquals(new Name("org"), new Name("org"));
		assertFalse(new Name("cipango.org").equals(new Name("cipango")));
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testBigLabel()
	{
		new Name("bigLabel012345678901234567890123456789012345678901234567890123456789.org");
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testBigName()
	{
		new Name("bigName.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.org");
	}
	
	@Test
	public void testAppend()
	{
		Name name = new Name("jira");
		name.append(new Name("cipango.org"));
		assertEquals("jira.cipango.org", name.toString());
		
		name = new Name("www.l");
		name.append(new Name("cipango.org"));
		assertEquals("www.l.cipango.org", name.toString());
		
	}
}
