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

import java.nio.ByteBuffer;


public abstract class AbstractCodec<T> implements DiameterCodec<T> 
{
	public ByteBuffer ensureSpace(ByteBuffer buffer, int space)
	{
		if (buffer.remaining() < space)
		{
			while (space < (buffer.capacity() / 2) && space < 128)
				space *= 2;
			ByteBuffer larger = ByteBuffer.allocate(buffer.capacity() + space);
			buffer.flip();
			larger.put(buffer);
			larger.position(buffer.position());
			return larger;
		}
		return buffer;
	}
		
	public ByteBuffer pokeInt(ByteBuffer buffer, int index, int value)
	{
		buffer.put(index, (byte)  (value >> 24 & 0xff));
		buffer.put(index+1, (byte)  (value >> 16 & 0xff));
		buffer.put(index+2, (byte)  (value >> 8 & 0xff));
		buffer.put(index+3, (byte)  (value & 0xff));
		return buffer;
	}
}
