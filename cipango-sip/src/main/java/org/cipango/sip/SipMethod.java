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
	
	 /*
	  * This method expects the method to be followed by a white space. For raw
	  * method strings, please use the get method below.
	  */
	 public static SipMethod lookAheadGet(byte[] bytes, int position, int limit)
	 {
		 int length=limit-position;

		 if (length < 4)
			 return null;
		 
		 switch (bytes[position])
		 {
	        case 'A':
	        	if (bytes[position+1] == 'C' && bytes[position+2] == 'K' && bytes[position+3] == ' ')
	        		return ACK;
		 		break;
	        case 'B':
	        	if (bytes[position+1] == 'Y' && bytes[position+2] == 'E' && bytes[position+3] == ' ')
	        		return BYE;  
		 		break;
	        case 'C':
	        	if (length >= 7 && bytes[position+1] == 'A' && bytes[position+2] == 'N' && bytes[position+3] == 'C'
	        		&& bytes[position+4] == 'E' && bytes[position+5] == 'L' && bytes[position+6] == ' ')
	        		return CANCEL;  
		 		break;
		 	case 'I':
		 		if (bytes[position+1] == 'N' && bytes[position+2] == 'F' && bytes[position+3] == 'O' 
		 			&& length >= 5 && bytes[position+4] == ' ')
		 			return INFO;
		 		if (bytes[position+1] == 'N' && bytes[position+2] == 'V' && bytes[position+3] == 'I' 
			 			&& length >= 7 && bytes[position+4] == 'T' && bytes[position+5] == 'E' && bytes[position+6] == ' ')
			 			return INVITE;
		 		break;
	        case 'M':
	        	if (length >= 8 && bytes[position+1] == 'E' && bytes[position+2] == 'S' && bytes[position+3] == 'S'
	        		&& bytes[position+4] == 'A' && bytes[position+5] == 'G' && bytes[position+6] == 'E' 
	        		&& bytes[position+7] == ' ')
	        		return MESSAGE;  
		 		break;
	        case 'N':
	        	if (length >= 7 && bytes[position+1] == 'O' && bytes[position+2] == 'T' && bytes[position+3] == 'I'
	        		&& bytes[position+4] == 'F' && bytes[position+5] == 'Y' && bytes[position+6] == ' ')
	        		return NOTIFY;  
		 		break;
	        case 'O':
	        	if (length >= 8 && bytes[position+1] == 'P' && bytes[position+2] == 'T' && bytes[position+3] == 'I'
	        		&& bytes[position+4] == 'O' && bytes[position+5] == 'N' && bytes[position+6] == 'S' 
	        		&& bytes[position+7] == ' ')
	        		return OPTIONS;  
		 		break;
	        case 'P':
	        	if (length >= 6 && bytes[position+1] == 'R' && bytes[position+2] == 'A' && bytes[position+3] == 'C'
	        		&& bytes[position+4] == 'K' && bytes[position+5] == ' ')
	        		return PRACK;  
	        	if (length >= 8 && bytes[position+1] == 'U' && bytes[position+2] == 'B' && bytes[position+3] == 'L'
	        		&& bytes[position+4] == 'I' && bytes[position+5] == 'S' && bytes[position+6] == 'H' 
	        		&& bytes[position+7] == ' ')
	        		return PUBLISH;  
		 		break;
	        case 'R':
	        	if (bytes[position+1] == 'E' && bytes[position+2] == 'F' && bytes[position+3] == 'E' && 
	        		length >= 6 && bytes[position+4] == 'R' && bytes[position+5] == ' ')
		        	return REFER;
	        	if (bytes[position+1] == 'E' && bytes[position+2] == 'G' && bytes[position+3] == 'I' && 
	        		length >= 9 && bytes[position+4] == 'S' && bytes[position+5] == 'T' && 
	        		bytes[position+6] == 'E' && bytes[position+7] == 'R' && bytes[position+8] == ' ')
		        	return REGISTER;
		 		break;
	        case 'S':
	        	if (length >= 10 && bytes[position+1] == 'U' && bytes[position+2] == 'B' && bytes[position+3] == 'S'
	        		&& bytes[position+4] == 'C' && bytes[position+5] == 'R' && bytes[position+6] == 'I' 
	        		&& bytes[position+7] == 'B' && bytes[position+8] == 'E' && bytes[position+9] == ' ')
	        		return SUBSCRIBE;  
		 		break;
	        case 'U':
	        	if (length >= 7 && bytes[position+1] == 'P' && bytes[position+2] == 'D' && bytes[position+3] == 'A'
	        		&& bytes[position+4] == 'T' && bytes[position+5] == 'E' && bytes[position+6] == ' ')
	        		return UPDATE;  
		 }
		 return null;
	}
	 
	public static SipMethod lookAheadGet(ByteBuffer buffer)
	{
		if (buffer.hasArray())
			return lookAheadGet(buffer.array(), buffer.arrayOffset() + buffer.position(),
					buffer.arrayOffset() + buffer.limit());
		return null;
	}

	public static SipMethod get(String method)
	{
		byte[] b = (method + " ").getBytes();
		return lookAheadGet(b, 0, b.length);
	}
}