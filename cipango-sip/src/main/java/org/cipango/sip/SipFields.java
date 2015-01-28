// ========================================================================
// Copyright 2006-2015 NEXCOM Systems
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
package org.cipango.sip;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.servlet.sip.Address;
import javax.servlet.sip.Parameterable;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletMessage.HeaderForm;

import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.StringUtil;

public class SipFields implements Iterable<SipFields.Field>
{
	private final Map<String, Field> _map = new LinkedHashMap<String, Field>();
	
	public SipFields()
	{
		
	}
	
	public SipFields(SipFields other)
	{
		_map.putAll(other._map);
	}
	
	private String normalizeName(String name) 
	{
		return name.toLowerCase(Locale.ENGLISH);
	}

	public Field getField(SipHeader header)
	{
		return _map.get(normalizeName(header.toString()));
	}
	
	public Field getField(String name)
	{
		return _map.get(normalizeName(name));
	}
	
	public Field getField(SipHeader header, String name)
	{
		if (header != null)
			name = header.toString();
		
		return getField(name);
	}
	
	public boolean containsKey(String name)
	{
		return _map.containsKey(normalizeName(name));
	}
	
	public String getString(String name)
	{
		return getString(null, SipHeader.getFormattedName(name));
	}
	
	public String getString(SipHeader header)
	{
		return getString(header, null);
	}
	
	public String getString(SipHeader header, String name)
	{
		if (header != null)
			name = header.toString();
		
		Field field = getField(name);
		return field == null ? null : field.getValue().toString();
	}
	
	public void addString(String name, String value, boolean first) throws IllegalArgumentException
	{
		add(name, value, first);
	}
	
	public void add(String name, Object value, boolean first) throws IllegalArgumentException
	{
		if (value == null)
			throw new IllegalArgumentException("null value");
		
		Field field = new Field(name, value);
		
		Field f = _map.get(normalizeName(name));
		
		if (f == null)
		{
			putInternal(field);
		}
		else
		{
			if (first)
			{
				field._next = f;
				putInternal(field);
			}
			else 
			{
				while (f._next != null)
					f = f._next;
				f._next = field;
			}
		}
	}
	
	public void add(String name, String value) throws IllegalArgumentException
	{
		name = SipHeader.getFormattedName(name);
		addString(name, value, false);
	}
	
	public Object get(SipHeader header)
	{
		Field field = getField(header);
		return field == null ? null : field.getValue();
	}
	
	public void set(String name, Object value)
	{
		name = SipHeader.getFormattedName(name);
		putInternal(new Field(name, value));
	}
	
	private void putInternal(Field field)
	{
		_map.put(normalizeName(field._name), field);
	}
	
	public void set(SipHeader header, Object value)
	{
		set(header.asString(), value);
	}
	
	public Object removeFirst(SipHeader header)
	{
		Field field = getField(header);
		
		if (field == null) return null;
		
		Field next = field._next;
		if (next != null)
			putInternal(next);
		else
			remove(field._name);
		
		return field._value;
	}
	
	public void remove(String name)
	{
		_map.remove(normalizeName(name));
	}
	
	public void copy(SipFields source, SipHeader header)
	{
		Field field = source.getField(header);
		if (field != null)
		{
			putInternal(copy(field));
		}
	}
	
	private Field copy(Field field)
	{
		Field previous = null;
		Field first = null;
		
		SipHeader header = field._header;
		
		while (field != null)
		{
			Object o = field.getValue();
			
			switch (header.getType())
			{
			case VIA: 
				o = ((Via) o).clone();
				break;
			case ADDRESS:
				o = ((Address) o).clone();
				break;
			case PARAMETERABLE:
				o = ((Parameterable) o).clone();
				break;
			}
			Field f = new Field(header, o);
			if (previous != null)
				previous._next = f;
			else
				first =f;
			
			field = field._next;
			previous = f;
		}
		return first;
	}
	
	public ListIterator<String> getValues(String name)
	{
		Field field = getField(SipHeader.getFormattedName(name));
    	
    	return new FieldIterator<String>(field)
    	{
    		public String getValue() { return _f.getValue().toString(); }
    	};
	}
	
	public long getLong(SipHeader header)
	{
		Field field = getField(header.asString());
		if (field != null)
			return Long.parseLong(field.getValue().toString());
		return -1l;
	}
	
    public Iterator<String> getNames()
    {
    	Set<String> result = new LinkedHashSet<>();
        for (Field field : _map.values())
            result.add(field._name);
        return result.iterator();
    }

