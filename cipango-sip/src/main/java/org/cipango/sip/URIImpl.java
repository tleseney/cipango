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

import java.io.Serializable;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.servlet.sip.URI;

import org.cipango.util.StringUtil;


public class URIImpl implements URI, Serializable 
{	
	 static final long serialVersionUID = -8927516108461106171L;
	
	private String _uri;
	private String _scheme;
	private String _file;
	private HashMap<String, String> _params = new HashMap<String, String>();
	
	protected URIImpl() { }
	
	public URIImpl(String uri) throws ParseException 
	{
		_uri = uri;
		parse();
	}
	
	private void parse() throws ParseException 
	{
		int indexScheme = _uri.indexOf(':');
		if (indexScheme < 0) 
			throw new ParseException("Missing scheme in uri [" + _uri + "]", 0);
		
		_scheme = _uri.substring(0, indexScheme);
// FIXME		if (!SipGrammar.isURIScheme(_scheme))
//			throw new ServletParseException("Invalid scheme [" + _scheme + "] in uri [" + _uri + "]");
		
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
		if (!toString().equals(uri.toString()))
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
			return (URI) super.clone();
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
	}
	
	public void setParameter(String name, String value)
	{
		if (name == null || value == null)
			throw new NullPointerException("Null value or name");
		_uri = null;
		_params.put(name, value);
	}
	
	public synchronized Iterator<String> getParameterNames() 
	{
		return _params.keySet().iterator();
	}
}
