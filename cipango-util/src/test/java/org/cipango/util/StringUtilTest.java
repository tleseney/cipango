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

import java.util.Random;

import org.cipango.util.StringUtil;
import org.eclipse.jetty.util.TypeUtil;
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
	
	public static final byte[] intToByteArray(int value) { return new byte[]{ (byte)(value >>> 24), (byte)(value >> 16 & 0xff), (byte)(value >> 8 & 0xff), (byte)(value & 0xff) }; }

	@Test
	public void testEncode()
	{
		assertEquals("fran%c3%a7ois", StringUtil.encode("françois"));
		assertEquals("*%20%40", StringUtil.encode("* @"));
	}
	
	@Test
	public void testBase62()
	{
		System.out.println(Long.toString(new Random().nextInt(), 36));
		System.out.println(StringUtil.toBase62String2(new Random().nextInt()));
		System.out.println(TypeUtil.toHexString(intToByteArray(new Random().nextInt())));
		System.out.println(StringUtil.toBase62String2(System.currentTimeMillis() / 1000));
	}
	
	@Test
	public void testStringEquals()
	{
		assertTrue(StringUtil.equals(null, null));
		assertFalse(StringUtil.equals("xxx", null));
		assertFalse(StringUtil.equals(null, "xxx"));
		assertTrue(StringUtil.equals("xxx", "xxx"));
	}
}
