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

import java.io.IOException;
import java.util.BitSet;

public class StringUtil extends org.eclipse.jetty.util.StringUtil
{
	final static String[] hex = {
	    "%00", "%01", "%02", "%03", "%04", "%05", "%06", "%07",
	    "%08", "%09", "%0a", "%0b", "%0c", "%0d", "%0e", "%0f",
	    "%10", "%11", "%12", "%13", "%14", "%15", "%16", "%17",
	    "%18", "%19", "%1a", "%1b", "%1c", "%1d", "%1e", "%1f",
	    "%20", "%21", "%22", "%23", "%24", "%25", "%26", "%27",
	    "%28", "%29", "%2a", "%2b", "%2c", "%2d", "%2e", "%2f",
	    "%30", "%31", "%32", "%33", "%34", "%35", "%36", "%37",
	    "%38", "%39", "%3a", "%3b", "%3c", "%3d", "%3e", "%3f",
	    "%40", "%41", "%42", "%43", "%44", "%45", "%46", "%47",
	    "%48", "%49", "%4a", "%4b", "%4c", "%4d", "%4e", "%4f",
	    "%50", "%51", "%52", "%53", "%54", "%55", "%56", "%57",
	    "%58", "%59", "%5a", "%5b", "%5c", "%5d", "%5e", "%5f",
	    "%60", "%61", "%62", "%63", "%64", "%65", "%66", "%67",
	    "%68", "%69", "%6a", "%6b", "%6c", "%6d", "%6e", "%6f",
	    "%70", "%71", "%72", "%73", "%74", "%75", "%76", "%77",
	    "%78", "%79", "%7a", "%7b", "%7c", "%7d", "%7e", "%7f",
	    "%80", "%81", "%82", "%83", "%84", "%85", "%86", "%87",
	    "%88", "%89", "%8a", "%8b", "%8c", "%8d", "%8e", "%8f",
	    "%90", "%91", "%92", "%93", "%94", "%95", "%96", "%97",
	    "%98", "%99", "%9a", "%9b", "%9c", "%9d", "%9e", "%9f",
	    "%a0", "%a1", "%a2", "%a3", "%a4", "%a5", "%a6", "%a7",
	    "%a8", "%a9", "%aa", "%ab", "%ac", "%ad", "%ae", "%af",
	    "%b0", "%b1", "%b2", "%b3", "%b4", "%b5", "%b6", "%b7",
	    "%b8", "%b9", "%ba", "%bb", "%bc", "%bd", "%be", "%bf",
	    "%c0", "%c1", "%c2", "%c3", "%c4", "%c5", "%c6", "%c7",
	    "%c8", "%c9", "%ca", "%cb", "%cc", "%cd", "%ce", "%cf",
	    "%d0", "%d1", "%d2", "%d3", "%d4", "%d5", "%d6", "%d7",
	    "%d8", "%d9", "%da", "%db", "%dc", "%dd", "%de", "%df",
	    "%e0", "%e1", "%e2", "%e3", "%e4", "%e5", "%e6", "%e7",
	    "%e8", "%e9", "%ea", "%eb", "%ec", "%ed", "%ee", "%ef",
	    "%f0", "%f1", "%f2", "%f3", "%f4", "%f5", "%f6", "%f7",
	    "%f8", "%f9", "%fa", "%fb", "%fc", "%fd", "%fe", "%ff"
	  };
	
	public static final String ALPHA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	public static final String DIGITS = "0123456789";
	public static final String HEX_DIGITS = DIGITS + "abcdefABCDEF";
	public static final String HNV_UNRESERVED = "[]/?:+$"; 
	public static final String LWS = " \r\n\t";
	public static final String MARK = "-_.!~*'()";
	public static final String PARAM_UNRESERVED = "[]/:&+$";
	public static final String PASSWD_UNRESERVED = "&=+$,";
	public static final String TOKEN = ALPHA + DIGITS + "-.!%*_+`'~";
	public static final String UNRESERVED = ALPHA + DIGITS + MARK;
	public static final String USER_UNRESERVED = "&=+$,;?/";
	
	public static final BitSet PARAM_BS = toBitSet(UNRESERVED + PARAM_UNRESERVED);
	public static final BitSet TOKEN_BS = toBitSet(TOKEN); 
	public static final BitSet HEADER_BS = toBitSet(UNRESERVED + HNV_UNRESERVED);
	
