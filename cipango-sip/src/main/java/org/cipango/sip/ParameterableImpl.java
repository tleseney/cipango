// ========================================================================
// Copyright 2008-2015 NEXCOM Systems
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

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Map.Entry;

import javax.servlet.sip.Parameterable;

import org.cipango.util.StringScanner;

public class ParameterableImpl extends Parameters implements Parameterable, Serializable
{
	private static final long serialVersionUID = 1L;
	
	private transient String _value;
	
	public ParameterableImpl()
	{
	}
	
	public ParameterableImpl(String s) throws ParseException
	{
		parse(s);
	}
	
	private void parse(String s) throws ParseException
	{
		StringScanner scanner = new StringScanner(s);
		
		scanner.skipToChar(';');
		
		if (scanner.eof()) 
			_value = s;
		else
		{
			_value = scanner.skipBackWhitespace().readFromMark();
			parseParameters(scanner);
		} 
	}

	/**
	 * @see Parameterable#getValue()
	 */
	public String getValue() 
	{
		return _value;
	}

	/**
	 * @see Parameterable#setValue(String)
	 */
	public void setValue(String value) 
	{
		_value = value;		
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(128);
		sb.append(_value);
		appendParameters(sb);
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof Parameterable)) 
			return false;
		Parameterable p = (Parameterable) o;
		
		if (!_value.equals(p.getValue()))
			return false;
		
		for (Entry<String, String> entry : getParameters())
		{
			String otherValue = p.getParameter(entry.getKey()); 
			if (otherValue != null && !entry.getValue().equalsIgnoreCase(otherValue))
				return false;
		}
		return true;
		
	}

	@Override
	public Parameterable clone()
	{
		try
		{
			return new ParameterableImpl(toString());
		}
		catch (ParseException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException
	{
		out.writeUTF(toString());
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		try
		{
			parse(in.readUTF());
		}
		catch (ParseException e)
		{
			throw new IOException(e);
		}
	}
}
