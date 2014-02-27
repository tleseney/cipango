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
import java.nio.ByteBuffer;

import org.cipango.dns.util.BufferUtil;


public class DnsClass
{

	/**
	 * the Internet
	 */
	public static DnsClass IN = new DnsClass(1);
	/**
	 * the CSNET class (Obsolete - used only for examples in some obsolete RFCs)
	 * @deprecated
	 */
	public static DnsClass CS = new DnsClass(2);
	/**
	 * the CHAOS class
	 */
	public static DnsClass CH = new DnsClass(3);
	/**
	 *  Hesiod [Dyer 87]
	 */
	public static DnsClass HS = new DnsClass(4);
	/** 
	 * Special value used in dynamic update messages 
	 */
	public static DnsClass NONE = new DnsClass(254);
	/** 
	 * Matches any class 
	 */
	public static DnsClass ANY = new DnsClass(255);


	private int _value;

	public DnsClass(int value)
	{
		_value = value;
	}
	
	public int getValue()
	{
		return _value;
	}
	
	public void encode(ByteBuffer b)
	{
		BufferUtil.put16(b, _value);
	}
	
	@Override
	public int hashCode()
	{
		return _value;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DnsClass other = (DnsClass) obj;
		if (_value != other._value)
			return false;
		return true;
	}
}