	public static String quoteIfNeeded(String s, BitSet bs)
	{
		if (s == null) return null;
		if (s.length() == 0) return "\"\"";
		
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (c == '\\' || c == '"' || !bs.get(c))
			{
				StringBuilder sb = new StringBuilder(s.length()+4);
				quote(s, sb);
				return sb.toString();
			}
		}
		return s;
	}
	
	public static final boolean isToken(String s) 
	{
		return contains(s, TOKEN_BS);
	}
		
	public static final boolean contains(String s, BitSet bs) 
	{
		for (int i = 0; i < s.length(); i++) 
		{
			int c = s.charAt(i);
			if (!bs.get(c))
				return false;
		}
		return true;
	}
	
	public static void quote(String s, Appendable buffer)
	{
		try
		{
			buffer.append('"');
			for (int i = 0; i < s.length(); i++)
			{
				char c = s.charAt(i);
				if (c == '\\' || c == '"')
					buffer.append('\\');
				buffer.append(c);
			}
			buffer.append('"');
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public static String unquote(String s)
	{
		if (s == null || s.length() < 2) return s;
		if (s.charAt(0) != '"' || s.charAt(s.length()-1) != '"') return s;
		
		s = s.substring(1, s.length()-1);
		StringBuilder escaped = null;
		
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (c == '\\')
			{
				i++;
				if (i == s.length())
					throw new IllegalArgumentException("invalid escape");
				c = s.charAt(i);
				if (escaped == null)
				{
					escaped = new StringBuilder(s.length()-1);
					escaped.append(s.substring(0, i-1));
				}
				escaped.append(c);
			}
			else
			{
				if (escaped != null)
					escaped.append(c);
			}
		}
		return escaped == null ? s : escaped.toString();
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
	
	public static final BitSet toBitSet(String s)
	{
		BitSet bs = new BitSet(256);
		for (int i = 0; i < s.length(); i++)
			bs.set(s.charAt(i));
		return bs;
	}
	
	private static final char[] BASE62 = 
			"0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
		
	public static String toBase62String(int i) 
	{
        char buf[] = new char[33];
        boolean negative = (i < 0);
        int charPos = 32;

        if (!negative) {
            i = -i;
        }

        while (i <= -62) {
            buf[charPos--] = BASE62[-(i % 62)];
            i = i / 62;
        }
        buf[charPos] = BASE62[-i];

        if (negative) {
            buf[--charPos] = '-';
        }

        return new String(buf, charPos, (33 - charPos));
    }

	public static String toBase62String2(int i) 
	{
		if (i == Integer.MIN_VALUE)
			return "-2lkCB2";
		
        char buf[] = new char[33];
        boolean negative = i < 0;
        int pos = 32;

        if (negative) 
        	i = -i;
        
        while (i >= 62) {
            buf[pos--] = BASE62[(i % 62)];
            i = i / 62;
        }
        buf[pos] = BASE62[i];

        if (negative) 
            buf[--pos] = '-';

        return new String(buf, pos, (33 - pos));
    }
	
	public static String toBase62String2(long i) 
	{
		if (i == Integer.MIN_VALUE)
			return "-2lkCB2";
		
        char buf[] = new char[33];
        boolean negative = i < 0;
        int pos = 32;

        if (negative) 
        	i = -i;
        
        while (i >= 62) {
            buf[pos--] = BASE62[(int)(i % 62)];
            i = i / 62;
        }
        buf[pos] = BASE62[(int)i];

        if (negative) 
            buf[--pos] = '-';

        return new String(buf, pos, (33 - pos));
    }
	
	public static int hashCode(String s) 
	{
		int hash = 5381;


			char[] a = s.toCharArray();
			int i = 0;
			for (; i<a.length; i++) {
				hash = ((hash << 5) + hash) + a[i]; /* hash * 33 + c */
			}
		
		return hash & 0x7fffffff;
	}
	
	public static String encode(String s)
	{
		return encode(s, toBitSet(UNRESERVED));
	}
	
	public static String encode(String s, BitSet unreserved)
	{
	    StringBuilder buffer = null;
	    int len = s.length();
	    for (int i = 0; i < len; i++) {
	      int ch = s.charAt(i);
	      if (unreserved.get(ch))
	      {
	    	  if (buffer != null)
	    		  buffer.append((char) ch);
	      }
	      else
	      { 
	    	  if (buffer == null)
	    	  {
	    		  buffer = new StringBuilder();
	    		  buffer.append(s.substring(0, i));
	    	  }
	    	  if (ch <= 0x007f) 
	    	  {		// other ASCII
	    		  buffer.append(hex[ch]);
	    	  } 
	    	  else if (ch <= 0x07FF) 
	    	  {		// non-ASCII <= 0x7FF
	    		  buffer.append(hex[0xc0 | (ch >> 6)]);
	    		  buffer.append(hex[0x80 | (ch & 0x3F)]);
	    	  } 
	    	  else 
	    	  {					// 0x7FF < ch <= 0xFFFF
	    		  buffer.append(hex[0xe0 | (ch >> 12)]);
	    		  buffer.append(hex[0x80 | ((ch >> 6) & 0x3F)]);
	    		  buffer.append(hex[0x80 | (ch & 0x3F)]);
	    	  }
	      }
	    }
	    return buffer == null ? s : buffer.toString();
	  }
	
	public static boolean equals(String s1, String s2)
	{
		if (s1 == null)
			return s2 == null;
		else
			return s1.equals(s2);
	}
	
	public static boolean equalsIgnoreCase(String s1, String s2)
	{
		if (s1 == null)
			return s2 == null;
		else
			return s1.equalsIgnoreCase(s2);
	}
}
