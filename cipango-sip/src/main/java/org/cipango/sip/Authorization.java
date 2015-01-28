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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;

import org.cipango.util.StringUtil;
import org.eclipse.jetty.util.ArrayTrie;
import org.eclipse.jetty.util.Trie;
import org.eclipse.jetty.util.security.Credential;

@SuppressWarnings("serial")
public class Authorization extends Credential
{
	enum Param
	{ 
		USERNAME("username"),
		REALM("realm"),
		NONCE("nonce"),
		DIGEST_URI("uri"),
		RESPONSE("response"),
		ALGORITHM("algorithm"),
		CNONCE("cnonce"),
		OPAQUE("opaque"),
		QOP("qop"),
		NONCE_COUNT("nc");

		private String _string;
		Param(String s) { _string = s; }
		
		@Override public String toString() { return _string; }
	}
	
	static Trie<Param> CACHE = new ArrayTrie<Param>();
	
	static 
	{
		for (Param p : Param.values())
		{
			CACHE.put(p.toString(), p);
		}
	}

	private static Random __random = new Random();
	
	private EnumMap<Param, String> _params = new EnumMap<Param, String>(Param.class);
	private HashMap<String, String> _unknownParams;

	private String _scheme;
	private String _method;
	
	public Authorization(Authenticate authenticate, String username, String password, String uri, String method)
	{
		_params.put(Param.USERNAME, username);
		_params.put(Param.REALM, authenticate.getRealm());
		_params.put(Param.NONCE, authenticate.getNonce());
		_params.put(Param.ALGORITHM, authenticate.getAlgorithm());
		_scheme = authenticate.getScheme();
		if (authenticate.getQop() != null)
		{
			_params.put(Param.CNONCE, newCNonce());
			StringTokenizer st = new StringTokenizer(authenticate.getQop(), ",");
			boolean first = true;
			while (st.hasMoreTokens())
			{
				String token = st.nextToken().trim();
				if (first || token.equalsIgnoreCase(DigestAuthenticator.AUTH))
				{
					_params.put(Param.QOP, token);
					first = false;
				}
			}
			_params.put(Param.NONCE_COUNT, "00000001");
		}
		_params.put(Param.OPAQUE, authenticate.getOpaque());
		_params.put(Param.DIGEST_URI, uri);
		_params.put(Param.RESPONSE, getCalculatedResponse(password, method));
	}
	
	public Authorization(String auth)
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
	
	public String getCalculatedResponse(String password, String method)
	{
		return new DigestAuthenticator().calculateResponse(this, password, method);
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

	public String getCNonce()
	{
		return _params.get(Param.CNONCE);
	}
	
	public String getNonce()
	{
		return _params.get(Param.NONCE);
	}

	public void setNonce(String nonce)
	{
		_params.put(Param.NONCE, nonce);
	}
	
	public String getNonceCount()
	{
		return _params.get(Param.NONCE_COUNT);
	}

	public String getOpaque()
	{
		return _params.get(Param.OPAQUE);
	}
	
	public void setOpaque(String opaque)
	{
		_params.put(Param.OPAQUE, opaque);
	}

	public String getQop()
	{
		return _params.get(Param.QOP);
	}

	public String getRealm()
	{
		return _params.get(Param.REALM);
	}

	public void setRealm(String realm)
	{
		_params.put(Param.REALM, realm);
	}

	public String getResponse()
	{
		return _params.get(Param.RESPONSE);
	}
	
	public String getScheme()
	{
		return _scheme;
	}
		
	public String getUri()
	{
		return _params.get(Param.DIGEST_URI);
	}
	
	public String getUsername()
	{
		return _params.get(Param.USERNAME);
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
				sb.append(p.toString());
				if (p == Param.NONCE_COUNT)
					sb.append("=").append(value);
				else	
					sb.append("=\"").append(value).append('"');
			}
		}
		return sb.toString();
	}

	@Override
	public boolean check(Object credentials)
	{
		if (credentials instanceof char[])
         credentials=new String((char[])credentials);
     String password = (credentials instanceof String) ? (String) credentials : credentials.toString();
     return getResponse().equals(getCalculatedResponse(password, _method));
	}

	public String getMethod()
	{
		return _method;
	}

	public void setMethod(String method)
	{
		_method = method;
	}
	
	private String newCNonce()
	{
		long r = __random.nextInt();
		return StringUtil.toBase62String2(r < 0 ? -r: r);
	}
}
