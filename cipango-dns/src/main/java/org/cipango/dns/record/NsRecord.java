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
package org.cipango.dns.record;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.cipango.dns.Compression;
import org.cipango.dns.Name;
import org.cipango.dns.Type;

/**
 * NS records cause both the usual additional section processing to locate a type A record, and,
 * when used in a referral, a special search of the zone in which they reside for glue information.
 * 
 * The NS RR states that the named host should be expected to have a zone starting at owner name of
 * the specified class. Note that the class may not indicate the protocol family which should be
 * used to communicate with the host, although it is typically a strong hint. For example, hosts
 * which are name servers for either Internet (IN) or Hesiod (HS) class information are normally
 * queried using IN class protocols.
 */
public class NsRecord extends Record
{
	private Name _nsdName;

	@Override
	public Type getType()
	{
		return Type.NS;
	}

	@Override
	public void doEncode(ByteBuffer b, Compression c) throws IOException
	{
		c.encodeName(_nsdName, b);
	}

	@Override
	public void doDecode(ByteBuffer b, Compression c, int dataLength) throws IOException
	{
		 _nsdName = c.decodeName(b);
	}

	/**
	 * A <domain-name> which specifies a host which should be authoritative for the specified class
	 * and domain.
	 */
	public Name getNsdName()
	{
		return _nsdName;
	}

	public void setNsdName(Name nsdName)
	{
		_nsdName = nsdName;
	}
	
	@Override
	public String toString()
	{
		if (_nsdName == null)
			return super.toString();
		return super.toString() + " " + _nsdName;
	}

	@Override
	public boolean doEquals(Record record)
	{
		return compare(_nsdName, ((NsRecord) record).getNsdName());
	}

}
