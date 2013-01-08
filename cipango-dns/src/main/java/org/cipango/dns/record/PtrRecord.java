// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
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
package org.cipango.dns.record;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import org.cipango.dns.Compression;
import org.cipango.dns.Name;
import org.cipango.dns.Type;

/**
 * PTR records cause no additional section processing. These RRs are used in special domains to
 * point to some other location in the domain space. These records are simple data, and don't imply
 * any special processing similar to that performed by CNAME, which identifies aliases. See the
 * description of the IN-ADDR.ARPA domain for an example.
 * 
 * <pre>
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  /                   PTRDNAME                    /
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * </pre>
 */
public class PtrRecord extends Record
{
	private Name _prtdName;

	public PtrRecord()
	{	
	}
		
	public PtrRecord(InetAddress address)
	{
		Name name;
		byte[] addr = address.getAddress();
		if (address instanceof Inet4Address)
		{
			name = new Name("in-addr.arpa");
			for (int i = 0; i < addr.length; i++)
			{
				Name tmp = name;
				name = new Name(Integer.toString(addr[i] & 0xFF));
				name.setChild(tmp);
			}
		}
		else
		{
			name = new Name("ip6.arpa");
			for (int i = 0; i < addr.length; i++)
			{
				Name tmp = name;
				name = new Name(Integer.toHexString((addr[i] >> 4) & 0xF));
				name.setChild(tmp);
				tmp = name;
				
				name = new Name(Integer.toHexString(addr[i] & 0xF));
				name.setChild(tmp);
			}
		}
		setName(getReverseName(address));
	}
	
	public static Name getReverseName(InetAddress address)
	{
		Name name;
		byte[] addr = address.getAddress();
		if (address instanceof Inet4Address)
		{
			name = new Name("in-addr.arpa");
			for (int i = 0; i < addr.length; i++)
			{
				Name tmp = name;
				name = new Name(Integer.toString(addr[i] & 0xFF));
				name.setChild(tmp);
			}
		}
		else
		{
			name = new Name("ip6.arpa");
			for (int i = 0; i < addr.length; i++)
			{
				Name tmp = name;
				name = new Name(Integer.toHexString((addr[i] >> 4) & 0xF));
				name.setChild(tmp);
				tmp = name;
				
				name = new Name(Integer.toHexString(addr[i] & 0xF));
				name.setChild(tmp);
			}
		}
		return name;
	}
	
	@Override
	public Type getType()
	{
		return Type.PTR;
	}

	@Override
	public void doEncode(ByteBuffer b, Compression c) throws IOException
	{
		c.encodeName(_prtdName, b);
	}

	@Override
	public void doDecode(ByteBuffer b, Compression c, int dataLength) throws IOException
	{
		_prtdName = c.decodeName(b);
	}

	@Override
	public String toString()
	{
		if (_prtdName == null)
			return super.toString();
		return super.toString() + " " + _prtdName;
	}

	public Name getPrtdName()
	{
		return _prtdName;
	}
	
	@Override
	public boolean doEquals(Record record)
	{
		return compare(_prtdName, ((PtrRecord) record).getPrtdName());
	}

}
