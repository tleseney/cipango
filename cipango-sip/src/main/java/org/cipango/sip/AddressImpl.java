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
import java.util.AbstractMap;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.sip.Address;
import javax.servlet.sip.URI;

import org.cipango.util.StringScanner;
import org.cipango.util.StringUtil;

public class AddressImpl extends Parameters implements Address, Serializable
{	
	private static final long serialVersionUID = 1L;

	public static final String TAG = "tag";
	
	private static final BitSet DISPLAY_NAME_BS = StringUtil.toBitSet(StringUtil.TOKEN + " ");
	
	private transient String _displayName;
	private transient String _string;
	private transient boolean _wildcard;
	
	private transient String _tag;
	
	private transient URI _uri;
	
	public AddressImpl(String string) 
	{
		_string = string;
	}
	
	public AddressImpl(String string, boolean parse) throws ParseException 
	{
		_string = string;
		if (parse)
			parse();
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
		String s = _string;
		parse(_string, true);
		_string = s;
	}
	
	
	private void parse(String address, boolean parseParam) throws ParseException 
	{
		StringScanner scanner = new StringScanner(address);
		
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
			suri = scanner.skipToChar(';').skipBackWhitespace().readFromMark();
		}
		
		_uri = URIFactory.parseURI(suri);
		
		if (parseParam)
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
	public Iterator<String> getParameterNames() 
	{
		Iterator<String> it = super.getParameterNames();
		if (_tag != null)
			it = new TagIterator(it);
		return it;
	}

	@Override
	public Set<Entry<String, String>> getParameters() 
	{
		Set<Entry<String, String>> set = super.getParameters();
		if (_tag != null)
		{
			set = new HashSet<Entry<String, String>>(set);
			set.add(new AbstractMap.SimpleEntry<String, String>("tag", _tag));
			set = Collections.unmodifiableSet(set);
		}
		return set;
	}

	/**
	 * @see Address#getValue()
	 */
	public String getValue() 
	{
		return getValueBuffer().toString();
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
		_string = null;
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
		_string = null;
	}

	@Override
	public void setValue(String value) 
	{
		if (value == null)
			throw new NullPointerException("Null value");
		
		try
		{
			_displayName = null;
			_wildcard = false;
			_string = null;
			_uri = null;
			parse(value, false);
		}
		catch (ParseException e)
		{
			// FIXME change exception class
			throw new RuntimeException(e);
		}
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
		String expires = getParameter("expires");
		if (expires != null) 
		{
			try 
			{
				return Integer.parseInt(expires);
			} 
			catch (NumberFormatException _) { }
		}
		return -1;
	}

	@Override
	public float getQ() 
	{
		String q = getParameter("q");
		if (q != null) 
		{
			try 
			{
				return Float.parseFloat(q);
			} 
			catch (NumberFormatException _) 
			{
			}
		}
		return -1.0f;
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
		_string = null;
	}

	@Override
	public void setExpires(int seconds) 
	{
		if (seconds < 0) 
			removeParameter("expires");
		else 
			setParameter("expires", Integer.toString(seconds));
	}

	@Override
	public void setQ(float q) 
	{
		if (q == -1.0f) 
			removeParameter("q");
		else 
		{
			if (q < 0 || q > 1.0f) 
				throw new IllegalArgumentException("Invalid q value:" + q);
			setParameter("q", String.valueOf(q));
		}
	}

	@Override
	public void setURI(URI uri) 
	{
		if (uri == null)
			throw new NullPointerException("Null URI");
		_uri = uri;
		_string = null;
	}
	
	@Override
	public String toString()
	{
		if (isWildcard())
			return "*";
		
		boolean uriModified = !(_uri instanceof Modifiable) || ((Modifiable) _uri).hasBeenModified();
		if (_string == null || uriModified)
		{
			StringBuilder buffer = getValueBuffer();
			
			if (_tag != null)
			{
				buffer.append(";tag=");
				buffer.append(_tag);
			}
			appendParameters(buffer);
			_string = buffer.toString();
		}
		return _string;
	}
	
	public StringBuilder getValueBuffer()
	{
		if (isWildcard())
			return new StringBuilder("*");
		
		StringBuilder buffer = new StringBuilder(64);
		if (_displayName != null)
			buffer.append(StringUtil.quoteIfNeeded(_displayName, DISPLAY_NAME_BS)).append(' ');
		
		buffer.append('<');
		buffer.append(_uri.toString());
		buffer.append('>');
		return buffer;
	}
	
	@Override
	public Address clone()
	{
		try
		{
			AddressImpl clone =  (AddressImpl) super.clone();
			if (_uri != null)
				clone._uri = _uri.clone();
			return clone;
		}
		catch (CloneNotSupportedException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public boolean equals(Object o) 
	{
		if (o == null || !(o instanceof Address)) 
			return false;
		
		Address other = (Address) o;
		
		if (isWildcard())
			return other.isWildcard();
		if (other.isWildcard())
			return isWildcard();
		
		if (!_uri.equals(other.getURI()))
			return false;
		
		if (!StringUtil.equals(_tag, other.getParameter("tag")))
			return false;
		
		// Use super.getParameters() as tag has already been checked
		for (Entry<String, String> entry : super.getParameters()) 
		{
			String otherValue = other.getParameter(entry.getKey()); 
			if (otherValue != null && !entry.getValue().equals(otherValue))
				return false;
		}
		return true;
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException
	{
		out.writeUTF(toString());
		// Write also URI to ensure if URI is also serialize in other object, the same URI instance will be used.
		out.writeObject(_uri);
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		try
		{
			_string = in.readUTF();
			parse();
			_uri = (URI) in.readObject();
		}
		catch (ParseException e)
		{
			throw new IOException(e);
		}
	}
	
	class TagIterator implements Iterator<String>
	{
		private Iterator<String> _it;
		private boolean _tagRead = false;
		private boolean _tagReading = true;
		
		public TagIterator(Iterator<String> it)
		{
			_it = it;
		}
		
		@Override
		public boolean hasNext()
		{
			return _it.hasNext() || !_tagRead;
		}

		@Override
		public String next()
		{
			if (!_tagRead)
			{
				_tagRead = true;
				return "tag";
			}
			_tagReading = false;
			return _it.next();
		}

		@Override
		public void remove()
		{
			if (_tagReading)
				_tag = null;
			else
				_it.remove();
		}
		
	}
}
