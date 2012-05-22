// ========================================================================
// Copyright 2008-2012 NEXCOM Systems
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
package org.cipango.util;

import org.cipango.util.StringUtil;
import org.junit.Test;
import static org.junit.Assert.*;

public class StringUtilTest 
{
	@Test
	public void testUnquote()
	{
		assertEquals(null, StringUtil.unquote(null));
		assertEquals("1", StringUtil.unquote("1"));
		assertEquals("", StringUtil.unquote("\"\""));
		assertEquals("quoted string", StringUtil.unquote("\"quoted string\""));
		assertEquals("escaped: \" foo \"", StringUtil.unquote("\"escaped: \\\"\\ \\f\\o\\o\\ \\\"\""));
	}
	
	@Test
	public void testQuote()
	{
		assertEquals("token", StringUtil.quoteIfNeeded("token", StringUtil.toBitSet(StringUtil.TOKEN)));
		assertEquals("\"hello world\"", StringUtil.quoteIfNeeded("hello world", StringUtil.toBitSet(StringUtil.TOKEN)));		
	}
}
