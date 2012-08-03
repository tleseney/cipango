package org.cipango.console.util;

import java.io.IOException;
import java.io.Writer;

public class ReplaceTool
{
	public StringBuilder toStringBuilder(String s)
	{
		return new StringBuilder(s);
	}
	
	public StringBuilder replaceOnce(StringBuilder sb, int index, String toFind, Object toSet)
	{
		if ((index = sb.indexOf(toFind, index)) != -1)
			sb.replace(index, index + toFind.length(), toSet.toString());
		return sb;
	}
	
	public String toFixedSize(Object o, int size)
	{
		String s;
		if (o == null)
			s = "";
		else
			s = o.toString();
		if (s.length() >= size)
			return s.substring(0, size - 1);
		else
		{
			StringBuilder sb = new StringBuilder(size);
			int i = 1;
			for (; i < ((size - s.length()) /2); i++)
				sb.append(' ');
			sb.append(s);
			i +=s.length();
			for (; i < size; i++)
				sb.append(' ');
			return sb.toString();
		}
	}
}
