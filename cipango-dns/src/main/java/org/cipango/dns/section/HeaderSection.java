// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================
package org.cipango.dns.section;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.cipango.dns.DnsMessage;
import org.cipango.dns.util.BufferUtil;

/**
 *                                   1  1  1  1  1  1
 *     0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                      ID                       |
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |QR|   Opcode  |AA|TC|RD|RA|   Z    |   RCODE   |
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                    QDCOUNT                    |
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                    ANCOUNT                    |
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                    NSCOUNT                    |
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                    ARCOUNT                    |
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 */
public class HeaderSection
{
	private static final int RESPONSE_FLAG = 0x8000;
	
	private static final int AUTHORITATIVE_ANSWER_FLAG = 0x400;
	private static final int TRUNCATION_FLAG = 0x200;
	private static final int RECURSION_DESIRED_FLAG = 0x0100;
	private static final int RECURSION_AVAILABLE_FLAG = 0x0080;
	
	public enum ResponseCode
	{
		/**
		 * No error condition
		 */
        NO_ERROR,               
		/**
		 * The name server was unable to interpret the query.
		 */
        FORMAT_ERROR,
		/**
		 * The name server was unable to process this query due to a problem with the name server.
		 */
        SERVER_FAILURE,
		/**
		 * Meaningful only for responses from an authoritative name server, this code signifies that
		 * the domain name referenced in the query does not exist.
		 */
        NAME_ERROR,              
		/**
		 * The name server does not support the requested kind of query.
		 */
        NOT_IMPLEMEMENTED,  
		/**
		 * The name server refuses to perform the specified operation for policy reasons. For
		 * example, a name server may not wish to provide the information to the particular
		 * requester, or a name server may not wish to perform a particular operation (e.g., zone
		 * transfer) for particular data.
		 */
        REFUSED,
        /**
         *  Reserved for future use.
         */
        CODE_6,
        CODE_7,
        CODE_8,
        CODE_9,
        CODE_10,
        CODE_11,
        CODE_12,
        CODE_13,
        CODE_14,
        CODE_15;      
        
        public static ResponseCode get(int value) throws IOException
		{
			for (ResponseCode code : ResponseCode.values())
				if (code.ordinal() == value)
					return code;
			throw new IOException("Could not found ResponseCode with value: " + value);
		}
	}
	
	private DnsMessage _message;
	private int _id;
	private OpCode _opCode;
	private boolean _authoritativeAnswer;
	private boolean _truncated;
	private boolean _recursionDesired = true;
	private boolean _recursionAvailable;
	private ResponseCode _responseCode;
	private int _questionRecords;
	private int _answerRecords;
	private int _authorityRecords;
	private int _additionalRecords;
	private boolean _response = false;
	
	public enum OpCode 
	{
		QUERY, IQUERY, STATUS;
		
		public static OpCode get(int value) throws IOException
		{
			for (OpCode code : OpCode.values())
				if (code.ordinal() == value)
					return code;
			throw new IOException("Could not found OpCode with value: " + value);
		}
	}


	public HeaderSection(DnsMessage message)
	{
		_message = message;
	}

	public void encode(ByteBuffer buffer) throws IOException
	{
		BufferUtil.put16(buffer, _id);
		int flags = 0;
		if (_response)
		{
			flags |= RESPONSE_FLAG;
			if (_authoritativeAnswer)
				flags |= AUTHORITATIVE_ANSWER_FLAG;

			if (_responseCode != null)
				flags |= _responseCode.ordinal();
		}
		flags |= _opCode.ordinal() << 11;
		
		if (_truncated)
			flags |= TRUNCATION_FLAG;
		
		if (_recursionDesired)
			flags |= RECURSION_DESIRED_FLAG;
		
		if (_recursionAvailable)
			flags |= RECURSION_AVAILABLE_FLAG;
		
		BufferUtil.put16(buffer, flags);
		
		BufferUtil.put16(buffer, getMessage().getQuestionSection().size());
		BufferUtil.put16(buffer, getMessage().getAnswerSection().size());
		BufferUtil.put16(buffer, getMessage().getAuthoritySection().size());
		BufferUtil.put16(buffer, getMessage().getAdditionalSection().size());
	}

	public void decode(ByteBuffer buffer) throws IOException
	{
		_id = BufferUtil.get16(buffer);
		int flags = BufferUtil.get16(buffer);
		_response = ((flags & RESPONSE_FLAG) == RESPONSE_FLAG);
		
		_opCode = OpCode.get((flags >> 11) & 0xF);
		
		_authoritativeAnswer = _response && ((flags & AUTHORITATIVE_ANSWER_FLAG) == AUTHORITATIVE_ANSWER_FLAG);
		_truncated = ((flags & TRUNCATION_FLAG) == TRUNCATION_FLAG);
		_recursionDesired = ((flags & RECURSION_DESIRED_FLAG) == RECURSION_DESIRED_FLAG);
		_recursionAvailable = ((flags & RECURSION_AVAILABLE_FLAG) == RECURSION_AVAILABLE_FLAG);
		
		if (_response)
			_responseCode = ResponseCode.get(flags & 0xF);
		
		_questionRecords = BufferUtil.get16(buffer);
		_answerRecords = BufferUtil.get16(buffer);
		_authorityRecords = BufferUtil.get16(buffer);
		_additionalRecords = BufferUtil.get16(buffer);	
	}

	public int getId()
	{
		return _id;
	}

	public void setId(int id)
	{
		_id = id;
	}

	public OpCode getOpCode()
	{
		return _opCode;
	}

	public void setOpCode(OpCode opCode)
	{
		_opCode = opCode;
	}

	public boolean isAuthoritativeAnswer()
	{
		return _authoritativeAnswer;
	}

	public void setAuthoritativeAnswer(boolean authoritativeAnswer)
	{
		_authoritativeAnswer = authoritativeAnswer;
	}

	public boolean isTruncated()
	{
		return _truncated;
	}

	public void setTruncated(boolean truncated)
	{
		_truncated = truncated;
	}

	public boolean isRecursionDesired()
	{
		return _recursionDesired;
	}

	public void setRecursionDesired(boolean recursionDesired)
	{
		_recursionDesired = recursionDesired;
	}

	public boolean isRecursionAvailable()
	{
		return _recursionAvailable;
	}

	public void setRecursionAvailable(boolean recursionAvailable)
	{
		_recursionAvailable = recursionAvailable;
	}

	public ResponseCode getResponseCode()
	{
		return _responseCode;
	}

	public void setResponseCode(ResponseCode responseCode)
	{
		_responseCode = responseCode;
	}

	public int getQuestionRecords()
	{
		return _questionRecords;
	}

	public int getAnswerRecords()
	{
		return _answerRecords;
	}

	public int getAuthorityRecords()
	{
		return _authorityRecords;
	}

	public int getAdditionalRecords()
	{
		return _additionalRecords;
	}
	
	public DnsMessage getMessage()
	{
		return _message;
	}

	public StringBuilder append(StringBuilder sb)
	{
		if (_responseCode != null)
			sb.append("Answer ").append(_id).append(" ").append(_responseCode);
		else
			sb.append("Query ").append(_id);
		sb.append('\n');
		return sb;
	}

	public boolean isResponse()
	{
		return _response;
	}

	public void setResponse(boolean response)
	{
		_response = response;
	}
}
