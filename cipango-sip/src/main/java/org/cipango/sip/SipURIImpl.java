package org.cipango.sip;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.sip.SipURI;

import org.cipango.util.StringUtil;
import org.eclipse.jetty.util.StringMap;
import org.eclipse.jetty.util.UrlEncoded;

public class SipURIImpl implements SipURI, Serializable
{
	private static final long serialVersionUID = 1L;
	public static final String SIP_SCHEME = "sip:";
	public static final String SIPS_SCHEME = "sips:";
	
	enum Param 
	{ 
		TRANSPORT, TTL, MADDR, METHOD, USER, LR;
	
		private String _string;
		
		Param()
		{
			_string = name().toLowerCase();
		}
		
		@Override
		public String toString()
		{
			return _string;
		}
		
		public String asString()
		{
			return _string;
		}
	}
	
	enum State { USER, PASSWORD, HOST, PORT, PARAMETERS, HEADERS }
	enum Host { IPV4, NAME, IPV6 }

	private static final StringMap<Param> CACHE = new StringMap<Param>(true);
	
	static 
	{
		for (Param param : Param.values())
		{
			CACHE.put(param.toString(), param);
		}
	}
	
	private boolean _secure = false;

	private String _user;
	private String _password;
	
	private String _host;
	private int _port = -1;
		
	private EnumMap<Param, String> _params;
	private Map<String, String> _otherParameters;
	
	private Map<String, String> _headers;
	
	public SipURIImpl(String user, String host, int port)
	{
		_user = user;
		_host = host;
		_port = port;
	}
	
	public SipURIImpl(String host, int port)
	{
		_host = host;
		_port = port;
	}
	
	public SipURIImpl(String uri) throws ParseException
	{
		parse(uri);
	}
	
	public SipURIImpl() { }
	
	public boolean isSipURI() 
	{
		return true;
	}

	public String getScheme() 
	{
		return _secure ? SipScheme.SIPS.toString() : SipScheme.SIP.toString();
	}
	
	public boolean isSecure()
	{
		return _secure;
	}
	
	public void setSecure(boolean b)
	{
		_secure = b;
	}
	
	public String getUser()
	{
		return _user;
	}
	
	public String getHost()
	{
		return _host;
	}
	
	public int getPort()
	{
		return _port;
	}
	
	public void setPort(int port)
	{
		if (port < 0)
			port = -1;
		_port = port;
	}
	
	public String getUserPassword()
	{
		return _password;
	}
	
	public void setUserPassword(String password)
	{
		_password = password;
	}
	
