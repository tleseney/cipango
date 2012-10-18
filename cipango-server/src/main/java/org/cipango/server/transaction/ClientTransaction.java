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
import java.net.InetAddress;
import java.nio.ByteBuffer;

import javax.servlet.sip.Address;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import org.cipango.server.SipConnection;
import org.cipango.server.SipConnector;
import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.Transport;
import org.cipango.sip.SipHeader;
import org.cipango.sip.Via;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;


/**
 * INVITE and non-INVITE client transaction. 
 * Supports RFC 6026.
 */
public class ClientTransaction extends Transaction 
{
	private static final Logger LOG = Log.getLogger(ClientTransaction.class);
	
	private long _aDelay = __T1;
    private long _eDelay = __T1;
    
    private ClientTransactionListener _listener;
    private SipRequest _pendingCancel;
    
    private boolean _canceled = false;
    
    private SipConnection _connection;
    
	public ClientTransaction(SipRequest request, ClientTransactionListener listener)
    {
		this(request, listener, request.appSession().newBranch());
	}
	
	public ClientTransaction(SipRequest request, ClientTransactionListener listener, String branch) 
    {
		super(request, branch);
        _listener = listener;
	}
	
	public ClientTransactionListener getListener()
	{
		return _listener;
	}
	
	private void ack(SipResponse response) 
    {
//	FIXME	SipRequest ack = _request.createRequest(SipMethod.ACK);
//		
//		if (ack.to().getParameter("tag") == null) 
//        {
//			String tag = response.to().getParameter("tag");
//			if (tag != null) 
//				ack.to().setParameter("tag", tag);
//		}
//		try 
//        {
//			getServer().send(ack, getConnection());
//		} 
//        catch (IOException e) 
//        {
//			LOG.ignore(e);
//		}
	}
	
	public void cancel(SipRequest cancel)
	{
		if (_canceled) 
			return;
		
		_canceled = true;
		
		if (_state == State.UNDEFINED || _state == State.CALLING || _state == State.TRYING)
		{
			_pendingCancel = cancel;
			return;
		}
		doCancel(cancel);
	}
	
	public void cancel()
    {   
    	cancel((SipRequest) _request.createCancel());
    }
	
	public boolean isCanceled()
	{
		return _canceled;
	}
	
	private ClientTransaction doCancel(SipRequest cancel)
	{
		ClientTransaction cancelTx = new ClientTransaction(cancel, _listener, cancel.getTopVia().getBranch());
		cancelTx._connection = getConnection();
		
		_transactionManager.addClientTransaction(cancelTx);
		
		try 
        {
			cancelTx.start();
		} 
        catch (IOException e) 
        {
			LOG.warn(e);
		}
        return cancelTx;
	}

	private void doSend() throws IOException 
    {
		if (getConnection() != null)
		{
			if (getConnection().isOpen())
				getServer().send(_request, getConnection());
			else
				LOG.debug("Could not sent request {} as the connection {} is closed", _request, getConnection());
		}
		else 
		{
			// TODO check Maxforwards
			URI uri = null;
			
			Address route = _request.getTopRoute();
			
			if (route != null /* && !_request.isNextHopStrictRouting() */)
				uri = route.getURI();
			else
				uri = _request.getRequestURI();
			
			if (!uri.isSipURI()) 
				throw new IOException("Cannot route on URI: " + uri);
			
			SipURI target = (SipURI) uri;
			
			InetAddress address;
			if (target.getMAddrParam() != null)
				address = InetAddress.getByName(target.getMAddrParam());
			else
				address = InetAddress.getByName(target.getHost()); // TODO 3263
			
			
			Transport transport;
			if (target.getTransportParam() != null)
				transport =Transport.valueOf(target.getTransportParam()); // TODO opt
			else
				transport = Transport.UDP;
			
			int port = target.getPort();
			if (port == -1) 
				port = transport.getDefaultPort();
		

			Via via = new Via(null, null, -1);
			via.setBranch(getBranch());
			//customizeVia(via);
			_request.getFields().add(SipHeader.VIA.asString(), via, true);
			
			_connection = _transactionManager.getTransportProcessor().getConnection(
					_request,
					transport,
					address,
					port);
			_listener.customizeRequest(_request, _connection);
			getServer().send(_request, _connection);

		}
	}
	

	
	public void start() throws IOException 
    {
        if (_state != State.UNDEFINED)
            throw new IllegalStateException("!undefined: " + _state);
        
        if (isInvite()) 
        {
			setState(State.CALLING);
			try
			{
				doSend();
			}
			finally
			{
				startTimer(Timer.B, 64L*__T1);
			}
			if (!isTransportReliable())
				startTimer(Timer.A, _aDelay);
		} 
        else if (isAck()) 
        {
			setState(State.TRYING);
			doSend();
		} 
        else 
        {
			setState(State.TRYING);
			try
			{
				doSend();
			}
			finally
			{
				startTimer(Timer.F, 64L*__T1);
			}
			if (!isTransportReliable()) 
				startTimer(Timer.E, _eDelay);
		}
	}
	
