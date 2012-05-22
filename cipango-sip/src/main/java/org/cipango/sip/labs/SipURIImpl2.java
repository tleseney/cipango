package org.cipango.sip.labs;

import java.net.InetAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.sip.SipURI;

import org.cipango.sip.SipScheme;
import org.eclipse.jetty.util.StringMap;
import org.eclipse.jetty.util.TypeUtil;
import org.eclipse.jetty.util.UrlEncoded;

public class SipURIImpl2 implements SipURI
{
	enum Param { TRANSPORT, TTL, MADDR, METHOD, USER, LR }
	enum State { USER, PASSWORD, HOST, PORT, PARAMETERS, HEADERS }
	enum Host { IPV4, NAME, IPV6 }

	private static final StringMap CACHE = new StringMap(true);
	
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
	
	public SipURIImpl2(String uri) throws ParseException
	{
		parse(uri);
	}
	
	public SipURIImpl2() { }
	

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
		
		int user, passwd, host, port;
		int params = e, headers = e;
		
		if (uri.indexOf('@') != -1)
		{
			state = State.USER;
			user = passwd = i;
			host = port = e;
		}
		else
		{
			state = State.HOST;
			host = i;
			user = passwd = port = e;
		}
		
		while (i < e)
		{
			int c = uri.charAt(i);
			int s = i++;
			
			switch (state)
			{
			case USER:
			{	
				switch (c)
				{
					case ':':
						passwd = s;
						state = State.PASSWORD;
						break;
					case '@':
						passwd = s;
						host = i;
						state = State.HOST;
						break;
				}
				continue;
			}
			case PASSWORD:
			{
				if (c == '@')
				{
					host = i;
					state = State.HOST;
				}
				continue;
			}
			case HOST:
			{
				if (c == ':')
				{
					state = State.PORT;
					port = s;
				}
				else if (c == ';')
				{
					params = s;
					state = State.PARAMETERS;
				}
				continue;
			}
			case PORT:
			{
				if (c == ';')
				{
					params = s;
					state = State.PARAMETERS;
				}
				continue;
			}
			}
		}
		
		if (user < passwd)
			_user = uri.substring(user, passwd);
		
		if (passwd < host - 2)
		{
			_password = uri.substring(passwd+1, host-1);
		}
		
		if (port > host)
			_host = uri.substring(host, port);
		else
			throw new ParseException("missing host", port);
		
		if (port < params)
			_port = TypeUtil.parseInt(uri, port+1, params-port-1, 10); 	
		
		if (params < headers)
			parseParams(uri, params+1);
	}
	
	protected void parseParams(String s, int offset)
	{
		String key = null;
		String value = null;
		int m = offset-1;
		//System.out.println("parsing " + s.substring(offset));
		
		for (int i = offset;i < s.length(); i++)
		{
			char c = s.charAt(i);
			switch (c)
			{
			case ';':
				int l = i-m-1;
				value = l == 0 ? "" : s.substring(m+1,i);
				m = i;
				if (key != null)
					System.out.println(key + "  =   " + value);
				else if (value != null && value.length() > 0)
					System.out.println(value);
				
				key = value = null;
				break;
			case '=':
				if (key != null)
					break;
				key = s.substring(m+1,i);
				m = i;
				break;
			}
		}
		if (key != null)
		{
			int l = s.length()-m-1;
			value = l == 0 ? "" : s.substring(m+1);
			System.out.println(key + "=" + value);
		}
		else if (m < s.length())
		{
			key = s.substring(m+1);
			if (key != null && key.length() > 0)
				System.out.println(key);
		}
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
		
		return null;
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

	
	
	public SipURIImpl2 clone() 
	{
		try
		{
			return (SipURIImpl2) super.clone();
		} 
		catch (CloneNotSupportedException e)
		{
			throw new RuntimeException(e);
		}
	}
}
