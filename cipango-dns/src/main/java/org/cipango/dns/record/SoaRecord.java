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
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  /                     MNAME                     /
 *  /                                               /
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  /                     RNAME                     /
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  |                    SERIAL                     |
 *  |                                               |
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  |                    REFRESH                    |
 *  |                                               |
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  |                     RETRY                     |
 *  |                                               |
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  |                    EXPIRE                     |
 *  |                                               |
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  |                    MINIMUM                    |
 *  |                                               |
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *
 */
public class SoaRecord extends Record
{
	private Name _primaryNameServer;
	private Name _mailbox;
	private int _serial;
	private int _refresh;
	private int _retry;
	private int _expires;
	private int _minimumTtl;
	
	@Override
	public Type getType()
	{
		return Type.SOA;
	}

	@Override
	public void doEncode(ByteBuffer b, Compression c) throws IOException
	{
		c.encodeName(_primaryNameServer, b);
		c.encodeName(_mailbox, b);
		BufferUtil.putInt(b, _serial);
		BufferUtil.putInt(b, _refresh);
		BufferUtil.putInt(b, _retry);
		BufferUtil.putInt(b, _expires);
		BufferUtil.putInt(b, _minimumTtl);
	}

	@Override
	public void doDecode(ByteBuffer b, Compression c, int dataLength) throws IOException
	{
		_primaryNameServer = c.decodeName(b);
		_mailbox = c.decodeName(b);
		_serial = BufferUtil.getInt(b);
		_refresh = BufferUtil.getInt(b);
		_retry = BufferUtil.getInt(b);
		_expires = BufferUtil.getInt(b);
		_minimumTtl = BufferUtil.getInt(b);
	}


	public int getSerial()
	{
		return _serial;
	}

	public int getRefresh()
	{
		return _refresh;
	}

	public int getRetry()
	{
		return _retry;
	}

	public int getExpires()
	{
		return _expires;
	}

	public int getMinimumTtl()
	{
		return _minimumTtl;
	}
	
	public String toString()
	{
		if (_primaryNameServer == null)
			return super.toString();
		return super.toString() + " mname " + _primaryNameServer;
	}

	public Name getPrimaryNameServer()
	{
		return _primaryNameServer;
	}

	public Name getMailbox()
	{
		return _mailbox;
	}
	
	@Override
	public boolean doEquals(Record record)
	{
		return compare(_primaryNameServer, ((SoaRecord) record).getPrimaryNameServer());
	}

}
