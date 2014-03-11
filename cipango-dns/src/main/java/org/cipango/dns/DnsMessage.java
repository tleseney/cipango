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
package org.cipango.dns;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

import org.cipango.dns.bio.UdpConnector;
import org.cipango.dns.record.OptRecord;
import org.cipango.dns.record.Record;
import org.cipango.dns.section.HeaderSection;
import org.cipango.dns.section.HeaderSection.OpCode;
import org.cipango.dns.section.QuestionSection;
import org.cipango.dns.section.ResourceRecordsSection;

/**
 * Message format:
 * <pre>
 *  +---------------------+
 *  |        Header       |
 *  +---------------------+
 *  |       Question      | the question for the name server
 *  +---------------------+
 *  |        Answer       | RRs answering the question
 *  +---------------------+
 *  |      Authority      | RRs pointing toward an authority
 *  +---------------------+
 *  |      Additional     | RRs holding additional information
 *  +---------------------+
 * </pre>
 */
public class DnsMessage
{
	
	private HeaderSection _headerSection = new HeaderSection(this);
	private QuestionSection _questionSection = new QuestionSection(this);
	private ResourceRecordsSection _answerSection = new ResourceRecordsSection(this);	
	private ResourceRecordsSection _authoritySection = new ResourceRecordsSection(this);	
	private ResourceRecordsSection _additionalSection = new ResourceRecordsSection(this);	
	private Compression _compression = new Compression();
	
	public DnsMessage()
	{
	}
	
	public DnsMessage(Record record)
	{
		_headerSection.setId(ThreadLocalRandom.current().nextInt() & 0xFFFF);
		_headerSection.setOpCode(OpCode.QUERY);
		_questionSection.add(record);
	}

	public void encode(ByteBuffer buffer) throws IOException
	{
		getCompression().clear();
		_headerSection.encode(buffer);
		_questionSection.encode(buffer);
		_answerSection.encode(buffer);
		_authoritySection.encode(buffer);
		_additionalSection.encode(buffer);
	}
	
	public void decode(ByteBuffer buffer) throws IOException
	{
		getCompression().clear();
		_headerSection.decode(buffer);
		_questionSection.decode(buffer);
		_answerSection.decode(buffer, _headerSection.getAnswerRecords());
		_authoritySection.decode(buffer, _headerSection.getAuthorityRecords());
		_additionalSection.decode(buffer, _headerSection.getAdditionalRecords());
	}
	
	public Compression getCompression()
	{
		return _compression;
	}


	public HeaderSection getHeaderSection()
	{
		return _headerSection;
	}


	public QuestionSection getQuestionSection()
	{
		return _questionSection;
	}


	public ResourceRecordsSection getAnswerSection()
	{
		return _answerSection;
	}


	public ResourceRecordsSection getAuthoritySection()
	{
		return _authoritySection;
	}


	public ResourceRecordsSection getAdditionalSection()
	{
		return _additionalSection;
	}
	
	public int getMaxUdpSize() {
		OptRecord record = _additionalSection.get(OptRecord.class);
		if (record != null)
			return record.getMaxPayloadSize();
		return UdpConnector.DEFAULT_MAX_PACKET_SIZE;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		_headerSection.append(sb);
		_questionSection.append(sb);
		_answerSection.append(sb, "Answers");
		_authoritySection.append(sb, "Authority name servers");
		_additionalSection.append(sb, "Additional records");
		return sb.toString();
	}
}
