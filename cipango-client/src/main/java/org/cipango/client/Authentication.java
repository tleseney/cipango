// ========================================================================
// Copyright 2011 NEXCOM Systems
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

package org.cipango.client;

import java.security.MessageDigest;
import java.util.Random;

import javax.servlet.ServletException;

import org.eclipse.jetty.util.QuotedStringTokenizer;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.TypeUtil;

public class Authentication 
{
	private Digest _digest;
	private Random _random;
	private int _nc = 1;
	
	public Authentication(Digest digest) throws ServletException
	{
		_digest = digest;
	}
	
	public Digest getDigest()
	{
		return _digest;
	}
	
	public String authorize(String method, String uri, Credentials credentials) throws ServletException
	{
		String cnonce = null;
		
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			
			md.update(credentials.getUser().getBytes(StringUtil.__ISO_8859_1));
			md.update((byte) ':');
			md.update(_digest._realm.getBytes(StringUtil.__ISO_8859_1));
			md.update((byte) ':');
			md.update(credentials.getPassword().getBytes(StringUtil.__ISO_8859_1));
			byte[] ha1 = md.digest();
						
			md.reset();
			md.update(method.getBytes(StringUtil.__ISO_8859_1));
			md.update((byte) ':');
			md.update(uri.getBytes(StringUtil.__ISO_8859_1));
			byte[] ha2 = md.digest();
			
			md.reset();
			md.update(TypeUtil.toString(ha1, 16).getBytes(StringUtil.__ISO_8859_1));
			md.update((byte) ':');
			md.update(_digest._nonce.getBytes(StringUtil.__ISO_8859_1));
			
			md.update((byte) ':');
			if (_digest._qop != null)
			{
				_digest._qop = "auth";
				
				byte[] b = new byte[4];
				if (_random == null)
					_random = new Random();
				_random.nextBytes(b);
				
				cnonce = TypeUtil.toString(b, 16); 
				
				md.update(toHex8(_nc++).getBytes(StringUtil.__ISO_8859_1));
				
				md.update((byte) ':');
				md.update(cnonce.getBytes(StringUtil.__ISO_8859_1));
				md.update((byte) ':');
				md.update(_digest._qop.getBytes(StringUtil.__ISO_8859_1));
				md.update((byte) ':');
			}
			md.update(TypeUtil.toString(ha2, 16).getBytes(StringUtil.__ISO_8859_1));
			
			byte[] response = md.digest();
			
			String authorize = "Digest username=\"" + credentials.getUser() 
				+ "\", nonce=\"" + _digest._nonce
				+ "\", realm=\"" + _digest._realm
				+ "\", uri=\"" + uri.toString();
			
			if (cnonce != null)
			{
				authorize += "\", qop=" + _digest._qop 
				+ ", nc=" + toHex8(_nc-1)
				+ ", cnonce=\"" + cnonce;
			}
			
			authorize += "\", response=\"" + TypeUtil.toString(response, 16);
			if (_digest._opaque != null)
			{
				authorize += "\", opaque=\"" + _digest._opaque;
			}
			authorize += "\"";
			
			return authorize;	
		}
		catch (Exception e)
		{
			throw new ServletException("Failed to authorize " + _digest, e);
		}
	}
	
	private static final String __padding[] = { "0000000", "000000", "00000", "0000", "000", "00", "0" };
	
	public static String toHex8(int i)
	{
		String s = Integer.toHexString(i);
		int l = s.length();
		if (l < 8)
		{
			s = __padding[l-1] + s;
		}
		return s;
	}
	
	public static Digest createDigest(String authenticate) throws ServletException
	{
		Digest digest = new Digest();
		
		try
		{
			QuotedStringTokenizer tokenizer = new QuotedStringTokenizer(authenticate, "=, ", true, false);
			String last = null;
			String name = null;
			
			while (tokenizer.hasMoreTokens())
			{
				String tok = tokenizer.nextToken();
				char c = (tok.length() ==1) ? tok.charAt(0) : '\0';
				
				switch (c)
				{
				case '=':
					name = last;
					last = tok;
					break;
				case ',':
					name = null;
					break;
				case ' ':
					break;
				
				default:
					last = tok;
					if (name != null)
					{
						if ("realm".equalsIgnoreCase(name))
							digest._realm = tok;
						else if ("qop".equalsIgnoreCase(name))
							digest._qop = tok;
						else if ("nonce".equalsIgnoreCase(name))
							digest._nonce = tok;
						else if ("opaque".equalsIgnoreCase(name))
							digest._opaque = tok;
						else if ("stale".equalsIgnoreCase(name))
							digest._stale = tok;
						
						name = null;
					}
				}
			}
			return digest;
		}
		catch (Exception e)
		{
			throw new ServletException("Failed to parse digest: " + authenticate, e);
		}
	}
	
	static class Digest
	{
		private String _realm;
		private String _qop;
		private String _nonce;
		private String _opaque;
		private String _stale;
		
		public String getRealm()
		{
			return _realm;
		}

		public String getQop()
		{
			return _qop;
		}

		public String getOpaque()
		{
			return _opaque;
		}
		
		public boolean isStale()
		{
			return Boolean.parseBoolean(_stale);
		}
		
		public String getNonce()
		{
			return _nonce;
		}
	}
}
