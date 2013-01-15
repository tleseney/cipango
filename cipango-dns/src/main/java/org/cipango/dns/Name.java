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

import java.io.Serializable;



public class Name implements Cloneable, Serializable
{
	private static final long serialVersionUID = 1L;
	
	public static final int MAX_LABEL_SIZE = 63;
	public static final int MAX_NAME_SIZE = 255;
	public static final Name EMPTY_NAME = new Name("");
	
	private String _label;
	private Name _child;
	
	public Name(String name)
	{
		if (name.length() > MAX_NAME_SIZE)
			throw new IllegalArgumentException("Name: " + name +  " is too long");
		int index = name.indexOf(".");
		if (index != -1)
		{
			_label = name.substring(0, index);
			_child = new Name(name.substring(index +1));
		}
		else
			_label = name;
		if (_label.length() > MAX_LABEL_SIZE)
			throw new IllegalArgumentException("Label: " + _label +  " is too long");
	}

	public void append(Name suffix)
	{
		Name name = this;
		while (name.hasChild())
			name = name.getChild();
		name.setChild(suffix);
	}
	
	public Name getChild()
	{
		return _child;
	}
	
	public void setChild(Name name)
	{
		if (_child != null)
			throw new IllegalStateException("Child already set");
		_child = name;
	}
	
	public String getLabel()
	{
		return _label;
	}
	
	public boolean hasChild()
	{
		return _child != null;
	}
	
	@Override
	public String toString()
	{
		if (hasChild())
			return _label + "." + _child.toString();
		else
			return _label;
	}
	
	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof Name))
			return false;
		
		Name name = (Name) o;
		if (!_label.equals(name.getLabel()))
			return false;
		
		if (hasChild())
			return name.hasChild() && _child.equals(name.getChild());
		else
			return !name.hasChild();
	}
	
	@Override
	public Name clone()
	{
		Name name;
		try
		{
			name = (Name) super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			throw new UnsupportedOperationException();
		}
		name._child = null;
		if (_child != null)
			name.setChild(_child.clone());
		return name;
	}
}
