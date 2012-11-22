package org.cipango.sip;

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Set;

import javax.servlet.sip.SipServletMessage.HeaderForm;
import javax.servlet.sip.URI;

import org.cipango.util.StringUtil;

import static org.cipango.sip.SipHeader.*;

public class SipGenerator 
{
	private Set<SipHeader> _fixedHeaders = EnumSet.of(VIA, FROM, TO, CALL_ID);
	
	public void generateRequest(ByteBuffer buffer, String method, URI requestUri, SipFields sipFields, byte[] content, HeaderForm headerForm)
	{
		generateRequestLine(buffer, method, requestUri);
		generateHeaders(buffer, sipFields, headerForm);
		if (content != null)
			buffer.put(content);
	}
	
	public void generateResponse(ByteBuffer buffer, int status, String reason, SipFields sipFields, byte[] content, HeaderForm headerForm)
	{
		generateResponseLine(buffer, status, reason);
		generateHeaders(buffer, sipFields, headerForm);
		if (content != null)
			buffer.put(content);
	}
	
	public void generateRequestLine(ByteBuffer buffer,String method, URI requestUri)
	{
		buffer.put(StringUtil.getUtf8Bytes(method));
		buffer.put(SipGrammar.SPACE);
        buffer.put(StringUtil.getUtf8Bytes(requestUri.toString()));
		buffer.put(SipGrammar.SPACE);
		buffer.put(SipVersion.SIP_2_0.toBuffer());
		buffer.put(SipGrammar.CRLF);
	}
	
	public void generateResponseLine(ByteBuffer buffer, int status, String reason)
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
			buffer.put(SipVersion.SIP_2_0.toBuffer());
			buffer.put(SipGrammar.SPACE);
			buffer.put((byte) ('0' + status / 100));
            buffer.put((byte) ('0' + (status % 100) / 10));
            buffer.put((byte) ('0' + (status % 10)));
            buffer.put(SipGrammar.SPACE);
            if (reason != null)
            	buffer.put(StringUtil.getBytes(reason, StringUtil.__UTF8));
			buffer.put(SipGrammar.CRLF);
		}
	}
	
	private void generateHeaders(ByteBuffer buffer, SipFields sipFields, HeaderForm headerForm)
	{
		if (sipFields != null)
		{
			for (SipFields.Field field : sipFields)
			{
				field.putTo(buffer, headerForm);
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
		int versionLength = SipVersion.SIP_2_0.toString().length();
		
		for (int i = 0; i < __responses.length; i++)
		{
			SipStatus status = SipStatus.get(i);
			if (status == null)
				continue;
			String reason = status.getReason();
			byte[] line = new byte[versionLength+5+reason.length()+2];
			SipVersion.SIP_2_0.toBuffer().get(line, 0, versionLength);
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
