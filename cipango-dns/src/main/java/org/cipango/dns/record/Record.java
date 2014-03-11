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
import java.nio.ByteBuffer;

import org.cipango.dns.Compression;
import org.cipango.dns.DnsClass;
import org.cipango.dns.Name;
import org.cipango.dns.Type;
import org.cipango.dns.util.BufferUtil;


public abstract class Record
{	
	private int _ttl;
	private Name _name;
	private int _class = DnsClass.IN;
	
	public abstract Type getType();
	
	
	public abstract void doEncode(ByteBuffer b, Compression c) throws IOException;
		
	public abstract void doDecode(ByteBuffer b, Compression c, int dataLength) throws IOException;
	
	public abstract boolean doEquals(Record record);
	
	protected byte[] decodeCharacterString(ByteBuffer b)
	{
		int length = b.get();
		return BufferUtil.getBytes(b, length);
	}
	
	protected void encodeCharacterString(ByteBuffer b, byte[] characterString)
	{
		b.put((byte) (characterString.length & 0xFF));
		b.put(characterString);
	}

	public int getTtl()
	{
		return _ttl;
	}

	public void setTtl(int ttl)
	{
		_ttl = ttl;
	}

	public Name getName()
	{
		return _name;
	}

	public void setName(Name name)
	{
		_name = name;
	}


	public int getDnsClass()
	{
		return _class;
	}


	public void setDnsClass(int clazz)
	{
		_class = clazz;
	}
	
	@Override
	public String toString()
	{
		return _name + ": type " + getType();
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o == this)
			return true;
		
		if (!(o instanceof Record))
			return false;
		
		Record record = (Record) o;
		if (record.getType() != getType())
			return false;
		
		if (!_name.equals(record.getName()))
			return false;
		
		if (_class != record.getDnsClass())
			return false;
		
		return doEquals(record);
	}
	
	protected boolean compare(Object o1, Object o2)
	{
		if (o1 == null)
			return o2 == null;
		else
			return o1.equals(o2);
	}
}
