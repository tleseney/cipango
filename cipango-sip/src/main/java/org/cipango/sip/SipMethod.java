package org.cipango.sip;

import java.nio.ByteBuffer;

import org.eclipse.jetty.util.StringMap;

public enum SipMethod 
{
	ACK, 
	BYE, 
	CANCEL, 
	INFO, 
	INVITE, 
	MESSAGE, 
	NOTIFY, 
	OPTIONS, 
	PRACK, 
	PUBLISH, 
	REFER, 
	REGISTER, 
	SUBSCRIBE, 
	UPDATE;
	
	public String asString()
	{
		return toString();
	}
	
	public static final StringMap<SipMethod> CACHE = new StringMap<SipMethod>(true);
	
	static 
	{
		for (SipMethod method : SipMethod.values())
			CACHE.put(method.toString(), method);
	}
	
	 public static SipMethod lookAheadGet(byte[] bytes, int position, int limit)
	 {
		 int length=limit-position;

		 switch (bytes[position])
		 {
		 	case 'I':
		 		if (length >= 6 && bytes[position+1] == 'N' && bytes[position+2] == 'V' && 
		 			bytes[position+3] == 'I' && bytes[position+4] == 'T' && bytes[position+5] == 'E')
		 			return INVITE;
	        case 'A':
	        	if (length >= 3 && bytes[position+1] == 'C' && bytes[position+2] == 'K')
	        		return ACK;
	        case 'B':
	        	if (length >= 3 && bytes[position+1] == 'Y' && bytes[position+2] == 'E')
	        		return BYE;       	
	        case 'R':
	        	if (length >= 8 && bytes[position+1] == 'E' && bytes[position+2] == 'G' && 
	        		bytes[position+3] == 'I' && bytes[position+4] == 'S' && bytes[position+5] == 'T' && 
	        		bytes[position+6] == 'E' && bytes[position+7] == 'R')
		        	return REGISTER;
		 }
		 return null;
	}
	 
	 public static SipMethod lookAheadGet(ByteBuffer buffer)
	    {
	        if (buffer.hasArray())
	            return lookAheadGet(buffer.array(),buffer.arrayOffset()+buffer.position(),buffer.arrayOffset()+buffer.limit());
	        return null;
	    }
}