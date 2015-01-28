/========================================================================
//Copyright 2006-2015 NEXCOM Systems
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================
package org.cipango.sip;

import java.nio.ByteBuffer;

import org.cipango.util.StringUtil;
import org.eclipse.jetty.util.ArrayTrie;
import org.eclipse.jetty.util.Trie;

public enum SipVersion 
{
	SIP_2_0("SIP/2.0");

	private String _string;
	private byte[] _bytes;
	private ByteBuffer _buffer;
	
	SipVersion(String s)
	{
		_string = s;
		_bytes = StringUtil.getBytes(s, StringUtil.__UTF8);
		_buffer = ByteBuffer.wrap(_bytes);
	}
	
	public ByteBuffer toBuffer()
	{
		return _buffer.asReadOnlyBuffer();
	}
	
	public String asString()
	{
		return _string;
	}
	
	@Override
	public String toString()
	{
		return _string;
	}
	
	public static Trie<SipVersion> CACHE = new ArrayTrie<SipVersion>();
    static
    {
        for (SipVersion version : SipVersion.values())
            CACHE.put(version.toString(),version);
    }
    
	public static SipVersion lookAheadGet(byte[] bytes, int position, int limit)
	{
		int length=limit-position;
        if (length<8)
            return null;
        
        if (bytes[position] == 'S' && bytes[position+1] == 'I' && bytes[position+2] == 'P' &&
        	bytes[position+3] == '/' && bytes[position+4] == '2' && bytes[position+5] == '.' &&
        	bytes[position+6] == '0' && Character.isWhitespace((char)bytes[position+7]))
        	return SIP_2_0;
        
        return null;
	}
	
	public static SipVersion lookAheadGet(ByteBuffer buffer)
	{
		if (buffer.hasArray())
			return lookAheadGet(buffer.array(), buffer.arrayOffset()+buffer.position(), buffer.arrayOffset()+buffer.limit());
		return null;
	}
}
