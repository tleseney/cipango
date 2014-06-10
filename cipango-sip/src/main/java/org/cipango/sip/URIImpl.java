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

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.servlet.sip.URI;

import org.cipango.util.StringUtil;


public class URIImpl implements URI, Serializable, Modifiable 
{	
	private static final long serialVersionUID = 1l;
	private static final BitSet ALPHA_BS = StringUtil.toBitSet(StringUtil.ALPHA);
	private static final BitSet SCHEME_BS = StringUtil.toBitSet(StringUtil.ALPHA + StringUtil.DIGITS + "+-.");

		
	private String _uri;
	private String _scheme;
	private String _file;
	private HashMap<String, String> _params = new HashMap<String, String>();
	private transient boolean _modified;
	
	protected URIImpl() { }
	
	public URIImpl(String uri) throws ParseException 
	{
		_uri = uri;
		parse();
		_modified = false;
	}
	
	private void parse() throws ParseException 
	{
		int indexScheme = _uri.indexOf(':');
		if (indexScheme < 0) 
			throw new ParseException("Missing scheme in uri [" + _uri + "]", 0);
		
		_scheme = _uri.substring(0, indexScheme);
		if (!isURIScheme(_scheme))
			throw new ParseException("Invalid scheme [" + _scheme + "] in uri [" + _uri + "]", 0);
		
		int indexParam = _uri.indexOf(';', indexScheme);
		if (indexParam < 0) 
		{
			_file = _uri.substring(indexScheme + 1);
		} 
		else 
		{
			_file = _uri.substring(indexScheme + 1, indexParam);
			String sParams = _uri.substring(indexParam + 1);
			parseParams(sParams);
		}
	}
	
	public static final boolean isURIScheme(String s)
	{
		if (s == null || s.length() == 0)
			return false;
		if (!ALPHA_BS.get(s.charAt(0))) return false;
		
		if (!StringUtil.contains(s, SCHEME_BS)) return false;
		return true;
	}
	
	
	private void parseParams(String sParams) throws ParseException 
	{	
		StringTokenizer st = new StringTokenizer(sParams, ";");
		while (st.hasMoreTokens()) 
		{
			String param = st.nextToken();
			String name;
			String value;
			int index = param.indexOf('=');
			
			if (index < 0) 
			{
				name  = param.trim();
				value = "";
			} 
			else 
			{
				name  = param.substring(0, index).trim();
				value = param.substring(index + 1).trim();
			}
// FIXME	
//			if (!StringUtil.PARAM_BS.containsAll(name)) 
//			{
//				throw new ServletParseException("Invalid parameter name [" 
//						+ name + "] in [" + _uri + "]");
//			}
//			if (!StringUtil.PARAM_BS.containsAll(value)) 
//			{
//				throw new ServletParseException("Invalid parameter value [" 
//						+ value + "] in [" + _uri + "]");
//			}			
			_params.put(StringUtil.unescape(name.toLowerCase()), StringUtil.unescape(value));
		}
	}
	
	public boolean isSipURI() 
	{
		return false;
	}
	
	public String getScheme() 
	{
		return _scheme;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof URI))
			return false;
		URI uri = (URI) o;
		if (!_scheme.equals(uri.getScheme()))
			return false;
		
		// FIXME improve equals
		if (!toString().equalsIgnoreCase(uri.toString()))
			return false;
		
		return true;
			
	}
	
	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}
	
	@Override
	public URI clone() 
	{
		try 
		{
			URIImpl uri = (URIImpl) super.clone();
			if (_params != null)
				uri._params = (HashMap<String, String>) _params.clone();
			return uri;
		} 
		catch (CloneNotSupportedException _) 
		{
			throw new RuntimeException();
		}
	}
	
	public String toString()
	{
		if (_uri != null)
			return _uri;
		StringBuffer sb = new StringBuffer();
		sb.append(_scheme).append(":");
		sb.append(_file);
		
		Iterator<String> it = getParameterNames();
		while (it.hasNext()) 
		{
			String name = (String) it.next();
			String value = getParameter(name);
			sb.append(';');
			sb.append(StringUtil.encode(name, StringUtil.PARAM_BS));
			if (value != null && value.length() > 0) 
			{
				sb.append('=');
				sb.append(StringUtil.encode(value, StringUtil.PARAM_BS));
			}
		}
		return sb.toString();
	}

	public String getParameter(String name) 
	{
		return _params.get(name.toLowerCase());
	}
	
	public void removeParameter(String name)
	{
		_uri = null;
		_params.remove(name);	
		_modified = true;
	}
	
	public void setParameter(String name, String value)
	{
		if (name == null || value == null)
			throw new NullPointerException("Null value or name");
		_uri = null;
		_params.put(name, value);
		_modified = true;
	}
	
	public synchronized Iterator<String> getParameterNames() 
	{
		return _params.keySet().iterator();
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException
	{
		out.writeUTF(toString());
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		try
		{
			_params = new HashMap<String, String>();
			_uri = in.readUTF();
			parse();
		}
		catch (ParseException e)
		{
			throw new IOException(e);
		}
	}

	@Override
	public boolean hasBeenModified()
	{
		return _modified;
	}

}
