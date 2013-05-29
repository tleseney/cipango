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
package org.cipango.diameter;

import org.cipango.diameter.util.DiameterVisitor;
import org.cipango.diameter.util.Visitable;

public class AVP<T> implements Visitable
{
	private Type<T> _type;
	private T _value;
	
	public AVP(Type<T> type)
	{
		_type = type;
	}
	
	public AVP(Type<T> type, T value)
	{
		_type = type;
		_value = value;
	}
	
	public Type<T> getType()
	{
		return _type;
	}
	
	public void setValue(T value)
	{
		_value = value;
	}
	
	public T getValue()
	{
		return _value;
	}
	
	@SuppressWarnings("unchecked")
	public void accept(DiameterVisitor visitor)
	{
		if (_value instanceof AVPList)
		{
			visitor.visitEnter((AVP<AVPList>) this);
			for (AVP<?> avp : (AVPList) _value)
			{
				avp.accept(visitor);
			}
			visitor.visitLeave((AVP<AVPList>) this);
		}
		else
			visitor.visit(this);
	}
	
	public String toString()
	{
		return _type + " = " + _value;
	}
}