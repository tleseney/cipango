package org.cipango.sip;

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Set;

import org.cipango.util.StringUtil;

import static org.cipango.sip.SipHeader.*;

public class SipGenerator 
{
	private Set<SipHeader> _fixedHeaders = EnumSet.of(VIA, FROM, TO, CALL_ID);
	
	public void generateResponse(ByteBuffer buffer, int status, String reason, SipFields sipFields)
	{
		generateResponseLine(buffer, status, reason);
		generateHeaders(buffer, sipFields);
	}
	
	private void generateResponseLine(ByteBuffer buffer, int status, String reason)
	{
		PreparedResponse prepared = status < __responses.length ? __responses[status] : null;
		if (prepared != null)
		{
			if (reason == null)
				buffer.put(prepared._responseLine);
			else
			{
				buffer.put(prepared._schemeCode);
				buffer.put(StringUtil.getBytes(reason, StringUtil.__UTF8));
				buffer.put(SipGrammar.CRLF);
			}
		}
		else
		{
			// TODO
		}
	}
	
	private void generateHeaders(ByteBuffer buffer, SipFields sipFields)
	{
		if (sipFields != null)
		{
			for (SipFields.Field field : sipFields)
			{
				field.putTo(buffer);
			}
		}
		
		buffer.put(SipGrammar.CRLF);
	}
	
	private static class PreparedResponse
	{
		byte[] _reason;
		byte[] _schemeCode;
		byte[] _responseLine;
	}
	
	private static final PreparedResponse[] __responses = new PreparedResponse[SipStatus.MAX_CODE+1];
	
	static
	{
		int versionLength = SipVersionTest.SIP_2_0.toString().length();
		
		for (int i = 0; i < __responses.length; i++)
		{
			SipStatus status = SipStatus.get(i);
			if (status == null)
				continue;
			String reason = status.getReason();
			byte[] line = new byte[versionLength+5+reason.length()+2];
			SipVersionTest.SIP_2_0.toBuffer().get(line, 0, versionLength);
			line[versionLength+0] = ' ';
			line[versionLength+1] = (byte) ('0' + i/100);
			line[versionLength+2] = (byte) ('0' + (i%100) /10);
			line[versionLength+3] = (byte) ('0' + (i%10));
			line[versionLength+4] = ' ';			
			
			for (int j = 0; j < reason.length(); j++)
				line[versionLength+5+j] = (byte) reason.charAt(j);
			line[versionLength+5+reason.length()] = SipGrammar.CR;
			line[versionLength+6+reason.length()] = SipGrammar.LF;
			
			__responses[i] = new PreparedResponse();
			__responses[i]._reason = new byte[line.length-versionLength-7];
			System.arraycopy(line,versionLength+5,__responses[i]._reason,0,line.length-versionLength-7);
			__responses[i]._schemeCode=new byte[versionLength+5];
            System.arraycopy(line,0,__responses[i]._schemeCode,0,versionLength+5);
            __responses[i]._responseLine=line;			
		}
	}
	
	public static void main(String[] args) {
		
	}
}
