package org.cipango.sip;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.sip.SipURI;

import org.eclipse.jetty.util.StringMap;
import org.eclipse.jetty.util.TypeUtil;
import org.eclipse.jetty.util.UrlEncoded;

public class SipURIImpl implements SipURI
{
	enum Param { TRANSPORT, TTL, MADDR, METHOD, USER, LR }
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
				continue;
			}
			case PASSWORD:
			{
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
				continue;
			}
			case HOST:
			{
				if (c == ':')
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
				else if (c == -1)
				{
					if (i-m>0)
						_host = encoded ? UrlEncoded.decodeString(uri, m, i-m, null) : uri.substring(m, i);
					else
						throw new ParseException("missing host", i);
					return;
				}
			}
			case PORT:
			{
				if (Character.isDigit(c))
				{
					if (port == -1) port = 0;
					port = port*10 + (c-'0');
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
					else if (c == -1)
					{
						return;
					}
				}	
				continue;
			}
			case PARAMETERS:
				switch (c)
				{
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
			}
		}		
		//System.out.println(uri + " user=" + user + ",passwd=" + passwd + ",host=" + host + ",port=" + port)
	}
	
	
	
	public static void main(String[] args) throws Exception
	{
		String s= "Fran%c3%a7ois";
		
		
		System.out.println(UrlEncoded.decodeString(s, 0, s.length(), null));
		/*
		InetAddress addr = InetAddress.getByName("2001:0db8:0000:85a3:0000:0000:ac1f:8001");
		System.out.println(addr);
		SipURIImpl suri = new SipURIImpl();
		suri.parse3("sip:alice@atlanta.com");
		System.out.println(suri.getUser());
		System.out.println(suri.getHost());
		System.out.println(suri.getPort());
		
		suri.setTransportParam("tcp");
		System.out.println(suri.getTransportParam());
		System.out.println(suri.getParameter("transport"));
			
		long start = System.currentTimeMillis();
		
		for (int i = 0; i < 1000000; i++)
		{
			SipURIImpl uri = new SipURIImpl();
			uri.parse("sip:alice@atlanta.com");
		}
		
		System.out.println(System.currentTimeMillis() - start);
		
	 start = System.currentTimeMillis();
		
	 
	 	int n = 1000000;
	 
		for (int i = 0; i < n; i++)
		{
			SipURIImpl uri = new SipURIImpl();
			uri.parse3("sip:atlanta.com");
		}
		
		System.out.println(System.currentTimeMillis() - start);
		
		String s = "sip:alice@atlanta.com:5060;lr";
		
		start = System.currentTimeMillis();
		
		for (int i = 0; i < n; i++)
		{
			SipURIImpl uri = new SipURIImpl();
			uri.parse3(s);
		}
		
		System.out.println("parse3: " + (System.currentTimeMillis() - start));
		
		start = System.currentTimeMillis();
		
		for (int i = 0; i < n; i++)
		{
			SipURIImpl uri = new SipURIImpl();
			uri.parse(s);
		}
		
		System.out.println("parse: " + (System.currentTimeMillis() - start));
		
		start = System.currentTimeMillis();
		
		for (int i = 0; i < 1000000; i++)
		{
			SipURIImpl3 uri = new SipURIImpl3(s);
			uri.getHost();
		}
		
		System.out.println("parse: " + (System.currentTimeMillis() - start));
		*/
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
		return list.iterator();
	}

	public void removeParameter(String name)
	{
		
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

	@Override
	public void removeHeader(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setHeader(String arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMAddrParam(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMethodParam(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTTLParam(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTransportParam(String transport) 
	{
		setParameter(Param.TRANSPORT, transport);
	}

	@Override
	public void setUserParam(String arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public String getHeader(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<String> getHeaderNames() {
		// TODO Auto-generated method stub
		return null;
	}

	
	
	public SipURIImpl clone() 
	{
		try
		{
			return (SipURIImpl) super.clone();
		} 
		catch (CloneNotSupportedException e)
		{
			throw new RuntimeException(e);
		}
	}
}
