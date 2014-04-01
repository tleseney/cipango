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
import java.net.InetAddress;
import java.nio.ByteBuffer;

import org.cipango.dns.Compression;
import org.cipango.dns.Name;
import org.cipango.dns.Type;
import org.cipango.dns.util.BufferUtil;

public class ARecord extends Record
{
	private InetAddress _address;

	public ARecord()
	{
	}
	
	public ARecord(String name)
	{
		setName(new Name(name));
	}
	
	public ARecord(Name name)
	{
		setName(name);
	}
	
	@Override
	public Type getType()
	{
		return Type.A;
	}

	public InetAddress getAddress()
	{
		return _address;
	}

	@Override
	public void doDecode(ByteBuffer b, Compression c, int dataLength) throws IOException
	{
		if (dataLength != 4)
			throw new IOException("Invalid RDLENGTH: " + dataLength + " in A record");	
		_address = InetAddress.getByAddress(BufferUtil.getBytes(b, 4));	
	}
	
	@Override
	public void doEncode(ByteBuffer b, Compression c) throws IOException
	{
		b.put(_address.getAddress());
	}
	
	@Override
	public String toString()
	{
		if (_address == null)
			return super.toString();
		return super.toString() + " " + _address.getHostAddress();
	}
	
	@Override
	public boolean doEquals(Record record)
	{
		return compare(_address, ((ARecord) record).getAddress());
	}

	public void setAddress(InetAddress address)
	{
		_address = address;
	}
}
