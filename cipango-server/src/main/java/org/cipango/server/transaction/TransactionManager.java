package org.cipango.server.transaction;

import java.util.concurrent.ConcurrentHashMap;

import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.processor.SipProcessorWrapper;
import org.cipango.util.TimerQueue;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class TransactionManager extends SipProcessorWrapper
{
	private final Logger LOG = Log.getLogger(TransactionManager.class);
	
	private ConcurrentHashMap<String, ServerTransaction> _serverTransactions = new ConcurrentHashMap<String, ServerTransaction>();
	private TimerQueue<TimerTask> _timerQueue = new TimerQueue<TransactionManager.TimerTask>();
	
	public void doProcess(SipMessage message) throws Exception
	{
		if (message.isRequest())
			doProcessRequest((SipRequest) message);
		else
			;
	}
	
	public void doStart()
	{
		new Thread(new Watcher()).start();
		new Thread(new Timer()).start();
	}
	
	public void doProcessRequest(SipRequest request) throws Exception
	{
		LOG.debug("handling transaction message");

		String branch = request.getTopVia().getBranch();
		
		ServerTransaction transaction = _serverTransactions.get(branch);
		if (transaction == null && !request.isAck())
		{
			ServerTransaction newTransaction = new ServerTransaction(request);
			newTransaction.setTransactionManager(this);
			
			transaction = _serverTransactions.putIfAbsent(branch, newTransaction);
		}
		
		if (transaction != null)
			transaction.handleRequest(request);
		else
			super.doProcess(request);
	}
	
	public void transactionTerminated(ServerTransaction transaction)
	{
		_serverTransactions.remove(transaction.getBranch());
	}
	
	public TimerTask schedule(Runnable runnable, long delay)
	{
		TimerTask task = new TimerTask(runnable, System.currentTimeMillis() + delay);
		synchronized (_timerQueue)
		{
			_timerQueue.offer(task);
			_timerQueue.notifyAll();
		}
		return task;
	}
	
	class Timer implements Runnable
	{
		public void run()
		{
			do
			{
				try
				{	
					TimerTask task;
					long delay;
					
					synchronized (_timerQueue)
					{
						task = _timerQueue.peek();
						delay = task != null ? task.getValue() - System.currentTimeMillis() : Long.MAX_VALUE;
						
						if (delay > 0)
							_timerQueue.wait(delay); 
						else
							_timerQueue.poll();
					}
					if (delay <= 0)
						task._runnable.run();
				}
				catch (InterruptedException e) { continue; }
			}
			while (isRunning());
		}
	}
	
	public class TimerTask extends TimerQueue.Node	
	{
		private Runnable _runnable;
		
		public TimerTask(Runnable runnable, long executionTime)
		{
			super(executionTime);
			_runnable = runnable;
		}
		
		public void cancel()
		{
			synchronized (_timerQueue)
			{
				_timerQueue.remove(this);
			}
		}
	}
	
	class Watcher implements Runnable
	{
		public void run()
		{
			while(isRunning())
			{
				try { Thread.sleep(5000); } catch (Exception e) {}
				LOG.info("transactions size: " + _serverTransactions.size());
			}
		}
	}
	
}