	public void handleResponse(SipResponse response) 
    {
		int status = response.getStatus(); 
        
		if (isInvite()) 
        {
			switch (_state) 
            {
			case CALLING:
				cancelTimer(Timer.A); cancelTimer(Timer.B);
				if (status < 200) 
                {
					setState(State.PROCEEDING);
					if (_pendingCancel != null)
						doCancel(_pendingCancel);
				} 
                else if (200 <= status && status < 300) 
                {
					setState(State.ACCEPTED);
					startTimer(Timer.M, 64L*__T1);
				} 
                else 
                {
					setState(State.COMPLETED);
					ack(response);
					if (isTransportReliable()) 
						terminate();
					else 
						startTimer(Timer.D, __TD);
				}
				_listener.handleResponse(response);
				break;
				
			case PROCEEDING:
				if (200 <= status && status < 300) 
                {
					setState(State.ACCEPTED);
					startTimer(Timer.M, 64L*__T1);
				} 
                else if (status >= 300) 
                {
					setState(State.COMPLETED);
					ack(response);
					if (isTransportReliable()) 
						terminate();
					else 
						startTimer(Timer.D, __TD);
				}
				_listener.handleResponse(response);
				break;
                
			case COMPLETED:
				ack(response);
				break;
			case ACCEPTED:
				if (!(200 <= status && status < 300))
				{
					LOG.debug("non 2xx response {} in Accepted state", response);
				}
				else
				{
					_listener.handleResponse(response);
				}
				break;
			default:
				LOG.debug("handleResponse (invite) && state ==" + _state);
			}
		} 
        else 
        {
			switch (_state) 
            {
			case TRYING:
				if (status < 200) 
                {
					setState(State.PROCEEDING);
				} 
                else 
                {
					cancelTimer(Timer.E); cancelTimer(Timer.F);
					setState(State.COMPLETED);
					if (isTransportReliable()) 
						terminate(); // TIMER_K == 0
					else 
						startTimer(Timer.K, __T4);
				}
                if (!isCancel())
                    _listener.handleResponse(response);
				break;
                
			case PROCEEDING:
				if (status >= 200) 
                {
                    cancelTimer(Timer.E); cancelTimer(Timer.F);
					setState(State.COMPLETED);
					if (isTransportReliable())
						terminate();
					else 
						startTimer(Timer.K, __T4);
                    if (!isCancel())
                        _listener.handleResponse(response);
				}
				break;
				
			case COMPLETED:
				break;
				
			default:
				LOG.warn("handleResponse (non-invite) && state ==" + _state);
			}
		}
	}
	
	public boolean isServer() 
    {
		return false;
	}
	
	public void terminate() 
    {
		setState(State.TERMINATED);
		_transactionManager.transactionTerminated(this);
    }
	
	
	public SipResponse create408()
	{
		// could not use request.createResponse() because the request is committed. 
		SipResponse responseB = new SipResponse(_request, SipServletResponse.SC_REQUEST_TIMEOUT, null);
//FIXME		if (responseB.getTo().getParameter(SipParams.TAG) == null)
//			responseB.setToTag(ID.newTag());
		
//		AccessLog accessLog = getServer().getConnectorManager().getAccessLog();
//		if (accessLog != null)
//			accessLog.messageReceived(responseB, new TimeoutConnection());
		
		return responseB;
	}
	

	@Override
	public SipConnection getConnection()
	{
		return _connection;
	}

	@Override
	protected void timeout(Timer timer)
	{
		switch (timer) 
        {
		case A:
			try 
            {
            	doSend();
			} 
            catch (IOException e) 
            {
				LOG.debug("Failed to (re)send request " + _request);
			}
			_aDelay = _aDelay * 2;
			startTimer(timer.A, _aDelay);
			break;
		case B:
			cancelTimer(Timer.A);
			SipResponse responseB = create408();
			// TODO send to ??
            if (!isCancel())
                _listener.handleResponse(responseB);
			terminate();
            break;
        case D:
            terminate();
            break;
            
        case E:
            try 
            {
                doSend();
            }
            catch (IOException e)
            {
                LOG.debug("Failed to (re)send request " + _request);
            }
            if (_state == State.TRYING)
                _eDelay = Math.min(_eDelay * 2, __T2);
            else
                _eDelay = __T2;
            startTimer(Timer.E, _eDelay);
            break;
        case F:
            cancelTimer(Timer.E);
            SipResponse responseF = create408();
            if (!isCancel())
                _listener.handleResponse(responseF); // TODO interface TU
            terminate();
            break;
        case K:
            terminate();
            break;
        case M:
        	terminate();
        	break;
        default:
            throw new RuntimeException("unknown timer  " + timer);
		}
	}
	
	class TimeoutConnection implements SipConnection
	{
		private SipConnector _connector;
		
		public TimeoutConnection()
		{
			if (getConnection() == null)
				_connector = getServer().getConnectors()[0];
			else
				_connector = getConnection().getConnector();
		}
		
		@Override
		public SipConnector getConnector()
		{			
			
			return _connector;
		}

		@Override
		public InetAddress getLocalAddress()
		{
			return _connector.getAddress();
		}

		@Override
		public int getLocalPort()
		{
			return _connector.getPort();
		}

		@Override
		public InetAddress getRemoteAddress()
		{
			return _connector.getAddress();
		}

		@Override
		public int getRemotePort()
		{
			return _connector.getPort();
		}

		@Override
		public Transport getTransport()
		{
			return _connector.getTransport();
		}

		@Override
		public void send(SipMessage message)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void write(ByteBuffer buffer) throws IOException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isOpen()
		{
			return false;
		}
		
	}

}


