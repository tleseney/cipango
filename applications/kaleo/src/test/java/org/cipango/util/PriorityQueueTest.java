// ========================================================================
// Copyright 2007-2008 NEXCOM Systems
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

package org.cipango.util;

import java.util.Random;

import junit.framework.TestCase;
import org.cipango.util.PriorityQueue.Node;

public class PriorityQueueTest extends TestCase
{
	public void testOne()
	{
		PriorityQueue queue = new PriorityQueue();
		queue.offer(new Node(123));
		assertEquals(123, queue.peek().getValue());
		assertEquals(1, queue.getSize());
		queue.offer(queue.peek(), 1234);
		assertEquals(1234, queue.peek().getValue());
		queue.offer(queue.peek(), 1);
		assertEquals(1, queue.poll().getValue());
		assertEquals(0, queue.getSize());
		
		Node t = new Node(4321);
		queue.offer(t);
		assertEquals(4321, queue.peek().getValue());
		queue.remove(t);
		assertEquals(0, queue.getSize());
		assertEquals(-1, t.getPosition());
		queue.remove(t);
		assertEquals(0, queue.getSize());
	}
	
	public void testOrder()
	{
		Random random = new Random();
		
		for (int i = 0; i < 1000; i++)
		{
			PriorityQueue queue = new PriorityQueue();
			long previous = Long.MIN_VALUE;
			for (int j = 0; j < 1000; j++)
			{
				queue.offer(new Node(random.nextLong()));
			}
			while (queue.getSize() != 0)
			{
				long min = queue.poll().getValue();
				assertTrue(previous <= min);
				previous = min;
			}
		}
	}
	
	public void testPosition()
	{
		Random random = new Random();
		
		for (int i = 0; i < 1000; i++)
		{
			PriorityQueue queue = new PriorityQueue();
			for (int j = 0; j < 1000; j++)
			{
				queue.offer(new Node(random.nextLong()));
			}
			checkPosition(queue.asArray());
		}
	}
	
	private void checkPosition(Node[] tasks)
	{
		for (int k = 0; k < tasks.length; k++)
		{
			assertEquals(k + 1, tasks[k].getPosition());
		}		
	}
	
	public void testReschedule()
	{
		PriorityQueue queue = new PriorityQueue();
		for (int i = 50; i-->0;)
		{
			queue.offer(new Node(i));
		}
		Node[] tasks = queue.asArray();
		for (int i = 0; i < tasks.length; i++)
		{
			queue.offer(tasks[i], 100 - tasks[i].getValue());
		}
		checkPosition(queue.asArray());
		int time = 51;
		while (queue.getSize() > 0)
		{
			assertEquals(time++, queue.poll().getValue());
		}
	}
	
	public void testReschedule2()
	{
		Random random = new Random();
		
		for (int i = 0; i < 1000; i++)
		{
			PriorityQueue queue = new PriorityQueue();
			long previous = Long.MIN_VALUE;
			for (int j = 0; j < 1000; j++)
			{
				queue.offer(new Node(random.nextLong()));
			}
			for (int j = 0; j < 10; j++)
			{
				queue.offer(queue.asArray()[random.nextInt(1000)], random.nextLong());
			}
			
			while (queue.getSize() != 0)
			{
				long min = queue.poll().getValue();
				assertTrue(previous <= min);
				previous = min;
			}
		}
	}
	
	public void testRemove()
	{
		PriorityQueue queue = new PriorityQueue();
		for (int i = 0; i < 100; i++)
		{
			queue.offer(new Node(i));
		}
		Node[] tasks = queue.asArray();
		for (int i = 0; i < tasks.length; i++)
		{
			if (tasks[i].getValue() % 2 == 0)
			{
				queue.remove(tasks[i]);
				assertEquals(-1, tasks[i].getPosition());
			}
		}
		tasks = queue.asArray();
		checkPosition(tasks);
		long min = -1;
		while (queue.getSize() > 0)
		{
			assertTrue(min <= queue.peek().getValue());
			min = queue.peek().getValue();
			assertEquals(1, queue.poll().getValue() % 2);
		}
	}
}
