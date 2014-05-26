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
package org.cipango.server.transaction;

import java.io.IOException;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.sip.SipServletResponse;

import org.cipango.server.SipConnection;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.dns.BlackList.Reason;
import org.cipango.server.dns.Hop;
import org.cipango.server.transaction.ClientTransactionImpl.TimeoutConnection;
import org.cipango.server.util.ClientTransactionProxy;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Transaction manager which support RFC 3263.
 * If a failure described in RFC 3263 §4.3 occurs, the request is sent in a new transaction.
 *
 */
@ManagedObject
public class RetryableTransactionManager extends TransactionManager
{

	private static final Logger LOG = Log.getLogger(RetryableTransactionManager.class);
	private long _maxRetryTime = -1;

	private final AtomicLong _retries = new AtomicLong();
	
	@Override
	protected ClientTransaction newClientTransaction(SipRequest request, ClientTransactionListener listener)
	{
		RetryableClientTransaction ctx = new RetryableClientTransaction(request, listener);
		return ctx;
	}

	@ManagedAttribute("Max retry time")
	public long getMaxRetryTime()
	{
		return _maxRetryTime;
	}

	/**
	 * Returns the max amount of time allowed for all the client transactions in case of
	 * retry. 
	 * The associated timer is canceled on received response and (re)start on a retry.
	 */
	public void setMaxRetryTime(long maxRetryTime)
	{
		_maxRetryTime = maxRetryTime;
	}
	
	@ManagedAttribute("Number of retries")
	public long getRetries()
	{
		return _retries.get();
	}
	
	public class RetryableClientTransaction extends ClientTransactionProxy implements ClientTransaction
	{
		
		private ClientTransaction _activeTx;
		private ClientTransactionListener _listener;
		private Listener _localListener;
		private boolean _isRetrying = false;
		private long _start;
		private TimerTask _retryTask;
		
		
		
		public RetryableClientTransaction(SipRequest request, ClientTransactionListener listener)
		{
			_listener = listener;
			_localListener = new Listener();
			_activeTx = newClientTransaction(request, false);
			_start = System.currentTimeMillis();
			
		}
		
		protected ClientTransaction newClientTransaction(SipRequest request, boolean isRetry)
		{
			if (isRetry)
				_retries.incrementAndGet();
			
			ClientTransaction oldTx = null;
			do
			{
				_activeTx = new ClientTransactionImpl(request, _localListener);
				
				if (isRetry)
				{
					_activeTx.setTransactionManager(RetryableTransactionManager.this);
		
					if (!request.isAck())
						oldTx = addClientTransaction(this);
					
					if (getMaxRetryTime() != -1 && _retryTask == null)
					{
						long delay = getMaxRetryTime() - (System.currentTimeMillis() - _start);
												
						_retryTask = schedule(new RetryTimeout(), delay);
					}
				}
			}
			while (oldTx != null);
			
			return _activeTx;
		}
		
		protected Reason isRetryable(SipResponse response)
		{
			int status = response.getStatus();
			if (isCanceled() || isCancel())
				return null;
			if (status == SipServletResponse.SC_SERVICE_UNAVAILABLE)
				return Reason.RESPONSE_CODE_503;
			else if (status == SipServletResponse.SC_REQUEST_TIMEOUT && response.getConnection() instanceof TimeoutConnection)
				return Reason.TIMEOUT;	// TODO do not select in case of Timer C timeout
			else
				return null;
		}
		
		public synchronized void start()
		{
			while (true)
			{
				try
				{
					_activeTx.start();
					return;
				}
				catch (IOException e)
				{
					ListIterator<Hop> hops = getRequest().getHops();
					if (hops == null)
					{
						LOG.debug(e.getCause()); // Could happens in case of UnknownHostException
						return;
					}
					else
					{	
						// Notify black list
						if (hops.hasPrevious()) 
						{
							Hop failedHop = hops.previous();
							getTransportProcessor().getBlackList().hopFailed(failedHop, Reason.CONNECT_FAILED, null);
							hops.next(); // Set iterator to initial value
							if (hops.hasNext())
								LOG.warn("Could not send request using hop {} due to {}, try with next hop", failedHop, e.getCause());
							else
								LOG.warn("Could not send request using hop {} due to {} and there is no more hops", failedHop, e.getCause());
						}				
		
						if (hops.hasNext())
						{
							_isRetrying = true;
							try
							{
								_activeTx.terminate();
								_activeTx = newClientTransaction(getRequest(), true);
							}
							finally
							{
								_isRetrying = false;
							}
						}
						else
						{
							LOG.debug(e.getCause());
							return;
						}
					}
				}
			}
		}
		
		@Override
		protected ClientTransaction getTransaction()
		{
			return _activeTx;
		}
		
		protected boolean retry(SipResponse response, Reason reason)
		{
			ListIterator<Hop> hops = getRequest().getHops();
			
			if (hops.hasPrevious())
			{
				Hop failedHop = hops.previous();
				getTransportProcessor().getBlackList().hopFailed(failedHop, reason, response);
				hops.next();
			}
			
			if (hops.hasNext())
			{
				try
				{
					LOG.debug("Retrying to send request on session {}", this);
					getRequest().removeTopVia();
					_activeTx = newClientTransaction(getRequest(), true);
					start();
					return true;
				}
				catch (Exception e)
				{
					LOG.debug("Failed to send request to another hop", e);
				}
			}
			LOG.debug("Could not retry to send request on session {} as there is no more hop", this);
			return false;
		}
		
		@Override
		public ClientTransactionListener getListener()
		{
			return _listener;
		}

		@Override
		public boolean isProcessingResponse() 
		{
			return _activeTx.isProcessingResponse();
		}
		
		@Override
		public String toString()
		{
			return "Retryable" + _activeTx;
		}
				
		class RetryTimeout implements Runnable
		{

			@Override
			public void run()
			{
				LOG.warn("Generate localy 408 Request Timeout due to retry timeout");
				SipResponse responseB = create408();
	            if (!isCancel())
	                _listener.handleResponse(responseB);
				terminate();
			}
			
		}

		class Listener implements ClientTransactionListener
		{

			@Override
			public void transactionTerminated(Transaction transaction)
			{
				if (transaction.isCancel())
					_listener.transactionTerminated(transaction);
				else if (!_isRetrying && transaction == _activeTx)
					_listener.transactionTerminated(RetryableClientTransaction.this);
				else
					LOG.warn("Ignore event transaction terminated({}) as {}", transaction,
							_isRetrying ? "a new transaction will be created" : "this transaction is no more active");
			}

			@Override
			public void handleResponse(SipResponse response)
			{
				Reason reason = isRetryable(response);
				if (reason != null)
				{
					LOG.debug("Response is retryable for reason {} on session {}", reason, getRequest().session());
					if (retry(response, reason))
						return;
				}
				
				if (_retryTask != null)
				{
					_retryTask.cancel();
					_retryTask = null;
				}
				
				_listener.handleResponse(response);
			}

			@Override
			public void customizeRequest(SipRequest request, SipConnection connection)
			{
				_listener.customizeRequest(request, connection);
			}
			
		}

	}
	
}