	public void parse(String uri) throws ParseException
	{
		int i = 0;
		int e = uri.length();
		
		if (uri.startsWith("sip:"))
			i = 4;
		else if (uri.startsWith("sips:"))
		{
			_secure = true;
			i = 5;
		}
		else
			throw new ParseException("invalid scheme", 0);
		
		State state;
		int port = -1;
		
		int m = i;
		boolean encoded = false;
		
		String key = null;
		String value = null;
		boolean ipv6Host = false;
		
		if (uri.indexOf('@') != -1)
			state = State.USER;
		else
			state = State.HOST;
		
		for (;; i++)
		{
			int c = (i < e) ? uri.charAt(i) : -1;
			
			switch (state)
			{
			case USER:
			{	
				switch (c)
				{
					case ':':
						if (i-m>0)
							_user = encoded ? UrlEncoded.decodeString(uri, m, i-m, null) : uri.substring(m, i);
						m = i+1; encoded = false;
						state = State.PASSWORD;
						break;
					case '@':
						if (i-m>0)
							_user = encoded ? UrlEncoded.decodeString(uri, m, i-m, null) : uri.substring(m, i);
						m = i+1; encoded = false;
						state = State.HOST;
						break;
					case '%':
						encoded = true;
						break;
					case -1:
						throw new ParseException("missing host", i);
				}
				break;
			}
			case PASSWORD:
				if (c == '@')
				{
					if (i-m>0)
						_password = encoded ? UrlEncoded.decodeString(uri, m, i-m, null) : uri.substring(m, i);
					m = i+1; encoded = false;
					state = State.HOST;
				}
				else if (c == '%')
				{
					encoded = true;
				}
				else if (c == -1)
					throw new ParseException("missing host", i);
				
				break;
			case HOST:
				if (c == ':' && !ipv6Host)
				{
					if (i-m>0)
						_host = encoded ? UrlEncoded.decodeString(uri, m, i-m, null) : uri.substring(m, i);
					else
						throw new ParseException("missing host", i);
					
					m = i+1; encoded = false;
					state = State.PORT;
				}
				else if (c == ';')
				{
					if (i-m>0)
						_host = encoded ? UrlEncoded.decodeString(uri, m, i-m, null) : uri.substring(m, i);
					else
						throw new ParseException("missing host", i);
					m = i; encoded = false;
					state = State.PARAMETERS;
				}
				else if (c == '?')
				{
					if (i-m>0)
						_host = encoded ? UrlEncoded.decodeString(uri, m, i-m, null) : uri.substring(m, i);
					else
						throw new ParseException("missing host", i);
					m = i; encoded = false;
					state = State.HEADERS;
				}
				else if (c == -1)
				{
					if (i-m>0)
						_host = encoded ? UrlEncoded.decodeString(uri, m, i-m, null) : uri.substring(m, i);
					else
						throw new ParseException("missing host", i);
					return;
				}
				else if (c == '[')
				{
					ipv6Host = true;
				}
				else if (c == ']')
				{
					if (!ipv6Host)
						throw new ParseException("invalid host. Got ']' without '['", i);
					ipv6Host = false;
				}
			
				break;
				
			case PORT:
				if (Character.isDigit(c))
				{
					if (port == -1) port = 0;
					port = port*10 + (c-'0');

					if (port > 65536)
						throw new ParseException("invalid port. Got " + port, i);
				}
				else
				{
					if (port != -1)
						_port = port;
					if (c == ';')
					{
						m = i;
						state = State.PARAMETERS;
					}
					else if (c == '?')
					{
						m = i;
						state = State.HEADERS;
					}
					else if (c == -1)
					{
						return;
					}
				}	
				break;
			
			case PARAMETERS:
				switch (c)
				{
				case '?':
					state = State.HEADERS;
				case ';':
					value = i-m-1 == 0 ? "" : 
						(encoded ? UrlEncoded.decodeString(uri, m+1, i-m-1, null) : uri.substring(m+1,i));
					encoded = false;
					m = i;
					if (key != null)
						setParameter(key, value);
					else if (value != null && value.length() > 0)
						setParameter(value, "");
					
					key = value = null;
					break;
				case '=':
					if (key != null)
						break;
					key = encoded ? UrlEncoded.decodeString(uri, m+1, i-m-1, null) : uri.substring(m+1,i);
					encoded = false;
					m = i;
					break;
				case '%':
					encoded = true;
					break;
				case -1:
					if (key != null)
					{
						value = i-m-1 == 0 ? "" : 
							(encoded ? UrlEncoded.decodeString(uri, m+1, i-m-1, null) : uri.substring(m+1));
						setParameter(key, value);
					}
					else if (m < i)
					{
						key = encoded ? UrlEncoded.decodeString(uri, m+1, i-m-1, null) : uri.substring(m+1);
						if (key != null && key.length() > 0)
							setParameter(key, "");
					}
					return;
				}
			
				break;
			
			case HEADERS:
				switch (c)
				{
				case ';':
					state = State.PARAMETERS;
				case '&':
					value = i-m-1 == 0 ? "" : 
						(encoded ? UrlEncoded.decodeString(uri, m+1, i-m-1, null) : uri.substring(m+1,i));
					encoded = false;
					m = i;
					if (key != null && value != null)
						setHeader(key, value);
					
					key = value = null;
					break;
				case '=':
					if (key != null)
						break;
					key = encoded ? UrlEncoded.decodeString(uri, m+1, i-m-1, null) : uri.substring(m+1,i);
					encoded = false;
					m = i;
					break;
				case '%':
					encoded = true;
					break;
				case -1:
					if (key != null)
					{
						value = i-m-1 == 0 ? "" : 
							(encoded ? UrlEncoded.decodeString(uri, m+1, i-m-1, null) : uri.substring(m+1));
						setHeader(key, value);
					}
					return;
				}
			
				break;
			}
		}		
	}
	