    public ListIterator<Parameterable> getParameterableValues(SipHeader header, String name) throws ServletParseException
    {
    	Field field = getField(header, name);
    	
    	Field f = field;
    	while (f != null) // Throw ServletParseException if needed 
    	{
    		f.asParameterable();
    		f = f._next;
    	}
    	
    	return new FieldIterator<Parameterable>(field)
    	{
    		public Parameterable getValue() { return (Parameterable) _f.getValue(); }
    	};
    }
    
    public ListIterator<Address> getAddressValues(SipHeader header, String name) throws ServletParseException
    {
    	Field field = getField(header, name);
    	
    	Field f = field;
    	while (f != null) // Throw ServletParseException if needed 
    	{
    		f.asAddress();
    		f = f._next;
    	}
    	
    	return new FieldIterator<Address>(field)
    	{
    		public Address getValue() { return (Address) _f.getValue(); }
    	};
    }
	
	public static final class Field
	{
		private  SipHeader _header;
		private  String _name;
		private Object _value;
		
		private Field _next;
		
		public Field(SipHeader header, Object value)
		{
			_header = header;
			_value = value;
			_name = _header.toString();
		}
		
		public Field(String name, Object value)
		{
			_header = SipHeader.CACHE.get(name);
			_name = _header == null ? name : _header.toString();
			_value = value;
		}
		
		public SipHeader getHeader()
		{
			return _header;
		}
		
		public Object getValue()
		{
			return _value;
		}
		
		public Address asAddress() throws ServletParseException
    	{
    		if (!(_value instanceof Address))
    		{
    			AddressImpl value = new AddressImpl(_value.toString());
    			try
				{
					value.parse();
				}
				catch (ParseException e)
				{
					throw new ServletParseException(e);
				}
    			_value = value;
    		}

    		return (Address) _value;
    	}
		
		public Parameterable asParameterable() throws ServletParseException
    	{
    		if (!(_value instanceof Parameterable))
    		{
    			Parameterable value;
    			try
    			{
    				value = new ParameterableImpl(_value.toString());
    			}
    			catch (ParseException e)
    			{
    				throw new ServletParseException(e);
    			}
    			_value = value;
    		}
    		return (Parameterable) _value;
    	}
		
		public void putTo(ByteBuffer buffer, HeaderForm headerForm)
		{
			if (_header != null)
			{
				if (headerForm == HeaderForm.COMPACT)
				{
					Byte compact = SipHeader.REVERSE_COMPACT_CACHE.get(_header);
					if (compact != null)
					{
						buffer.put(compact);
						buffer.put(SipGrammar.COLON);
						buffer.put(SipGrammar.SPACE);
					}
					else
						buffer.put(_header.getBytesColonSpace());
				}
				else
					buffer.put(_header.getBytesColonSpace());
			}
			else
			{
				buffer.put(StringUtil.getUtf8Bytes(_name));
				buffer.put(SipGrammar.COLON);
				buffer.put(SipGrammar.SPACE);
			}

			Field next = _next;
			
			buffer.put(StringUtil.getBytes(_value.toString(), StringUtil.__UTF8));
			if (_header != null && _header.isMerge())
			{
				while (next != null)
				{
					buffer.put((byte) ',');
					buffer.put(SipGrammar.SPACE);
					buffer.put(StringUtil.getUtf8Bytes((next._value.toString())));
					next = next._next;
				}
			}
			
			BufferUtil.putCRLF(buffer);
			
			if (next != null)
				next.putTo(buffer, headerForm);

		}
		
		@Override
		public String toString()
		{
			return String.valueOf(_value);
		}
	}

	public Iterator<Field> iterator() 
	{
		return _map.values().iterator();
	}
	
    abstract class FieldIterator<E> implements ListIterator<E> 
    {
    	Field _f;
		Field _first;
		int _index = 0;
		
		public FieldIterator(Field field)
		{
			_f = field;
			_first = field;
		}
		
		public boolean hasNext()
		{
			return _f != null;
		}
		
		public E next()
		{
			if (_f == null) throw new NoSuchElementException();
			
			E value = getValue();
			_f = _f._next;
			_index++;
			
			return value;
		}
		
		public abstract E getValue();
		
		public boolean hasPrevious() 
		{ 
			return _index > 0;
		}
		
		public E previous()
		{
			if (!hasPrevious()) throw new NoSuchElementException();
			
			_index--;
			
			_f = _first;
			for (int i = 0; i < _index; i++)
				_f = _f._next;
			
			return getValue();
		}
		
		public int nextIndex() { return _index; }
		public int previousIndex() { return _index-1; }
		
		public void set(E e) { throw new UnsupportedOperationException(); }
		public void add(E e) { throw new UnsupportedOperationException(); }
		public void remove() { throw new UnsupportedOperationException(); }
    }
}
