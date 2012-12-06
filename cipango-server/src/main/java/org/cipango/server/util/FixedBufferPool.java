// ========================================================================
// Copyright 2012 NEXCOM Systems
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
package org.cipango.server.util;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.BufferUtil;

public class FixedBufferPool implements ByteBufferPool
{
	
	private final Queue<ByteBuffer> _directQueue= new ConcurrentLinkedQueue<ByteBuffer>();
	private final Queue<ByteBuffer> _indirectQueue= new ConcurrentLinkedQueue<ByteBuffer>();
	private int _size;
	
	
	public FixedBufferPool(int size)
	{
		_size = size;
	}
	
	@Override
	public ByteBuffer acquire(int size, boolean direct)
	{
		 ByteBuffer buffer = direct ? _directQueue.poll():_indirectQueue.poll();

        if (buffer == null)
        {
            buffer = direct ? BufferUtil.allocateDirect(_size) : BufferUtil.allocate(_size);
        }

        return buffer;
	}

	@Override
	public void release(ByteBuffer buffer)
	{
		if (buffer != null)
        {    
            if (buffer.isDirect())
            	_directQueue.offer(buffer);
            else
            	_indirectQueue.offer(buffer);
        }
	}
}
