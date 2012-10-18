package org.cipango.server;

import java.nio.ByteBuffer;

import org.cipango.sip.SipGenerator;

public class SipMessageGenerator extends SipGenerator
{


	public void generateMessage(ByteBuffer buffer, SipMessage message)
	{
		if (message instanceof SipResponse)
		{

			SipResponse response = (SipResponse) message;
			generateResponse(buffer, response.getStatus(),
					response.getReasonPhrase(), response.getFields(), response.getRawContent());
		}
		else
		{
			SipRequest request = (SipRequest) message;
			generateRequest(buffer, request.getMethod(), request.getRequestURI(), 
					request.getFields(), request.getRawContent());
		}
	}

}
