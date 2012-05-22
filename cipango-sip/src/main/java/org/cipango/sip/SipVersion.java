package org.cipango.sip;

import java.nio.ByteBuffer;

import org.cipango.util.StringUtil;

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
	
	@Override
	public String toString()
	{
		return _string;
	}
}
