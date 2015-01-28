//========================================================================
//Copyright 2006-2015 NEXCOM Systems
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================
package org.cipango.sip;

import java.text.ParseException;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.cipango.util.StringScanner;
import org.cipango.util.StringUtil;

public class Parameters 
{
	private final static BitSet END_PNAME = StringUtil.toBitSet(" \t;=");
	private final static BitSet END_PVALUE = StringUtil.toBitSet(" \t;");
	
	private Map<String, String> _parameters;

	public String getParameter(String name) 
	{
		if (name == null)
			throw new NullPointerException("Null name");
		
		return _parameters != null ? _parameters.get(name) : null;
	}

	public Iterator<String> getParameterNames() 
	{
		if (_parameters == null)
			return Collections.emptyIterator();
		return Collections.unmodifiableSet(_parameters.keySet()).iterator();
	}

	public Set<Entry<String, String>> getParameters() 
	{
		if (_parameters == null)
			return Collections.emptySet();
		return Collections.unmodifiableSet(_parameters.entrySet());
	}

	public void removeParameter(String name) 
	{
		if (name == null)
			throw new NullPointerException("Null name");
		
		if (_parameters != null)
			_parameters.remove(name);		
	}

	public void setParameter(String name, String value) 
	{
		if (_parameters == null)
			_parameters = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		_parameters.put(name, value);		
	}
	
	public void parameterParsed(String name, String value)
	{
		setParameter(name, value);
	}
	
	public void parseParameters(StringScanner scanner) throws ParseException
	{
		while (!scanner.eof())
		{
			int start = scanner.position();
			scanner.skipWhitespace();
			if (!scanner.eof() && scanner.peekChar() == ';')
			{
				scanner.skipChar().skipWhitespace();
				String key = scanner.readTo(END_PNAME);
				
				if (key.length() > 0)
				{
					String value = "";
					
					scanner.skipWhitespace();
					if (!scanner.eof() && scanner.peekChar() == '=')
					{
						scanner.skipChar().skipWhitespace(); 
						
						if (!scanner.eof() && scanner.peekChar() == '"')
							value = StringUtil.unquote(scanner.readQuoted());
						else
							value = scanner.readTo(END_PVALUE);
					}
					
					if (!StringUtil.isToken(key)) 
						throw new ParseException("Invalid parameter name [" 
								+ key + "]", scanner.position());
					
					parameterParsed(key, value);
				}
			}
			else
			{
				scanner.position(start);
				return;
			}
		}
	}
	
	public void appendParameters(StringBuilder appendable)
	{
		if (_parameters != null)
		{
			for (String name : _parameters.keySet())
			{
				String value = _parameters.get(name);
				appendable.append(';');
				appendable.append(name);
				if (value != null && !"".equals(value))
				{
					appendable.append('=');
					appendable.append(StringUtil.quoteIfNeeded(value, StringUtil.TOKEN_BS));
				}
			}
		}
	}

	@Override
	protected Object clone() throws CloneNotSupportedException 
	{
		Parameters parameters = (Parameters) super.clone();
		parameters._parameters = null;
		for (Entry<String, String> entry : getParameters())
			parameters.setParameter(entry.getKey(), entry.getValue());
		return parameters;
	}
}
