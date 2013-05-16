package org.cipango.server;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.cipango.sip.SipGenerator;

public class SipMessageGenerator extends SipGenerator
{
	public void generateMessage(ByteBuffer buffer, SipMessage message) throws MessageTooLongException
	{
		try
		{
			if (message instanceof SipResponse)
    		{
    			SipResponse response = (SipResponse) message;
    			generateResponse(buffer, 
    					response.getStatus(),
    					response.getNullableReasonPhrase(), 
    					response.getFields(), 
    					response.getRawContent(),
    					response.getHeaderForm());
    		}
    		else
    		{
    			SipRequest request = (SipRequest) message;
    			generateRequest(buffer, 
    					request.getMethod(), 
    					request.getRequestURI(), 
    					request.getFields(), 
    					request.getRawContent(),
    					request.getHeaderForm());
    		}
		}
		catch (BufferOverflowException e)
		{
			throw new MessageTooLongException(e);
		}
	}
}
