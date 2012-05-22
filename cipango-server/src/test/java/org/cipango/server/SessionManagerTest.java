package org.cipango.server;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.cipango.server.session.CallSessionManager;
import org.cipango.server.session.CallSessionManager.SessionScope;
import org.junit.Test;

public class SessionManagerTest 
{
	private AtomicInteger n = new AtomicInteger();
	
	@Test
	public void testSession() throws Exception
	{
		CallSessionManager manager = new CallSessionManager();
		manager.start();
		
		for (int i = 0; i < 10000; i++)
		{
		SessionScope scope =  manager.openScope("test" + i);
		assertNotNull(scope.getCallSession());
		final int k = i;
		scope.getCallSession().schedule(new Runnable() { 
			public void run() { System.out.println("hello world " + k); n.incrementAndGet(); }
		}, 100+i/3);
		
		scope.close();
		}
		
		Thread.sleep(5000);
		System.out.println(n);
	}
}
