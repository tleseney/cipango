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

public class AaaaRecord extends Record
{
	private InetAddress _address;

	public AaaaRecord()
	{
	}
	
	public AaaaRecord(Name name)
	{
		setName(name);
	}
	
	public AaaaRecord(String name)
	{
		setName(new Name(name));
	}
	
	@Override
	public Type getType()
	{
		return Type.AAAA;
	}

	@Override
	public void doEncode(ByteBuffer b, Compression c) throws IOException
	{
		b.put(_address.getAddress());
	}

	@Override
	public void doDecode(ByteBuffer b, Compression c, int dataLength) throws IOException
	{
		if (dataLength != 16)
			throw new IOException("Invalid RDlength in AAAA record");
		_address = InetAddress.getByAddress(BufferUtil.getBytes(b, 16));
		
	}

	public InetAddress getAddress()
	{
		return _address;
	}

	public void setAddress(InetAddress address)
	{
		_address = address;
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
		return compare(_address, ((AaaaRecord) record).getAddress());
	}

}
