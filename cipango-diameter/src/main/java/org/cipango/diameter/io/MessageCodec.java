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
package org.cipango.diameter.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.cipango.diameter.DiameterCommand;
import org.cipango.diameter.Dictionary;
import org.cipango.diameter.Factory;
import org.cipango.diameter.base.Common;
import org.cipango.diameter.node.DiameterAnswer;
import org.cipango.diameter.node.DiameterMessage;
import org.cipango.diameter.node.DiameterRequest;

/**
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ 
 * |    Version    |                 Message Length                |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ 
 * | command flags |                  Command-Code                 |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ 
 * |                         Application-ID                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ 
 * |                      Hop-by-Hop Identifier                    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ 
 * |                      End-to-End Identifier                    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ 
 * |  AVPs ... 
 * +-+-+-+-+-+-+-+-+-+-+-+-+-
 * </pre>
 */
public class MessageCodec extends AbstractCodec<DiameterMessage>
{
	public static final int DIAMETER_VERSION_1 = 1;
	public static final int REQUEST_FLAG = 0x80;
	public static final int PROXIABLE_FLAG = 0x40;
	
	public DiameterMessage decode(ByteBuffer buffer) throws IOException
	{
		int i = buffer.getInt();
		int version = i >> 24 & 0xff;
		
		if (version != DIAMETER_VERSION_1)
			throw new IOException("Unsupported diameter version: " + version);
		
		i = buffer.getInt();
		
		int flags = i >> 24 & 0xff;
		boolean isRequest = ((flags & REQUEST_FLAG) == REQUEST_FLAG);
		
		int code = i & 0xffffff;
		
		Dictionary dictionary = Dictionary.getInstance();
		
		DiameterCommand command = isRequest ? dictionary.getRequest(code) : dictionary.getAnswer(code);
		if (command == null)
			command = isRequest ? Factory.newRequest(code, "Unknown") : Factory.newAnswer(code, "Unknown");
		
		DiameterMessage message = isRequest ? new DiameterRequest() : new DiameterAnswer();
		
		message.setApplicationId(buffer.getInt());
		message.setHopByHopId(buffer.getInt());
		message.setEndToEndId(buffer.getInt());
		message.setCommand(command);
		
		if (isRequest)
			((DiameterRequest) message).setUac(false);
		
		message.setAVPList(Common.__grouped.decode(buffer));
		return message;
	}
	
	public ByteBuffer encode(ByteBuffer buffer, DiameterMessage message) throws IOException
	{
		int start = buffer.position();
		buffer.position(start+4);
		
		int flags = 0;
		DiameterCommand command = message.getCommand();
		
		if (command.isRequest())
			flags |= REQUEST_FLAG;
		
		if (command.isProxiable())
			flags |= PROXIABLE_FLAG;
		
		buffer.putInt(flags << 24 | command.getCode() & 0xffffff);
		buffer.putInt(message.getApplicationId());
		buffer.putInt(message.getHopByHopId());
		buffer.putInt(message.getEndToEndId());
		
		buffer = Common.__grouped.encode(buffer, message.getAVPs());
		pokeInt(buffer, start, DIAMETER_VERSION_1 << 24 | (buffer.position() - start) & 0xffffff);
		
		return buffer;
	}
}
