package org.cipango.server.transaction;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.sip.SipServletResponse;

import org.cipango.server.SipMessage;
import org.cipango.server.SipProxy;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.processor.SipProcessorWrapper;
import org.cipango.server.processor.TransportProcessor;
import org.cipango.sip.SipGrammar;
import org.cipango.util.TimerQueue;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.Dumpable;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.statistic.CounterStatistic;

@ManagedObject("Transaction manager")
public class TransactionManager extends SipProcessorWrapper implements Dumpable
{
	private final Logger LOG = Log.getLogger(TransactionManager.class);

	private static final String CANCEL_PREFIX = "cancel-";
	
	private ConcurrentHashMap<String, ServerTransaction> _serverTransactions = new ConcurrentHashMap<String, ServerTransaction>();
	private ConcurrentHashMap<String, ClientTransaction> _clientTransactions = new ConcurrentHashMap<String, ClientTransaction>();
	private TimerQueue<TimerTask> _timerQueue = new TimerQueue<TransactionManager.TimerTask>();
	private TransportProcessor _transportProcessor;
	
	private final CounterStatistic _serverTxStats = new CounterStatistic();
	private final CounterStatistic _clientTxStats = new CounterStatistic();
	
	public void setTransportProcessor(TransportProcessor processor)
	{
		_transportProcessor = processor;
	}
	
	
	public void doProcess(SipMessage message) throws Exception
	{
		if (message.isRequest())
			doProcessRequest((SipRequest) message);
		else
			doProcessResponse((SipResponse) message);
	}
	
	public void doStart()
	{
		//new Thread(new Watcher()).start();
		new Thread(new Timer()).start();
	}
	
	public void doProcessRequest(SipRequest request) throws Exception
	{
		String branch = request.getTopVia().getBranch();
		
		if (branch == null || !branch.startsWith(SipGrammar.MAGIC_COOKIE)) 
        {
			// Opensips use 0 as branch
			if (!("0".equals(branch) && request.isAck()))
			{
				LOG.debug("Not 3261 branch: {}. Dropping request", branch);
				return;
			}
		}

		// FIXME handle CANCEL
		if (request.isCancel()) 
			branch = CANCEL_PREFIX + branch;
		
		ServerTransaction transaction = _serverTransactions.get(branch);
		
		LOG.debug("handling server transaction message with tx {}", transaction);
		
		ServerTransaction newTransaction = null;
		if (transaction == null)
		{
			newTransaction = new ServerTransaction(request);
			newTransaction.setTransactionManager(this);
			
			if (!request.isAck())
			{
				transaction = _serverTransactions.putIfAbsent(branch, newTransaction);
				if (transaction == null)
					_serverTxStats.increment();
				// transaction may be not null on some concurrent access as there
				// is no lock between get and set Tx
			}
		}
		
		if (transaction != null) // retransmission or ACK for negative response
			transaction.handleRequest(request);
		else
		{
			// TODO move to Session
			if (request.isCancel())
			{
				String txBranch = request.getTopVia().getBranch();
				ServerTransaction stx = _serverTransactions.get(txBranch);
				if (stx == null)
				{
					if (LOG.isDebugEnabled())
						LOG.debug("No transaction for cancelled branch {}", txBranch);
					SipResponse unknown = (SipResponse) request
							.createResponse(SipServletResponse.SC_CALL_LEG_DONE);
					newTransaction.send(unknown);
				}
				else
				{
					SipResponse ok = (SipResponse) request.createResponse(SipServletResponse.SC_OK);
					newTransaction.send(ok);
					stx.cancel(request);
				}
			}
			else
				super.doProcess(request);
		}
	}
	
	public void doProcessResponse(SipResponse response) throws Exception
	{
		String branch = response.getTopVia().getBranch();
		
		if (response.isCancel()) 
			branch = CANCEL_PREFIX + branch;
		
		ClientTransaction transaction = _clientTransactions.get(branch);

		LOG.debug("handling client transaction message with tx {}", transaction);
		
		if (transaction == null)
		{
			if (LOG.isDebugEnabled())
				LOG.debug("did not find client transaction for response {}", response);
			
			transactionNotFound();
			return;
		}
		
		response.setRequest(transaction.getRequest());
		transaction.handleResponse(response);
	}
	
	protected void transactionNotFound()
	{
		
		//TODO
	}
	
	private String getId(Transaction transaction)
	{
		if (transaction.isCancel()) 
			return CANCEL_PREFIX +  transaction.getBranch();
		return transaction.getBranch();
	}
	
	public void transactionTerminated(ServerTransaction transaction)
	{
		ServerTransaction tx = _serverTransactions.remove(getId(transaction));
		if (tx != null)
			_serverTxStats.decrement();
	}
	
