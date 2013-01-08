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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * In order to reduce the size of messages, the domain system utilizes a compression scheme which
 * eliminates the repetition of domain names in a message. In this scheme, an entire domain name or
 * a list of labels at the end of a domain name is replaced with a pointer to a prior occurance of
 * the same name.
 */
public class Compression
{
	private Map<Name, Integer> _mapping = new HashMap<Name, Integer>();
	private Map<Integer, Name> _reverse = new HashMap<Integer, Name>();
	//private List<Entry> _mapping = new ArrayList<Entry>();
	
	private static final int POINTER_PREFIX = 0xC0;
	

	public void encodeName(Name name, ByteBuffer buffer)
	{
		encodeName(name, buffer, false);
	}
	
	public void encodeName(Name name, ByteBuffer buffer, boolean rdata)
	{
		while (name != null)
		{
			Integer index = getPosition(name);
			if (rdata || index == null)
			{
				addName(name, buffer.position());
				buffer.put((byte) name.getLabel().length());
				buffer.put(name.getLabel().getBytes());
			}
			else
			{
				buffer.put((byte) ((POINTER_PREFIX + (index >> 8)) & 0xFF));
				buffer.put((byte) (index & 0xFF));
				return;
			}
			name = name.getChild();
		}
		buffer.put((byte) 0);
	}
	
	private void addName(Name name, int index)
	{
		//_mapping.add(new Entry(name, index));
		_mapping.put(name, index);
		_reverse.put(index, name);
	}
	
	private Integer getPosition(Name name)
	{
		/*for (Entry entry : _mapping)
			if (entry.getName().equals(name))
				return entry.getPosition();
		return null;*/
		return _mapping.get(name);
	}
	
	private Name getName(Integer position)
	{
		/*for (Entry entry : _mapping)
			if (entry.getPosition().equals(position))
				return entry.getName();
		return null;*/
		return _reverse.get(position);
	}
	
	public void clear()
	{
		_mapping.clear();
	}
	
	public Name decodeName(ByteBuffer buffer)
	{
		List<Name> names = new ArrayList<Name>();
		while (true)
		{
			int size = buffer.get() & 0xff;
			if (size >= POINTER_PREFIX)
			{
				int index = (((size - POINTER_PREFIX) & 0xff) << 8) | (buffer.get() & 0xff);
				names.add(getName(index));
				break;
			}
			else if (size == 0)
			{
				break;
			}
			else
			{
				int position = buffer.position() - 1;
				byte[] bName = new byte[size];
				buffer.get(bName);
				Name name = new Name(new String(bName));
				addName(name, position);
				names.add(name);
			}
		}
		if (names.isEmpty())
			return Name.EMPTY_NAME;
		
		Iterator<Name> it = names.iterator();
		Name result = it.next();
		Name name = result;
		while (it.hasNext())
		{
			Name tmp = it.next();
			name.setChild(tmp);
			name = tmp;
		}
		return result;
	}
	
/*	class Entry
	{
		private Name _name;
		private Integer _position;
		
		public Entry(Name name, Integer position)
		{
			_name = name;
			_position = position;
		}
		
		public Name getName()
		{
			return _name;
		}
		public Integer getPosition()
		{
			return _position;
		}
		
	}*/
}
