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
import org.cipango.dns.util.BufferUtil;

/**
 * @see <a href="http://www.faqs.org/rfcs/rfc2915.html">RFC 2915 - NAPTR DNS RR</a>
 * 
 * <pre>
 *                                  1  1  1  1  1  1
 *    0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  |                     ORDER                     |
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  |                   PREFERENCE                  |
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  /                     FLAGS                     /
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  /                   SERVICES                    /
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  /                    REGEXP                     /
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  /                  REPLACEMENT                  /
 *  /                                               /
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * </pre>
 */
public class NaptrRecord extends Record implements AdditionalName, Comparable<NaptrRecord>
{
	private int _order;
	private int _preference;
	private String _flags;
	private String _service;
	private String _regexp;
	private Name _replacement;

	public NaptrRecord()
	{
	}
	
	public NaptrRecord(String name)
	{
		setName(new Name(name));
	}
	
	@Override
	public Type getType()
	{
		return Type.NAPTR;
	}

	@Override
	public void doEncode(ByteBuffer b, Compression c) throws IOException
	{
		BufferUtil.put16(b, _order);
		BufferUtil.put16(b, _preference);
		encodeCharacterString(b, _flags.getBytes());
		encodeCharacterString(b, _service.getBytes());
		encodeCharacterString(b, _regexp.getBytes());
		c.encodeName(_replacement, b, true);
	}

	@Override
	public void doDecode(ByteBuffer b, Compression c, int dataLength) throws IOException
	{
		_order = BufferUtil.get16(b);
		_preference = BufferUtil.get16(b);
		_flags = new String(decodeCharacterString(b));
		_service = new String(decodeCharacterString(b));
		_regexp = new String(decodeCharacterString(b));
		_replacement = c.decodeName(b);
		
	}

	@Override
	public String toString()
	{
		if (_replacement == null)
			return super.toString();
		return super.toString() + " " + _order + " " + _preference + " \"" + _flags + "\" \"" + _service + "\" \"" + _regexp + "\" " + _replacement;
	}

	public Name getAdditionalName()
	{
		return _replacement;
	}

	public int getOrder()
	{
		return _order;
	}

	public int getPreference()
	{
		return _preference;
	}

	public String getFlags()
	{
		return _flags;
	}

	public String getService()
	{
		return _service;
	}

	public String getRegexp()
	{
		return _regexp;
	}

	public Name getReplacement()
	{
		return _replacement;
	}

	@Override
	public boolean doEquals(Record record)
	{
		NaptrRecord naptrRecord = (NaptrRecord) record;
		return compare(_regexp, naptrRecord.getRegexp())
				&& compare(_replacement, naptrRecord.getReplacement())
				&& compare(_service, naptrRecord.getService()) ;
	}
	
	@Override
	public int compareTo(NaptrRecord o2)
	{
		int order = getOrder() - o2.getOrder();
		if (order != 0)
			return order;
		int pref = getPreference() - o2.getPreference();
		if (pref != 0)
			return pref;
		return hashCode() - o2.hashCode();
	}
}
