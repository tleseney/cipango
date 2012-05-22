package org.cipango.sip;

import java.nio.ByteBuffer;
import java.util.Iterator;

import javax.servlet.sip.Address;
import javax.servlet.sip.Parameterable;

import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.StringMap;
import org.eclipse.jetty.util.StringUtil;

public class SipFields implements Iterable<SipFields.Field>
{
	private StringMap<Field> _map = new StringMap<SipFields.Field>(true);
	
	public Field getField(SipHeader header)
	{
		return _map.get(header.toString());
	}
	
	public Field getField(String name)
	{
		return _map.get(name);
	}
	
	public boolean containsKey(String name)
	{
		return _map.containsKey(name);
	}
	
	public String getString(String name)
	{
		Field field = getField(name);
		return field == null ? null : field.getValue().toString();
	}
	
	public String getString(SipHeader header)
	{
		return getString(header.toString());
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
		
		Field f = _map.get(name);
		
		if (f == null)
		{
			_map.put(name, field);
		}
		else
		{
			if (first)
			{
				field._next = f;
				_map.put(name, field);
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
		addString(name, value, false);
	}
	
	public Object get(SipHeader header)
	{
		Field field = getField(header);
		return field == null ? null : field.getValue();
	}
	
	public Object removeFirst(SipHeader header)
	{
		Field field = getField(header);
		
		if (field == null) return null;
		
		Field next = field._next;
		if (next != null)
			_map.put(next._name, next);
		else
			_map.remove(field._name);
		
		return field._value;
	}
	
	public void copy(SipFields source, SipHeader header)
	{
		Field field = source.getField(header);
		if (field != null)
		{
			_map.put(field._name, copy(field));
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
		
		public void putTo(ByteBuffer buffer)
		{
			if (_header != null)
			{
				buffer.put(_header.getBytesColonSpace());
				buffer.put(StringUtil.getBytes(_value.toString(), StringUtil.__UTF8));
			}
			BufferUtil.putCRLF(buffer);
			
			if (_next != null)
				_next.putTo(buffer);
		}
	}

	public Iterator<Field> iterator() 
	{
		return _map.values().iterator();
	}
}
