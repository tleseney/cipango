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

import java.io.IOException;
import java.nio.ByteBuffer;

import org.cipango.diameter.AVP;
import org.cipango.diameter.ims.Cx;
import org.junit.Test;

public class AVPCodecTest
{
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testAVPCodec() throws IOException
	{
		AVP avp = new AVP(Cx.PUBLIC_IDENTITY, "sip:alice@cipango.org");
		ByteBuffer buffer = ByteBuffer.allocate(64);
		Codecs.__avp.encode(buffer, avp);
		buffer.flip();
	
		AVP decoded = Codecs.__avp.decode(buffer);
		
		assertEquals(avp.getType().getCode(), decoded.getType().getCode());
		assertEquals(avp.getType().getVendorId(), decoded.getType().getVendorId());
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testPadding() throws IOException
	{
		byte[] b = { 13 };
		AVP<byte[]> avp = new AVP<byte[]>(Cx.INTEGRITY_KEY, b);
		ByteBuffer buffer = ByteBuffer.allocate(64);
		for (int i = 0; i < 64; i++)
			buffer.put((byte) 44);
		buffer.position(0);
		Codecs.__avp.encode(buffer, avp);
		
		ByteBuffer view = buffer.duplicate();
		view.position(view.position() - 3);
		for (int i = 0; i < 3; i++)
			assertEquals(0, view.get());
		
		buffer.flip();
		AVP<byte[]> decoded = (AVP<byte[]>) Codecs.__avp.decode(buffer);
		
		assertEquals(avp.getType().getCode(), decoded.getType().getCode());
		assertEquals(avp.getType().getVendorId(), decoded.getType().getVendorId());
		assertEquals(avp.getValue()[0], decoded.getValue()[0]);
		
	}
}
