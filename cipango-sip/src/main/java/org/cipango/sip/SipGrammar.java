// ========================================================================
// Copyright 2008-2009 NEXCOM Systems
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

import java.util.BitSet;

public class SipGrammar 
{
	public static final byte CR = 0x0D;
	public static final byte LF = 0x0A;
	public static final byte TAB = 0x09;
	public static final byte SPACE = (byte) ' ';
	public static final byte COLON = (byte) ':';
	public static final byte[] CRLF = {CR, LF};

	public static final String MAGIC_COOKIE = "z9hG4bK";
	
	public static final BitSet getBitSet(String s)
	{
		BitSet bs = new BitSet(256);
		for (int i = 0; i < s.length(); i++)
			bs.set(s.charAt(i));
		return bs;
	}
	
	enum Charset
	{
		ALPHA("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"),
		DIGITS("0123456789"),
	    HNV_UNRESERVED("[]/?:+$"), 
	    LWS(" \r\n\t"),
		MARK("-_.!~*'()"),
	    PARAM_UNRESERVED("[]/:&+$"),
	    PASSWD_UNRESERVED("&=+$,"),
	    TOKEN("-.!%*_+`'~"),
		UNRESERVED(ALPHA.toString() + DIGITS.toString() + MARK.toString()),
		USER_UNRESERVED("&=+$,;?/");
		
		private String _string;
		private BitSet _bitset;
		
		Charset(String s)
		{
			_string = s;
			_bitset = getBitSet(s);
		}
		
		public String toString()
		{
			return _string;
		}
		
		public BitSet toBitSet()
		{
			return _bitset;
		}
		
		public boolean contains(int c)
		{
			return (c>0 && _bitset.get(c));
		}
		
		public boolean contains(String s)
		{
			for (int i = 0; i < s.length(); i++)
			{
				if (!contains(s.charAt(i)))
					return false;
			}
			return true;				
		}
	}
	
	
	/*
	public static final boolean isURIScheme(String s)
	{
		if (s == null || s.length() == 0)
			return false;
		if (!__alpha.contains(s.charAt(0))) return false;
		
		for (int i = 1; i < s.length(); i++)
		{
			if (!__scheme.contains(s.charAt(i))) return false;
		}
		return true;
	}
	
	public static final boolean isToken(String s)
	{
		if (s.length() == 0) return false;
		
		for (int i = 0; i < s.length(); i++) 
		{
			if (!__token.contains(s.charAt(i)))
				return false;
		}
		return true;
	}
	
	public static final boolean isTokens(String s) 
	{
		for (int i = 0; i < s.length(); i++) 
		{
			int c = s.charAt(i);
			if (!__token.contains(c) && !__lws.contains(c))
				return false;
		}
		return true;
	}
	
	public static final boolean isLWS(int c) 
	{
		return __lws.contains(c);
	}
	
	public static final String escape(String s, Charset charset)
	{
		StringBuffer escaped = null;
		for (int i = 0; i < s.length(); i++)
		{
			int c = s.charAt(i);
			if (charset.contains(c) && c != '%')
			{
				if (escaped != null) 
					escaped.append((char) c);
			}
			else 
			{
				if (escaped == null)
				{
					escaped = new StringBuffer(s.length() + 6);
					escaped.append(s.substring(0, i));
				}
				escaped.append('%');
				
				escaped.append(toHex(c));
			}
		}
		return escaped != null ? escaped.toString() : s;
	}
	
	private static String toHex(int c) 
	{
	    String s = Integer.toString(c, 16).toUpperCase();
	    if (c > 0xf)
		return s;
	    else 
		return "0" + s;
	    
	}
	
	public static String unescape(String s)
	{
		StringBuffer unescaped = null;
		for (int i = 0; i < s.length(); i++)
		{
			int c = s.charAt(i);
			if (c == '%')
			{
				int c2;
				try
				{
					c2 = Integer.parseInt(s.substring(i+1, i+3), 16);
				}
				catch (Exception e)
				{
					throw new IllegalArgumentException("Invalid escaped char at " + i + "in [" + s + "]");
				}
				if (unescaped == null)
				{
					unescaped = new StringBuffer(s.length() - 2);
					unescaped.append(s.substring(0, i));
				}
				unescaped.append((char) c2);
				i += 2;
			}
			else 
			{
				if (unescaped != null)
					unescaped.append((char) c);
			}
		}
		return unescaped != null ? unescaped.toString() : s;
	}
	
	public static String escapeQuoted(String s)
	{
		StringBuffer escaped = null;
		for (int i = 0; i < s.length(); i++)
		{
			int c = s.charAt(i);
			if (c == '"' || c == '\\')
			{
				if (escaped == null)
				{
					escaped = new StringBuffer(s.length() + 2);
					escaped.append(s.substring(0, i));
				}
				escaped.append('\\');
			}
			if (escaped != null) 
				escaped.append((char) c);
		}
		return escaped != null ? escaped.toString() : s;
	}
	
	public static String unquote(String s)
	{
		if (s != null && s.startsWith("\"") && s.endsWith("\""))
		{
			s = s.substring(1, s.length() - 1);
			StringBuffer unescaped = null;
			for (int i = 0; i < s.length(); i++)
			{
				int c = s.charAt(i);
				if (c == '\\')
				{
					int c2;
					try
					{
						c2 = s.charAt(i+1);
					}
					catch (Exception e)
					{
						throw new IllegalArgumentException("Invalid escaped char at " + i + "in [" + s + "]");
					}
					if (unescaped == null)
					{
						unescaped = new StringBuffer(s.length() - 1);
						unescaped.append(s.substring(0, i));
					}
					unescaped.append((char) c2);
					i += 1;
				}
				else 
				{
					if (unescaped != null)
						unescaped.append((char) c);
				}
			}
			return unescaped != null ? unescaped.toString() : s;
		}
		else 
		{
			return s;
		}
	}
	
	public static String toHexString(int c)
	{
		StringBuffer buf = new StringBuffer();
		HexString.appendHex(buf, c);
		return buf.toString();
	}
	
	*/
}
