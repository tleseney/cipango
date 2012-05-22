package org.cipango.util;

import static junit.framework.Assert.assertTrue;

import java.util.PriorityQueue;
import java.util.Random;

import org.junit.Test;

public class TimerTaskTest 
{
	@Test
	public void testOrder()
	{
		PriorityQueue<TimerTask> queue = new PriorityQueue<TimerTask>();
		
		Random random = new Random();
		for (int i = 0; i < 10; i++)
		{
			queue.offer(new TimerTask(null, Math.abs(random.nextLong())));
		}
		
		long time = -1;
		
		while (!queue.isEmpty())
		{
			TimerTask task = queue.poll();
			assertTrue(task.getExecutionTime() >= time);
			time = task.getExecutionTime();
		}
		
		/*
		time = Math.abs(random.nextLong());
		
		TimerTask task = null;
		while ((task = list.getExpired(time)) != null)
		{
			assertTrue(task.getExecutionTime() <= time);
		}
		
		while (list.size() > 0)
		{
			assertTrue(list.remove(0).getExecutionTime() > time);
		}
		*/
	} 
}
