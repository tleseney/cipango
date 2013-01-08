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
 * CNAME RRs cause no additional section processing, but name servers may choose to restart the
 * query at the canonical name in certain cases. See the description of name server logic in
 * [RFC-1034] for details.
 */
public class CnameRecord extends Record implements AdditionalName
{
	private Name _cname;

	@Override
	public Type getType()
	{
		return Type.CNAME;
	}

	@Override
	public void doEncode(ByteBuffer b, Compression c) throws IOException
	{
		c.encodeName(_cname, b);
	}

	@Override
	public void doDecode(ByteBuffer b, Compression c, int dataLength) throws IOException
	{
		 _cname = c.decodeName(b);
	}

	@Override
	public String toString()
	{
		if (_cname == null)
			return super.toString();
		return super.toString() + " " + _cname;
	}

	public Name getCname()
	{
		return _cname;
	}

	public void setCname(Name cname)
	{
		_cname = cname;
	}

	public Name getAdditionalName()
	{
		return _cname;
	}

	@Override
	public boolean doEquals(Record record)
	{
		return compare(_cname, ((CnameRecord) record).getCname());
	}

}
