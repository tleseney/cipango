// ========================================================================
// Copyright 2011-2012 NEXCOM Systems
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
package org.cipango.console.data;

import java.util.ArrayList;
import java.util.List;

import javax.management.ObjectName;

public class Row
{

	private List<Value> _values = new ArrayList<Value>();
	private List<Object> _operations;
	private ObjectName _objectName;
	
	public List<Value> getValues()
	{
		return _values;
	}

	public List<?> getOperations()
	{
		return _operations;
	}
	
	public void addOperation(Object o)
	{
		if (_operations == null)
			_operations = new ArrayList<Object>();
		_operations.add(o);
	}

	public ObjectName getObjectName()
	{
		return _objectName;
	}

	public void setObjectName(ObjectName objectName)
	{
		_objectName = objectName;
	}
	
	public Value get(Header header)
	{
		for (Value value: _values)
		{
			if (value.getHeader().equals(header))
				return value;
		}
		return null;
	}
	
	public static class Value
	{
		private Object _value;
		private Header _header;
		
		public Value(Object value, Header header)
		{
			_value = value;
			_header = header;
		}

		public Object getValue()
		{
			return _value;
		}

		public Header getHeader()
		{
			return _header;
		}

		public void setValue(Object value)
		{
			_value = value;
		}
	}
	
	public static class Header
	{
		private String _simpleName;
		private String _name;
		private String _note;
		
		public Header(String name)
		{
			_name = _simpleName = name;
		}
		
		
		public Header(String simpleName, String name)
		{
			_simpleName = simpleName;
			_name = name;
		}
		
		public Header(String simpleName, String name, String note)
		{
			_simpleName = simpleName;
			_name = name;
			_note = note;
		}
				
		public String getName()
		{
			return _name;
		}
		public void setName(String name)
		{
			_name = name;
		}
		public String getNote()
		{
			return _note;
		}
		public void setNote(String note)
		{
			_note = note;
		}

		public String getSimpleName()
		{
			return _simpleName;
		}	
		
		@Override
		public boolean equals(Object o)
		{
			if (o == this)
				return true;
			if (!(o instanceof Header))
				return false;
			return ((Header) o).getSimpleName().equals(_simpleName);
		}
	}
}

