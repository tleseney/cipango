//========================================================================
//Copyright 2008-2012 NEXCOM Systems
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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.jetty.util.StringMap;

/**
* Parser for WWW-Authenticate or Proxy-Authenticate headers
* @author nicolas
*
*/
public class Authenticate
{
	public enum Param
	{ 
		REALM("realm"),
		DOMAIN("domain"),
		NONCE("nonce"),
		OPAQUE("opaque"),
		STALE("stale"),
		ALGORITHM("algorithm"),
		QOP("qop");

		private String _string;
		Param(String s) { _string = s; }
		
		@Override public String toString() { return _string; }
	}
	
	static StringMap<Param> CACHE = new StringMap<Param>(true);
	
	static 
	{
		for (Param p : Param.values())
		{
			CACHE.put(p.toString(), p);
		}
	}

	private EnumMap<Param, String> _params = new EnumMap<Param, String>(Param.class);
	private HashMap<String, String> _unknownParams;

	private String _scheme;

	public Authenticate(String auth)
	{
		// TODO port to StringScanner usage?
		int beginIndex = auth.indexOf(' ');
		int endIndex;
		_scheme = auth.substring(0, beginIndex).trim();
		while (beginIndex > 0)
		{
			endIndex = auth.indexOf('=', beginIndex);
			String name = auth.substring(beginIndex, endIndex).trim();
			if (auth.charAt(endIndex + 1) == '"')
			{
				beginIndex = endIndex + 2;
				endIndex = auth.indexOf('"', beginIndex);
			}
			else
			{
				beginIndex = endIndex + 1;
				endIndex = auth.indexOf(',', beginIndex);
				if (endIndex == -1)
					endIndex = auth.length(); 
			}

			String value = auth.substring(beginIndex, endIndex);	
			setParameter(name, value);
			beginIndex = auth.indexOf(',', endIndex) + 1;
		}
	}
	
	public Authenticate(String scheme, String realm, String nonce, boolean stale, String algorithm)
	{
		_scheme = scheme;
		_params.put(Param.REALM, realm);
		_params.put(Param.NONCE, nonce);
		_params.put(Param.ALGORITHM, algorithm);
		if (stale)
			_params.put(Param.STALE, "true");
	}
	
	public String getParameter(String name)
	{
		Param param = CACHE.get(name);
		if (param != null)
			return _params.get(param);
		else
		{
			if (_unknownParams != null)
			return _unknownParams.get(name);
		}
		return null;
	}
	
	public void setParameter(String name, String value)
	{
		Param param = CACHE.get(name);
		if (param != null)
			_params.put(param, value);
		else
		{
			if (_unknownParams == null)
				_unknownParams = new HashMap<String, String>();
			_unknownParams.put(name, value);
		}
	}
	
	public String getAlgorithm()
	{
		return _params.get(Param.ALGORITHM);
	}

	public String getDomain()
	{
		return _params.get(Param.DOMAIN);
	}

	public String getNonce()
	{
		return _params.get(Param.NONCE);
	}

	public String getOpaque()
	{
		return _params.get(Param.OPAQUE);
	}

	public String getQop()
	{
		return _params.get(Param.QOP);
	}

	public String getRealm()
	{
		return _params.get(Param.REALM);
	}

	public String getScheme()
	{
		return _scheme;
	}
	
	public boolean isStale()
	{
		return "true".equalsIgnoreCase(_params.get(Param.STALE));
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(getScheme()).append(' ');
		boolean first = true;
		for (Param p : Param.values())
		{
			String value = _params.get(p);
			if (value != null)
			{
				if (!first)
					sb.append(',');
				else
					first = false;
				sb.append(p.toString()).append("=\"").append(value).append('"');
			}
		}
		if (_unknownParams != null)
		{
			Iterator<String> it = _unknownParams.keySet().iterator();
			while (it.hasNext())
			{
				String key = it.next();
				sb.append(',');
				sb.append(key).append("=\"").append(_unknownParams.get(key)).append('"');
			}
		}
		return sb.toString();
	}
}
