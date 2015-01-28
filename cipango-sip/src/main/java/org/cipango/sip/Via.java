// ========================================================================
// Copyright 2006-2015 NEXCOM Systems
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
import java.util.EnumMap;

import javax.servlet.sip.Parameterable;

import org.cipango.util.StringScanner;
import org.cipango.util.StringUtil;
import org.eclipse.jetty.util.ArrayTrie;
import org.eclipse.jetty.util.Trie;

public class Via extends Parameters implements Parameterable, Serializable
{

	private static final long serialVersionUID = 1L;

	enum Param
	{ 
		BRANCH("branch"), RECEIVED("received"), RPORT("rport"), MADDR("maddr"), ALIAS("alias"), COMP("comp"), KEEP("keep"), SIGCOMPID("sigcomp-id"), TTL("ttl");
	
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
	
	public static final String MAGIC_COOKIE = "z9hG4bK";
	
	private String _string;
	private String _transport;
	private String _host;
	private int _port = -1;
	
	private EnumMap<Param, String> _params= new EnumMap<Param, String>(Param.class);
	
	public Via(String via) 
	{
		_string = via;
	}
	
	public Via(String host, String transport, int port) 
	{
		_host = host;
		_transport = transport;
		_port = port;
	}
	
	public String getTransport()
	{
		return _transport;
	}
	
	public int getPort()
	{
		return _port;
	}
	
	public String getHost()
	{
		return _host;
	}
	
	public String getBranch()
	{
		return _params.get(Param.BRANCH);
	}
	
	public String getReceived()
	{
		return _params.get(Param.RECEIVED);
	}
	
	public void setReceived(String received)
	{
		_params.put(Param.RECEIVED, received);
	}
	
	public void setBranch(String branch)
	{
		_params.put(Param.BRANCH, branch);
	}
	
	public int getRPort()
	{
		String s = _params.get(Param.RPORT);
		return s != null && s.length() > 0 ? Integer.parseInt(s) : -1;
	}
	
	public boolean hasRPort()
	{
		return _params.get(Param.RPORT) != null;
	}
	
	public void setRPort(int port)
	{
		_params.put(Param.RPORT, Integer.toString(port));
	}

	public String getMAddr()
	{
		return _params.get(Param.MADDR);
	}

	public boolean hasMAddr()
	{
		return _params.get(Param.MADDR) != null;
	}

	public void setMAddr(String maddr)
	{
		_params.put(Param.MADDR, maddr);
	}

	/**
	 * @see Parameterable#setParameter(String, String)
	 */
	public void setParameter(String name, String value)
	{
		Param param = CACHE.get(name);
		if (param != null)
			_params.put(param, value);
		else
			super.setParameter(name, value);
	}
	
	@Override
	public void parameterParsed(String name, String value)
	{
		setParameter(name, value);
	}
	
	/**
	 * @see Parameterable#getParameter(String)
	 */
	public String getParameter(String name)
	{
		Param param = CACHE.get(name);
		if (param != null)
			return _params.get(param);
		else
			return super.getParameter(name);
	}
		
	private final static BitSet END_HOST = StringUtil.toBitSet(":; \t");
	
	public void parse() throws ParseException
	{	
		// sent-protocol LWS sent-by *(SEMI via-params)
		// sent-protocol = protocol-name SLASH protocol-version SLASH transport
		// sent-by = host [COLON port]
		
		StringScanner scanner = new StringScanner(_string);
		scanner.skipToChar('/').readChar('/');
		scanner.skipToChar('/').readChar('/').skipWhitespace();
		
		_transport = scanner.readToSpace();
		
		scanner.skipWhitespace();
		
		if (scanner.peekChar() == '[')
		{
			scanner.skipChar().mark().skipToChar(']'); 
			_host = scanner.readFromMark();
			scanner.readChar(']');
		}
		else
		{	
			_host = scanner.readTo(END_HOST);
		}
		
		if (!scanner.eof() && scanner.peekChar() == ':')
			_port = scanner.skipChar().readInt();
		
		scanner.skipToChar(';');
		
		parseParameters(scanner);
	}
					
	@Override
	public String getValue() 
	{
		return getValueBuffer().toString();
	}

	@Override
	public void setValue(String value) 
	{
		throw new UnsupportedOperationException();
	}
	
	public StringBuilder getValueBuffer()
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append("SIP/2.0/");
		buffer.append(_transport);
		buffer.append(' ');
		boolean ipv6Addr = _host.indexOf(':') !=-1 &&  _host.indexOf('[') == -1;
		if (ipv6Addr)
			buffer.append("[");
		buffer.append(_host);
		if (ipv6Addr)
			buffer.append("]");
		if (_port > -1)
		{
			buffer.append(':');
			buffer.append(_port);
		}
		
		return buffer;
	}
	
	public String toString()
	{
		StringBuilder buffer = getValueBuffer();
		for (Param p : Param.values())
		{
			String value = _params.get(p);
			if (value != null)
			{
				buffer.append(';');
				buffer.append(p.toString());
				if (!"".equals(value))
				{
					buffer.append('=');
					buffer.append(value);
				}
			}
		}
		appendParameters(buffer);
		
		return buffer.toString();
	}
	
	public Via clone() 
	{
		try
		{
			Via via = (Via) super.clone();
			via._params = _params.clone();
			return via;
		}
		catch (CloneNotSupportedException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void setTransport(String transport)
	{
		_transport = transport;
	}

	public void setHost(String host)
	{
		_host = host;
	}

	public void setPort(int port)
	{
		_port = port;
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
			_params = new EnumMap<Param, String>(Param.class);
			_string = in.readUTF();
			parse();
		}
		catch (ParseException e)
		{
			throw new IOException(e);
		}
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
			return false;
		if (!(obj instanceof Via))
			return false;
		
		return obj.toString().equals(toString());
	}
}
