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


public enum DnsClass
{

	/**
	 * the Internet
	 */
	IN(1),
	/**
	 * the CSNET class (Obsolete - used only for examples in some obsolete RFCs)
	 * @deprecated
	 */
	CS(2),
	/**
	 * the CHAOS class
	 */
	CH(3),
	/**
	 *  Hesiod [Dyer 87]
	 */
	HS(4),
	/** 
	 * Special value used in dynamic update messages 
	 */
	NONE(254),
	/** 
	 * Matches any class 
	 */
	ANY(255);


	private int _value;

	private DnsClass(int value)
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
	
	public static DnsClass getClass(int value) throws IOException
	{
		for (DnsClass t : DnsClass.values())
			if (t.getValue() == value)
				return t;
		throw new IOException("Could not found DNS class with value: " + value);
	}
}