	private String getParameter(Param name)
	{
		if (_params == null)
			return null;
		
		return _params.get(name);
	}
	
	private synchronized void setParameter(Param name, String value)
	{
		if (_params == null)
			_params = new EnumMap<Param, String>(Param.class);
		_params.put(name, value);
	}
	
	private void removeParameter(Param name)
	{
		if (_params != null)
			_params.remove(name);
	}
	
	public String getParameter(String name) 
	{
		Param param = (Param) CACHE.get(name);
		if (param != null)
			return getParameter(param);
		else 
			return _otherParameters == null ? null : _otherParameters.get(name);
	}

	public Iterator<String> getParameterNames() 
	{
		List<String> list = new ArrayList<String>();
		if (_params != null)
		{
			for (Param param : _params.keySet())
			{
				list.add(param.toString());
			}
		}
		if (_otherParameters != null)
		{
			list.addAll(_otherParameters.keySet());
		}
		return list.iterator();
	}

	public void removeParameter(String name)
	{
		Param param = (Param) CACHE.get(name);
		if (param != null)
			removeParameter(param);
		else if (_otherParameters != null)
			_otherParameters.remove(name);
	}

	public void setParameter(String name, String value) 
	{
		Param param = (Param) CACHE.get(name);
		if (param != null)
			setParameter(param, value);	
		else
		{
			if (_otherParameters == null)
				_otherParameters = new HashMap<String, String>();
			_otherParameters.put(name, value);
		}
	}
	
	public boolean getLrParam() 
	{
		return "".equals(getParameter(Param.LR));
	}

	public String getMAddrParam() 
	{
		return getParameter(Param.MADDR);
	}

	public String getMethodParam() 
	{
		return getParameter(Param.METHOD);
	}

	public int getTTLParam() 
	{
		String s = getParameter(Param.TTL);
		return (s != null) ? Integer.parseInt(s) : -1;
	}

	public String getTransportParam() 
	{
		return getParameter(Param.TRANSPORT);
	}
	
	public void setHost(String host) 
	{
		if (host.contains(":") && !host.contains("["))
    		_host = "[" + host + "]";
    	else
            _host = host;
	}
	

	public void setUser(String user) 
	{
		_user = user;
	}

	public void setLrParam(boolean b) 
	{
		if (b)
			setParameter(Param.LR, "");
		else
			removeParameter(Param.LR);		
	}
	
	public String getUserParam() 
	{
		return getParameter(Param.USER);
	}

	public void removeHeader(String name) 
	{
		if (_headers != null)
			_headers.remove(name);
	}

	public void setHeader(String name, String value) 
	{
		if (_headers == null)
			_headers = new HashMap<String, String>();
		_headers.put(name, value);		
	}

	public void setMAddrParam(String maddr) 
	{
		setParameter(Param.MADDR, maddr);		
	}

	public void setMethodParam(String method) 
	{
		setParameter(Param.METHOD, method);		
	}

	public void setTTLParam(int ttl) 
	{
		setParameter(Param.TTL, Integer.toString(ttl));		
	}

	public void setTransportParam(String transport) 
	{
		setParameter(Param.TRANSPORT, transport);
	}

	public void setUserParam(String user) 
	{
		setParameter(Param.USER, user);
	}
	
	public String getHeader(String name) 
	{
		return _headers != null ? _headers.get(name) : null;
	}

	public Iterator<String> getHeaderNames() 
	{
		if (_headers == null)
			return Collections.emptyIterator();
		
		return new ArrayList<String>(_headers.keySet()).iterator();
	}

