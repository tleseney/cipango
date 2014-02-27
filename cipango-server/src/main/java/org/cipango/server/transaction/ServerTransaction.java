// ========================================================================
// Copyright 2008-2012 NEXCOM Systems
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

import org.cipango.server.SipConnection;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.session.Session;
import org.cipango.server.session.scoped.ScopedServerTransactionListener;
import org.cipango.sip.SipHeader;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class ServerTransaction extends TransactionImpl
{
	private static final Logger LOG = Log.getLogger(ServerTransaction.class);
	
	private SipResponse _latestResponse;
	
	private long _gDelay = __T1;
	private ServerTransactionListener _listener;
	
	public ServerTransaction(SipRequest request)
	{
		super(request, request.getTopVia().getBranch());
		if (isInvite())
			setState(State.PROCEEDING);
		else
			setState(State.TRYING);
	}
	
	public boolean isServer()
	{
		return true;
	}
	
	public SipConnection getConnection()
	{
		return _request.getConnection();
	}
	
	/**
	 * Subsequent request in transaction (retransmission or ACK for negative response)
	 */
	public synchronized void handleRequest(SipRequest request)
	{
		request.setTransaction(this);
		if (request.isAck())
		{
			if (isInvite())
			{
				if (_state != State.COMPLETED)
				{
					LOG.debug("received ACK in non-completed state for transaction {}", this);
					return;
				}
				setState(State.CONFIRMED);
				cancelTimer(Timer.H); cancelTimer(Timer.G);
				
				if (isTransportReliable())
					terminate(); // timer I == 0
				else
					startTimer(Timer.I, __T4);
			}
			else
			{
				LOG.debug("received ACK for non-INVITE transaction: {}", this);
			}
		}
		else
		{
			if (!request.getHeader(SipHeader.CSEQ.toString()).equals(_request.getHeader(SipHeader.CSEQ.toString())))
			{
				LOG.debug("invalid retransmission {}", this);
				return;
			}
			if (_state == State.PROCEEDING || _state == State.COMPLETED) 
			{
				if (_latestResponse != null)
				{
					try
					{
						doSend(_latestResponse);
					}
					catch (IOException e)
					{
						LOG.debug(e);
					}
				}
			}
		}
	}
	
	public void cancel(SipRequest cancel) throws IOException
	{
		if (_listener == null) { // Case CANCEL received before session mode choose
    		Session session = _request.session();
    		if (session == null) {
    			LOG.warn("No transaction listener set on {}. Could not handle:\n{}", this, cancel);
    			return;
    		}
    		
        	if (session.getUa() == null)
    			session.setUAS();
			setListener(new ScopedServerTransactionListener(session, session.getUa()));
    	}
		
		_listener.handleCancel(this, cancel);
	}
	
	/**
	 * Sends a response within this transaction
	 */
	public synchronized void send(SipResponse response)
	{
		int status = response.getStatus();
		
		if (isInvite())
		{
			switch (_state)
			{
			case PROCEEDING:
				if (status < 200)
				{
					_latestResponse = response;
				}
				else if (status < 300)
				{
					setState(State.ACCEPTED);
					startTimer(Timer.L, 64*__T1);
				}
				else 
				{
					setState(State.COMPLETED);
					_latestResponse = response;
					
					if (isTransportReliable())
						startTimer(Timer.G, _gDelay);
						
					startTimer(Timer.H, 64*__T1); 
				}
				
				break;
				
			case ACCEPTED:
				if (status < 200 || status >= 300)
					throw new IllegalStateException("accepted && !2xx");
				break;
			default:
				throw new IllegalStateException("!proceeding && send(invite)");
			}
		}
		else
		{
			switch (_state)
			{
			case TRYING:
			case PROCEEDING:
				_latestResponse = response;
				if (status < 200)
				{
					if (_state == State.TRYING)
						setState(State.PROCEEDING);
				}
				else
				{
					setState(State.COMPLETED);
					if (isTransportReliable())
						terminate(); // TIMER_J = 0
					else
						startTimer(Timer.J, 64*__T1);
				}
				break;
			default:
				throw new IllegalStateException("state != trying||proceeding && send(non-invite)");
			}
		}
		
		try
		{
			doSend(response);
		}
		catch (IOException e)
		{
			LOG.debug(e);
		}
	}
	
	protected void terminate()
	{
		super.terminate();
		_latestResponse = null;
		
		_transactionManager.transactionTerminated(this);
		if (_listener != null)
			_listener.transactionTerminated(this);
	}
	
	private void doSend(SipResponse response) throws IOException
	{
		getServer().sendResponse(response, getConnection());
	}
	
	protected synchronized void timeout(Timer timer)
	{
		if (isCanceled(timer))
		{
			LOG.debug("Do not run timer {} on transaction {} as it is canceled ", timer, this);
			return;
		}
		
		switch(timer)
		{
			case G:
				try { doSend(_latestResponse); } catch (IOException e) { LOG.debug(e); }
				_gDelay = _gDelay * 2;
				startTimer(Timer.G, Math.min(_gDelay, __T2));
				break;
			case H: // TODO noAck ?
				cancelTimer(Timer.G);
				terminate();
				break;
			case I:
			case J:
			case L:
				terminate();
				break;
			default:
				throw new IllegalArgumentException("unknown timer in server transaction: " + timer);
		}
	}

	public ServerTransactionListener getListener()
	{
		return _listener;
	}

	public void setListener(ServerTransactionListener listener)
	{
		_listener = listener;
	}
	
	@Override
	public String toString()
	{
		return "ServerTransaction {branch=" + getBranch() 
				+ ", method=" + getRequest().getMethod()
				+ ", state=" + getState() + "}";
	}
}
