// ========================================================================
// Copyright 2010 NEXCOM Systems
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

import javax.management.MBeanAttributeInfo;

public class Property
{
	private String _name;
	private Object _value;
	private String _note;
	
	public Property()
	{
	}
	
	public Property(String name, Object value)
	{
		_name = name;
		_value = value;
	}
	
	public Property(MBeanAttributeInfo info, Object value, String note)
	{
		setName(info.getDescription());
		setValue(value);
		setNote(note);
	}
	
	public String getName()
	{
		return _name;
	}
	public void setName(String name)
	{
		_name = name;
	}
	public Object getValue()
	{
		return _value;
	}
	public void setValue(Object value)
	{
		_value = value;
	}
	public String getNote()
	{
		return _note;
	}
	public void setNote(String note)
	{
		_note = note;
	}
	public boolean hasNote()
	{
		return _note != null;
	}
}
