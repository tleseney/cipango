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
package org.cipango.dns.section;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import org.cipango.dns.DnsMessage;
import org.cipango.dns.Name;
import org.cipango.dns.Type;
import org.cipango.dns.record.Record;
import org.cipango.dns.util.BufferUtil;

/**
 * <pre>
 *                                   1  1  1  1  1  1
 *     0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
 *    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *    |                                               |
 *    /                                               /
 *    /                      NAME                     /
 *    |                                               |
 *    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *    |                      TYPE                     |
 *    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *    |                     CLASS                     |
 *    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *    |                      TTL                      |
 *    |                                               |
 *    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *    |                   RDLENGTH                    |
 *    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--|
 *    /                     RDATA                     /
 *    /                                               /
 *    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * </pre>
 */
public class ResourceRecordsSection extends AbstractList<Record>
{
	private List<Record> _records = new ArrayList<Record>();
	private DnsMessage _message;
	
	public ResourceRecordsSection(DnsMessage message)
	{
		_message = message;
	}

	public void encode(ByteBuffer buffer) throws IOException
	{
		for (Record record : _records)
		{
			getMessage().getCompression().encodeName(record.getName(), buffer);
			record.getType().encode(buffer);
			BufferUtil.put16(buffer, record.getDnsClass());
			BufferUtil.putInt(buffer, record.getTtl());
			int index = buffer.position();
			buffer.position(index + 2);
			record.doEncode(buffer, getMessage().getCompression());
			BufferUtil.poke16(buffer, index, buffer.position() - index - 2);
		}
	}

	public void decode(ByteBuffer buffer, int nbRecords) throws IOException
	{
		for (int i = 0; i < nbRecords; i++)
		{
			Name name = getMessage().getCompression().decodeName(buffer);
			Type type = Type.getType(BufferUtil.get16(buffer));
			int clazz = BufferUtil.get16(buffer);
			
			Record record = type.newRecord();
			record.setName(name);
			record.setDnsClass(clazz);
			record.setTtl(BufferUtil.getInt(buffer));
			int dataLength = BufferUtil.get16(buffer);
			record.doDecode(buffer, getMessage().getCompression(), dataLength);
			_records.add(record);
		}
	}
	
	public void add(int index,Record record)
	{
		_records.add(index,record);
	}
	
	public int size()
	{
		return _records.size();
	}
		
	public Record get(int index)
	{
		return _records.get(index);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Record> T get(Class<T> clazz)
	{
		for (Record record : _records)
			if (clazz.isAssignableFrom(record.getClass()))
				return (T) record;
		return null;
	}

	public DnsMessage getMessage()
	{
		return _message;
	}
	
	public StringBuilder append(StringBuilder sb, String role)
	{
		if (!isEmpty())
		{	
			sb.append(role).append("\n");
			for (Record record : _records)
				sb.append("  ").append(record).append("\n");
		}
		return sb;
	}
}
