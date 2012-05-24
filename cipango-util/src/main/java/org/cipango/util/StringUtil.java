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
	public static final String ALPHA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	public static final String DIGITS = "0123456789";
	public static final String HNV_UNRESERVED = "[]/?:+$"; 
	public static final String LWS = " \r\n\t";
	public static final String MARK = "-_.!~*'()";
	public static final String PARAM_UNRESERVED = "[]/:&+$";
	public static final String PASSWD_UNRESERVED = "&=+$,";
	public static final String TOKEN = ALPHA + DIGITS + "-.!%*_+`'~";
	public static final String UNRESERVED = ALPHA + DIGITS + MARK;
	public static final String USER_UNRESERVED = "&=+$,;?/";
	
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
	
	public static int hashCode(String s) {
		int hash = 5381;


			char[] a = s.toCharArray();
			int i = 0;
			for (; i<a.length; i++) {
				hash = ((hash << 5) + hash) + a[i]; /* hash * 33 + c */
			}
		
		return hash & 0x7fffffff;
	}
}
