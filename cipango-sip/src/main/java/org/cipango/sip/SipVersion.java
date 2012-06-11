package org.cipango.sip;

import java.nio.ByteBuffer;

import org.cipango.util.StringUtil;
import org.eclipse.jetty.util.StringMap;

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
	
	public final static StringMap<SipVersion> CACHE= new StringMap<SipVersion>(true);
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