	public void transactionTerminated(ClientTransaction transaction)
	{
		ClientTransaction tx = _clientTransactions.remove(getId(transaction));
		if (tx != null)
			_clientTxStats.decrement();
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
	

	public TransportProcessor getTransportProcessor()
	{
		return _transportProcessor;
	}
	
	protected ClientTransaction addClientTransaction(ClientTransaction tx)
	{
		ClientTransaction oldTx = _clientTransactions.putIfAbsent(getId(tx), tx);
		if (oldTx != null)
			LOG.warn("Try to add client transaction {} when there is already the transaction {}", tx, oldTx);
		else
			_clientTxStats.increment();
		return oldTx;
	}

	public ClientTransaction sendRequest(SipRequest request, ClientTransactionListener listener)
	{
		ClientTransaction oldTx = null;
		ClientTransaction ctx;
		do
		{
			ctx = new ClientTransaction(request, listener);
			ctx.setTransactionManager(this);

			if (!request.isAck())
				oldTx = addClientTransaction(ctx);
		}
		while (oldTx != null);

		try
		{
			ctx.start();
		}
		catch (IOException e)
		{
			LOG.warn(e);
		}
		return ctx;
	}
	
	@ManagedAttribute("Current active client transactions")
	public long getClientTransactions()
	{
		return _clientTxStats.getCurrent();
	}
	
	@ManagedAttribute("Max simultaneous client transactions")
	public long getClientTransactionsMax()
	{
		return _clientTxStats.getMax();
	}
	
	@ManagedAttribute("Total client transactions")
	public long getClientTransactionsTotal()
	{
		return _clientTxStats.getTotal();
	}
	
	@ManagedAttribute("Current active server transactions")
	public long getServerTransactions()
	{
		return _serverTxStats.getCurrent();
	}
	
	@ManagedAttribute("Max simultaneous server transactions")
	public long getServerTransactionsMax()
	{
		return _serverTxStats.getMax();
	}
	
	@ManagedAttribute("Total server transactions")
	public long getServerTransactionsTotal()
	{
		return _serverTxStats.getTotal();
	}
	
	@ManagedAttribute(value="Timer T1 in milliseconds", readonly=true)
	public int getT1() { return Transaction.__T1; }
	@ManagedAttribute(value="Timer T2 in milliseconds", readonly=true)
	public int getT2() { return Transaction.__T2; }
	@ManagedAttribute(value="Timer T4 in milliseconds", readonly=true)
	public int getT4() { return Transaction.__T4; }
	@ManagedAttribute(value="Timer TD in milliseconds", readonly=true)
	public int getTD() { return Transaction.__TD; }
	@ManagedAttribute(value="Timer C in seconds", readonly=true)
	public int getTimerC() { return SipProxy.__timerC; }
	
	public void setT1(int millis)
	{ 
		if (millis < 0)
			throw new IllegalArgumentException("SIP Timers must be positive");
		Transaction.__T1 = millis;
	}
	
	public void setT2(int millis) 
	{
		if (millis < 0)
			throw new IllegalArgumentException("SIP Timers must be positive");
		Transaction.__T2 = millis;
	}
	
	public void setT4(int millis) 
	{
		if (millis < 0)
			throw new IllegalArgumentException("SIP Timers must be positive");
		Transaction.__T4 = millis;
	}
	
	public void setTD(int millis) 
	{
		if (millis < 0)
			throw new IllegalArgumentException("SIP Timers must be positive");
		Transaction.__TD = millis;
	}
	
	@ManagedOperation(value="Reset statistics", impact="ACTION")
	public void statsReset()
	{
		_serverTxStats.reset(_serverTransactions.size());
		_clientTxStats.reset(_clientTransactions.size());
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

	@Override
	public String dump()
	{
		return ContainerLifeCycle.dump(this);
	}


	@Override
	public void dump(Appendable out, String indent) throws IOException
	{
		out.append(indent).append(" +- TransactionManager\n");
		indent = indent + "    ";
		out.append(indent).append(" +- ClientTransactions\n");
		Iterator<ClientTransaction> it = _clientTransactions.values().iterator();
		int i = 50;
		while (it.hasNext() && --i >0)
			out.append(indent).append(it.next().toString()).append("\n");
		if (it.hasNext())
			out.append(indent).append("...\n");
		
		out.append(indent).append(" +- ServerTransactions\n");
		Iterator<ServerTransaction> it2 = _serverTransactions.values().iterator();
		i = 50;
		while (it2.hasNext() && --i >0)
			out.append(indent).append(it2.next().toString()).append("\n");
		if (it.hasNext())
			out.append(indent).append("...\n");

	}

	
}
