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
package org.cipango.server.dns;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import org.cipango.dns.DnsMessage;
import org.cipango.dns.DnsService;
import org.cipango.dns.Type;
import org.cipango.dns.record.Record;
import org.cipango.dns.section.HeaderSection.ResponseCode;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.junit.Ignore;

/**
 * DnsService used for test purpose.
 * When a lookup is done, it first checks if the answer is not saved in resources (answer has been 
 * saved by exporting packet bytes with Wireshark). 
 * This ensures that test can be done even if DNS server is not configured.
 */
@Ignore
public class TestDnsService extends DnsService
{
	private static final Logger LOG = Log.getLogger(DnsService.class);

	@Override
	public List<Record> lookup(Record record) throws IOException
	{
		List<Record> records = getCache().getRecords(record.getName(), record.getType());
		if (records != null && !records.isEmpty())
			return records;
		
		String name = record.getType() == Type.SRV ? record.getName().toString() : record.getName().getLabel();
		String resource = (record.getType() + "/" + name + ".dat").toLowerCase();
		try
		{
			DnsMessage query = new DnsMessage(record);
			DnsMessage answer = getMessage(resource);
			ResponseCode responseCode = answer.getHeaderSection().getResponseCode();
			//LOG.warn(">>>>>" + responseCode + "///"  + record);
			if (responseCode == ResponseCode.NAME_ERROR)
				getCache().addNegativeRecord(query, answer);
			else
				getCache().addRecordSet(query, answer);
			
			//System.out.println(getCache().dump());
		}
		catch (Exception e)
		{
			LOG.warn("Failed to get response for record " + record + " for resource " + resource);
		}

		return super.lookup(record);
	}
	
	public byte[] getRawMessage(String name) throws Exception
	{
		InputStream is = getClass().getResourceAsStream(name);
		if (is == null)
			return null;
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] b = new byte[512];
		int read;
		while ((read = is.read(b)) != -1)
			os.write(b, 0, read);;
		return os.toByteArray();
	}
	
	public DnsMessage getMessage(String name) throws Exception
	{
		ByteBuffer buffer = ByteBuffer.wrap(getRawMessage(name));
		DnsMessage message = new DnsMessage();
		message.decode(buffer);
		return message;
	}

}
