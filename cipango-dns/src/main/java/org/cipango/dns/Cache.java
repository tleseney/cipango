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

import java.rmi.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.cipango.dns.record.AdditionalName;
import org.cipango.dns.record.Record;
import org.cipango.dns.record.SoaRecord;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class Cache
{
	public static final int DEFAULT_NEGATIVE_TTL = 3600;
	private static final Logger LOG = Log.getLogger(Cache.class);
	
	private ConcurrentMap<Name, List<Element>> _cache = new ConcurrentHashMap<Name, List<Element>>();
	
	public Cache()
	{
	}
	
	public void addRecord(Record record)
	{
		List<Element> records = _cache.get(record.getName());
		
		if (records ==null)
		{
			records = new ArrayList<Element>();
			List<Element> records2 = _cache.putIfAbsent(record.getName(), records);
			if (records2 != null)	
				records = records2;
		}
		
		synchronized (records)
		{
			for (Element element : records)
			{
				if (element.get().equals(record))
				{
					LOG.debug("Do not add record {} on {} as it is already present", record, records);
					return;
				}
			}
			LOG.debug("cache.add: {} on {}", record, records);
			records.add(new Element(record));
		}
	}
	
	public void addNegativeRecord(DnsMessage query, DnsMessage answer)
	{
		Record record = query.getQuestionSection().get(0);
		List<Element> records = _cache.get(record.getName());
		
		if (records ==null)
		{
			records = new ArrayList<Element>();
			List<Element> records2 = _cache.putIfAbsent(record.getName(), records);
			if (records2 != null)	
				records = records2;
		}
		
		int ttl = DEFAULT_NEGATIVE_TTL;
		for (Record record2 : answer.getAuthoritySection())
		{
			if (record2 instanceof SoaRecord)
				ttl = ((SoaRecord) record2).getTtl();
		}
		
		LOG.debug("Negative cache.add: " + record + " on " + records + " with ttl " + ttl);
		synchronized (records)
		{
			records.add(new Element(record, ttl, true));
		}
	}
	
	public void addRecordSet(DnsMessage query, DnsMessage answer)
	{		
		List<Name> toAdd = new ArrayList<Name>();
		for (Record record : query.getQuestionSection())
			toAdd.add(record.getName());
		
		for (Record record : answer.getAnswerSection())
		{
			if (toAdd.contains(record.getName()))
			{
				addRecord(record);
				if (record instanceof AdditionalName)
					toAdd.add(((AdditionalName) record).getAdditionalName());
			}
			
		}
		for (Record record : answer.getAdditionalSection())
		{
			if (toAdd.contains(record.getName()))
			{
				addRecord(record);
				if (record instanceof AdditionalName)
					toAdd.add(((AdditionalName) record).getAdditionalName());
			}
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public List<Record> getRecords(Name name, Type type) throws UnknownHostException
	{
		List<Element> records = _cache.get(name);
		if (records != null)
		{
			List<Record> list = new ArrayList<Record>();
			synchronized (records)
			{
				Iterator<Element> it = records.iterator();
				while (it.hasNext())
				{
					Element element = it.next();
					if (element.isExpired())
						it.remove();
					else if (element.get().getType() == type)
					{
						if (element.isNegative())
							throw new UnknownHostException(name.toString());
						list.add(element.get());
					}
					else if (element.get().getType() == Type.CNAME)
					{
						list.add(element.get());
					}
				}
			}
			return list;
		}
		return Collections.EMPTY_LIST;
	}
	
	
	class Element
	{
		private Record _record;
		private Long _expires;
		private boolean _negative;
		
		public Element(Record record)
		{
			_record = record;
			_expires = System.currentTimeMillis() + record.getTtl() * 1000;
		}
		
		public Element(Record record, long ttl, boolean negative)
		{
			_record = record;
			_expires = System.currentTimeMillis() + ttl * 1000;
			_negative = negative;
		}
		
		public boolean isExpired()
		{
			return System.currentTimeMillis() > _expires;
		}
		
		public Record get()
		{
			return _record;
		}
		
		public String toString()
		{
			return _record.toString() + "@" + new Date(_expires);
		}

		public boolean isNegative()
		{
			return _negative;
		}
	}
}
