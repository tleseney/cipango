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
package org.cipango.diameter.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.cipango.diameter.AVP;
import org.cipango.diameter.Dictionary;
import org.cipango.diameter.Factory;
import org.cipango.diameter.Type;
import org.cipango.diameter.base.Common;

/**
 * <pre>
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                           AVP Code                            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |V M P r r r r r|                  AVP Length                   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                        Vendor-ID (opt)                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    Data ...
 * +-+-+-+-+-+-+-+-+
 * </pre>
 */
public class AVPCodec extends AbstractCodec<AVP<?>>
{
	private static final int AVP_VENDOR_FLAG = 0x80;
	private static final int AVP_MANDATORY_FLAG = 0x40;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public AVP<?> decode(ByteBuffer buffer) throws IOException
	{
		int code = buffer.getInt();
		int i = buffer.getInt();
		
		int flags = i >> 24 & 0xff;
		int length = i & 0xffffff;
		
		int dataLength = length - 8;
		int vendorId = 0;
		if ((flags & AVP_VENDOR_FLAG) == AVP_VENDOR_FLAG)
		{
			vendorId = buffer.getInt();
			dataLength -= 4;
		}
		
		//Buffer data = buffer.slice();
		ByteBuffer data = buffer.duplicate();
		
		data.position(buffer.position());
		data.limit(data.position() + dataLength);
		
		buffer.position(buffer.position() + (dataLength + 3 & -4));
		
		Type type = Dictionary.getInstance().getType(vendorId, code);
		
		if (type == null)
			type = Factory.newType("Unknown", vendorId, code, Common.__octetString);
		
		AVP avp = new AVP(type);
		// TODO flags
		avp.setValue(type.getDataFormat().decode(data));
		
		return avp;	
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ByteBuffer encode(ByteBuffer buffer, AVP avp) throws IOException
	{
		buffer = ensureSpace(buffer, 12);
		int flags = 0;
		if (avp.getType().isMandatory())
			flags |= AVP_MANDATORY_FLAG;
		
		int start = buffer.position();
		buffer.putInt(avp.getType().getCode());
		buffer.position(start+8);
		if (avp.getType().isVendorSpecific())
		{
			flags |= AVP_VENDOR_FLAG;
			buffer.putInt(avp.getType().getVendorId());
		}
		
		buffer = avp.getType().getDataFormat().encode(buffer, avp.getValue());
		
		buffer = ensureSpace(buffer, 8);
		
		pokeInt(buffer, start+4, flags << 24 | (buffer.position() - start) & 0xffffff);
		while (buffer.position() % 4 != 0)
			buffer.put((byte) 0);
		return buffer;
	}
}
