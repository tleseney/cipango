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
import org.cipango.dns.Type;
import org.cipango.dns.util.BufferUtil;

public class GenericRecord extends Record
{
	private Type _type;
	private byte[] _data;
	
	public GenericRecord()
	{
	}
	
	public GenericRecord(Type type)
	{
		_type = type;
	}

	@Override
	public Type getType()
	{
		return _type;
	}

	@Override
	public void doEncode(ByteBuffer b, Compression c) throws IOException
	{
		if (_data != null)
			b.put(_data);
	}

	@Override
	public void doDecode(ByteBuffer b, Compression c, int dataLength) throws IOException
	{
		_data = BufferUtil.getBytes(b, dataLength);
	}

	public byte[] getData()
	{
		return _data;
	}

	public void setData(byte[] data)
	{
		_data = data;
	}

	public void setType(Type type)
	{
		_type = type;
	}

	@Override
	public boolean doEquals(Record record)
	{
		return false;
	}

}
