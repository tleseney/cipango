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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.cipango.diameter.base.Common;
import org.junit.Test;

public class CodecTest
{
	@Test
	public void testSize() throws Exception
	{
		ByteBuffer buffer = ByteBuffer.allocate(1);
		for (int i = 0; i < 10000; i++)
		{
			buffer = Common.__unsigned32.encode(buffer, i);
		}
		buffer.flip();
		
		for (int i = 0; i < 10000; i++)
		{
			assertTrue(buffer.hasRemaining());
			assertEquals(i, (int) Common.__unsigned32.decode(buffer)); 
		}
		assertFalse(buffer.hasRemaining());
	}
}
