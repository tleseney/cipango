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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StringScannerTest 
{
	@Test
	public void testSkip() throws Exception
	{
		StringScanner scanner = new StringScanner("a1;,:;   2\t    \t 3defezjfjlk  4  kdlopvhbejo 5        6   ");
		
		scanner.skipChar().readChar('1');
		scanner.skipChars(StringUtil.toBitSet(";,/+: ")).readChar('2');
		scanner.skipWhitespace().readChar('3');
		scanner.skipToChar('4').readChar('4');
		scanner.skipToOneOf(StringUtil.toBitSet("0123456789")).readChar('5');
		scanner.skipWhitespace();
		assertEquals('6', scanner.peekChar());
		scanner.skipBackWhitespace();
		assertEquals('5', scanner.charAt(scanner.position()-1));
		
		assertTrue(scanner.end().eof());
		assertEquals('6', scanner.skipBackWhitespace().charAt(scanner.position()-1));
	}
	
	@Test
	public void testRead() throws Exception
	{
		StringScanner scanner = new StringScanner("1    foo, 12345-- \"quote: \\\"\"    bar ");
		scanner.readChar('1');
		assertEquals("foo", scanner.skipWhitespace().readTo(StringUtil.toBitSet(",")));
		assertEquals(12345, scanner.readChar(',').skipWhitespace().readInt());
		
		scanner.skipToChar('"');
		assertEquals("\"quote: \\\"\"", scanner.readQuoted());
		assertEquals("bar", scanner.skipWhitespace().mark().skipToOneOf(StringUtil.toBitSet(" ")).readFromMark());
	
		scanner = new StringScanner("123 456 789");
		assertEquals(123, scanner.readInt());
		assertEquals(456, scanner.skipWhitespace().readInt());
		assertEquals(789, scanner.skipWhitespace().readInt());
		
	}
}