	@Override 
	public String toString() // TODO escape
	{
		StringBuilder buffer = new StringBuilder(32);
		
		buffer.append(_secure ? SIPS_SCHEME : SIP_SCHEME);
		if (_user != null)
		{
			buffer.append(_user);
			if (_password != null)
			{
				buffer.append(':');
				buffer.append(_password);
			}
			buffer.append('@');
		}
		buffer.append(_host);
		if (_port > 0)
		{
			buffer.append(':'); 
			buffer.append(_port);
		}
		
		if (_params != null && _params.size() > 0)
		{
			for (Param p : Param.values())
			{
				if (_params.containsKey(p))
				{
					String value = _params.get(p);
					buffer.append(';');
					buffer.append(p.asString());
					if (value != null && !value.equals(""))
					{
						buffer.append('=');
						buffer.append(StringUtil.encode(value, StringUtil.PARAM_BS));
					}
				}
			}
		}
		
		if (_otherParameters != null)
		{
			for (String name : _otherParameters.keySet())
			{
				String value = _otherParameters.get(name);
				buffer.append(';');
				buffer.append(StringUtil.encode(name, StringUtil.PARAM_BS));
				if (value != null && !value.equals(""))
				{
					buffer.append('=');
					buffer.append(StringUtil.encode(value, StringUtil.PARAM_BS));
				}
			}
		}
		
		Iterator<String> it2 = getHeaderNames();
		boolean first = true;
		while (it2.hasNext()) 
		{
			String name = it2.next();
			String value = getHeader(name);
			if (first) 
			{
				first = false;
				buffer.append('?');
			} 
			else 
			{
				buffer.append('&');
			}
			buffer.append(StringUtil.encode(name, StringUtil.HEADER_BS));
			buffer.append('=');
			buffer.append(StringUtil.encode(value, StringUtil.HEADER_BS));
		}
		
		return buffer.toString();
	}
	
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof SipURI))
			return false;
		
		SipURI uri = (SipURI) o;
		if (_secure != uri.isSecure())
			return false;
		
		if (!StringUtil.equals(_user, uri.getUser()))
			return false;
		
		if (!StringUtil.equals(_password, uri.getUserPassword()))
			return false;
		
		if (!_host.equalsIgnoreCase(uri.getHost()))
			return false;
		
		if (_port != uri.getPort())
			return false;
		
		for (Param param : Param.values())
		{
			if (param == Param.LR)
			{
				if (getParameter(Param.LR) != null && uri.getParameter(Param.LR.asString()) != null &&
					getLrParam() != uri.getLrParam())
					return false;
			}
			else
			{	
				if (!StringUtil.equalsIgnoreCase(getParameter(param), uri.getParameter(param.asString())))
					return false;
			}
		}
		
		if (_otherParameters != null)
		{
			Iterator<String> parameters = uri.getParameterNames();
			while (parameters.hasNext())
			{
				String name = parameters.next();
				if (_otherParameters.containsKey(name) 
						&& !StringUtil.equalsIgnoreCase(_otherParameters.get(name), uri.getParameter(name)))
						return false;
			}
		}
		
		Iterator<String> headers = uri.getHeaderNames();
		if (headers.hasNext())
		{
			if (_headers == null)
				return false;
			
			int i = 0;
			while (headers.hasNext())
			{
				String name = headers.next();
				if (!uri.getHeader(name).equals(_headers.get(name)))
					return false;
				i++;
			}
			if (i != _headers.size())
				return false;
		}
		else
		{
			if (_headers != null && _headers.size() > 0)
				return false;
		}
		
		return true;
	}
	
	
	
	public SipURIImpl clone() 
	{
		try
		{
			SipURIImpl clone = (SipURIImpl) super.clone();
			if (_params != null)
				clone._params = _params.clone();
			if (_otherParameters != null)
				clone._otherParameters = new HashMap<String, String>(_otherParameters);
			if (_headers != null)
				clone._headers = new HashMap<String, String>(_headers);
			
			return clone;
		}
		catch (CloneNotSupportedException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException
	{
		out.writeUTF(toString());
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		try
		{
			_port = -1;
			parse(in.readUTF());
		}
		catch (ParseException e)
		{
			throw new IOException(e);
		}
	}

}
