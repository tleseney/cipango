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

package org.cipango.sip;

import java.text.ParseException;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.sip.Address;
import javax.servlet.sip.URI;

import org.cipango.util.StringScanner;
import org.cipango.util.StringUtil;

public class AddressImpl extends Parameters implements Address
{	
	public static final String TAG = "tag";
	
	private static final BitSet DISPLAY_NAME_BS = StringUtil.toBitSet(StringUtil.TOKEN + " ");
	
	private String _displayName;
	private String _string;
	private boolean _wildcard;
	
	private String _tag;
	
	private URI _uri;
	
	public AddressImpl(String string) 
	{
		_string = string;
	}
	
	public AddressImpl(URI uri)
	{
		_uri = uri;
	}
	
	public String getTag()
	{
		return _tag;
	}
	
	public String getTag2()
	{
		int m = _string.indexOf("tag=") + 4;
		
		int i = m; 
		while (i < _string.length() && (StringUtil.TOKEN.indexOf(_string.charAt(i)) != -1))
			i++;
		return _string.substring(m, i);
	}

	public void parse() throws ParseException
	{
		StringScanner scanner = new StringScanner(_string);
		
		int m = scanner.skipWhitespace().position();
		
		if (scanner.peekChar() == '*')
		{
			_wildcard = true;
			return;
		}
		
		String suri = null;
		boolean laquote = false;
		
		if (scanner.peekChar() == '"')
		{
			_displayName = StringUtil.unquote(scanner.readQuoted());
			scanner.skipToChar('<').readChar('<').mark();
			laquote = true;
		}
		else if (scanner.peekChar() == '<')
		{
			scanner.readChar('<').mark();
			laquote = true;
		}
		else
		{
			scanner.skipToChar('<');
			if (scanner.eof())
			{
				scanner.reset();
			}
			else
			{
				laquote = true;
				scanner.skipBackWhitespace();
				_displayName = scanner.readFromMark();
				scanner.skipToChar('<').skipChar();
			}
		}
		scanner.skipWhitespace().mark();
		if (laquote)
		{
			suri = scanner.skipToChar('>').skipBackWhitespace().readFromMark();
			scanner.skipToChar('>').readChar('>');
		}
		else 
		{
			suri = scanner.skipToChar(';').readFromMark();
		}
		
		_uri = new SipURIImpl(suri);
		
		parseParameters(scanner);		
	}
	
	@Override
	public String getParameter(String name) 
	{
		if ("tag".equalsIgnoreCase(name))
			return _tag;
		else
			return super.getParameter(name);
	}

	@Override
	public Iterator<String> getParameterNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Entry<String, String>> getParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see Address#getValue()
	 */
	public String getValue() 
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see Address#removeParameter(String)
	 */
	public void removeParameter(String name) 
	{
		if ("tag".equalsIgnoreCase(name))
			_tag = null;
		else 
			super.removeParameter(name);
	}

	/**
	 * @see Address#setParameter(String, String)
	 */
	public void setParameter(String name, String value) 
	{
		_string = null;
		
		if (name.equalsIgnoreCase("tag"))
			_tag = value;
		else
			super.setParameter(name, value);
	}

	@Override
	public void setValue(String value) 
	{
		// TODO Auto-generated method stub
		
	}

	/**
	 * @see Address#getDisplayName()
	 */
	public String getDisplayName() 
	{
		return _displayName;
	}

	@Override
	public int getExpires() 
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getQ() 
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @see Address#getURI()
	 */
	public URI getURI()
	{
		return _uri;
	}

	/**
	 * @see Address#isWildcard()
	 */
	public boolean isWildcard() 
	{
		return _wildcard;
	}

	/**
	 * @see Address#setDisplayName(String)
	 */
	public void setDisplayName(String name) 
	{
		_displayName = name;
	}

	@Override
	public void setExpires(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setQ(float arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setURI(URI uri) 
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public String toString()
	{
		if (_string == null)
		{
			StringBuilder buffer = new StringBuilder(64);
			if (_displayName != null)
				buffer.append(StringUtil.quoteIfNeeded(_displayName, DISPLAY_NAME_BS));
			
			buffer.append('<');
			buffer.append(_uri.toString());
			buffer.append('>');
			
			if (_tag != null)
			{
				buffer.append(";tag=");
				buffer.append(_tag);
			}
			_string = buffer.toString();
		}
		return _string;
	}
	
	@Override
	public Address clone()
	{
		try
		{
			AddressImpl clone =  (AddressImpl) super.clone();
			clone._uri = _uri.clone();
			return clone;
		}
		catch (CloneNotSupportedException e)
		{
			throw new RuntimeException(e);
		}
	}
}
