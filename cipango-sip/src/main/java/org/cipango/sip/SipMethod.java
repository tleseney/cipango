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

		 if (length < 4)
			 return null;
		 
		 switch (bytes[position])
		 {
		 	case 'I':
		 		if (bytes[position+1] == 'N' && bytes[position+2] == 'V' && bytes[position+3] == 'I' 
		 			&& length >= 7 && bytes[position+4] == 'T' && bytes[position+5] == 'E' && bytes[position+6] == ' ')
		 			return INVITE;
	        case 'A':
	        	if (length >= 3 && bytes[position+1] == 'C' && bytes[position+2] == 'K' && bytes[position+3] == ' ')
	        		return ACK;
	        case 'B':
	        	if (length >= 3 && bytes[position+1] == 'Y' && bytes[position+2] == 'E' && bytes[position+3] == ' ')
	        		return BYE;       	
	        case 'R':
	        	if (bytes[position+1] == 'E' && bytes[position+2] == 'G' && bytes[position+3] == 'I' && 
	        		length >= 9 && bytes[position+4] == 'S' && bytes[position+5] == 'T' && 
	        		bytes[position+6] == 'E' && bytes[position+7] == 'R' && bytes[position+8] == ' ')
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