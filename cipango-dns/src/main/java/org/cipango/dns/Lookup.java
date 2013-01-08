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
package org.cipango.dns;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import org.cipango.dns.record.CnameRecord;
import org.cipango.dns.record.Record;
import org.cipango.dns.section.HeaderSection.ResponseCode;

public class Lookup
{
	private Record _record;
	private Name _toSearch;
	private DnsClient _dnsClient;
	private int _iterations = 0;
	
	public Lookup(DnsClient client, Record record)
	{
		_dnsClient = client;
		_record = record;
	}
	
	public List<Record> resolve() throws IOException, UnknownHostException
	{
		IOException e = null;
		try
		{
			return resolve(_record);
		}
		catch (IOException e1) 
		{
			e = e1;
			for (Name suffix : _dnsClient.getSearchList())
			{
				Record record = _record.getType().newRecord();
				Name newName = _record.getName().clone();
				newName.append(suffix);
				record.setName(newName);
				record.setDnsClass(_record.getDnsClass());
				try 
				{
					return resolve(record);
				}
				catch (IOException e2) 
				{
				}
			}
		}
		
		throw e;
	}
	
	public List<Record> resolve(Record record) throws IOException, UnknownHostException
	{
		_toSearch = null;
		List<Record> records = getFromCache(record);
		
		while (records.isEmpty())
		{			
			if (_toSearch != null)
			{
				record = record.getType().newRecord();
				record.setName(_toSearch);
				record.setDnsClass(_record.getDnsClass());
			}
		
			DnsMessage query = new DnsMessage(record);
			DnsMessage answer = _dnsClient.resolve(query);
			incrementIteration();
			
			ResponseCode responseCode = answer.getHeaderSection().getResponseCode();
			if (responseCode == ResponseCode.NAME_ERROR)
			{
				_dnsClient.getCache().addNegativeRecord(query, answer);
				throw new UnknownHostException(_record.getName().toString());
			} 
			else if (responseCode != ResponseCode.NO_ERROR)
				throw new IOException("Got negative answer: " + answer.getHeaderSection().getResponseCode());
			
			if (answer.getAnswerSection().isEmpty())
				throw new UnknownHostException(_record.getName().toString());
			
			_dnsClient.getCache().addRecordSet(query, answer);
			
			records = getFromCache(record);
		}
		return records;
	}
	
	private List<Record> getFromCache(Record record) throws IOException
	{
		List<Record> records = _dnsClient.getCache().getRecords(record.getName(), record.getType());
		while (records.size() == 1 && records.get(0).getType() == Type.CNAME && record.getType() != Type.CNAME)
		{
			incrementIteration();
			_toSearch = ((CnameRecord) records.get(0)).getCname();
			records = _dnsClient.getCache().getRecords(_toSearch, record.getType());
		}
		return records;
	}
	
	private void incrementIteration() throws IOException
	{
		_iterations++;
		if (_iterations > 12)
			throw new IOException("Name " + _record.getName() + " Looped");
	}
}
