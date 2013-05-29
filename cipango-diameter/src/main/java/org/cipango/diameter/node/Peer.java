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
package org.cipango.diameter.node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.cipango.diameter.base.Common;
import org.cipango.diameter.base.Common.DisconnectCause;
import org.cipango.diameter.bio.DiameterSocketConnector;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.Dumpable;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 *  A Diameter Peer is a Diameter Node to which a given Diameter Node
 *  has a direct transport connection.
 */
@ManagedObject("Peer")
public class Peer implements Dumpable, PeerStateListener
{
	private static final Logger LOG = Log.getLogger(Peer.class);
	private Node _node;
	
	private String _host;
	private InetAddress _address;
	private int _port;
	
	private volatile State _state;
	
	private DiameterConnection _rConnection;
	private DiameterConnection _iConnection;
	
	private Map<Integer, Pending> _pendingRequests = new HashMap<Integer, Pending>();
	private Map<Integer, Pending> _waitingRequests = new HashMap<Integer, Pending>();
	
	private AtomicInteger _maxPendings = new AtomicInteger();
	
	private ArrayList<PeerStateListener> _listeners = new ArrayList<PeerStateListener>();
	
	private long _lastAccessed;
	private volatile boolean _pending;
	
	// indicate whether the peer has been explicitly stopped
	private boolean _stopped;
	
	public Peer()
	{
		_state = CLOSED;
		_port = DiameterSocketConnector.DEFAULT_PORT;
		addListener(this);
	}
	
	public Peer(String host)
	{
		this();
		_host = host;
	}
	
	@ManagedAttribute(value="Peer host identity", readonly=true)
	public String getHost()
	{
		return _host;
	}
	
	public void setHost(String host)
	{
		if (_host != null)
			throw new IllegalArgumentException("host already set");
		_host = host;
	}
	
	@ManagedAttribute(value="Peer remote port", readonly=true)
	public int getPort()
	{
		return _port;
	}
	
	public void setPort(int port)
	{
		if (port == -1)
    		port = DiameterSocketConnector.DEFAULT_PORT;
		_port = port;
	}
	
	@ManagedAttribute(value="Peer host address", readonly=true)
	public InetAddress getAddress()
	{
		if (_address == null)
		{
			try 
			{
				_address = InetAddress.getByName(_host);
			} 
			catch (UnknownHostException e) 
			{
				LOG.debug("Host {} is not resolvable", _host);
			}
		}
		return _address;
	}
	
	public void setAddress(InetAddress address)
	{
		_address = address;
	}
	
	public Node getNode()
	{
		return _node;
	}
	
	public void setNode(Node node)
	{
		_node = node;
	}
	
	public State getState()
	{
		return _state;
	}
	
	@ManagedAttribute("Peer state")
	public String getStateAsString()
	{
		return _state.toString();
	}
	
	public boolean isOpen()
	{
		return _state == OPEN;
	}
	
	public boolean isClosed()
	{
		return _state == CLOSED;
	}
	
	public boolean isStopped()
	{
		return _stopped;
	}
		
	public DiameterConnection getConnection()
	{
		return _iConnection != null ? _iConnection : _rConnection;
	}
	
	public void send(DiameterRequest request) throws IOException
	{
		if (!isOpen())
		{
			// FIXME use same timeout ???
			synchronized (_pendingRequests)
			{
				_waitingRequests.put(request.getHopByHopId(), new Pending(request));
			}
			return;
		}
		
		DiameterConnection connection = getConnection();
		if (connection == null || !connection.isOpen())
			throw new IOException("connection not open");
		
		synchronized (_pendingRequests)
		{
			_pendingRequests.put(request.getHopByHopId(), new Pending(request));
			
			if (_node.isStatsOn() && _pendingRequests.size() > _maxPendings.get())
				_maxPendings.set(_pendingRequests.size());
			
		}
		connection.write(request);
	}
	
	public void receive(DiameterMessage message) throws IOException
	{
		synchronized (this)
		{	
			_lastAccessed = System.currentTimeMillis();
			_pending = false; 
		}
		
		if (message.isRequest())
			receiveRequest((DiameterRequest) message);
		else
			receiveAnswer((DiameterAnswer) message);
	}
	
	protected void receiveRequest(DiameterRequest request) throws IOException
	{
		synchronized (this)
		{
			switch (request.getCommand().getCode()) 
			{
			case Common.DWR_ORDINAL:
				receiveDWR(request);
				return;
			case Common.CER_ORDINAL:
				rConnCER(request);
				return;
			case Common.DPR_ORDINAL:
				_state.rcvDPR(request);
				return;
			default:
				break;
			}
		}
		getNode().handle(request);
	}
	
	protected void receiveAnswer(DiameterAnswer answer) throws IOException
	{
		synchronized (this)
		{
			switch (answer.getCommand().getCode()) 
			{
			case Common.CEA_ORDINAL:
				_state.rcvCEA(answer);
				return;
			case Common.DWA_ORDINAL:
				return;
			case Common.DPA_ORDINAL:
				_state.rcvDPA(answer);
				return;
			}
		}
		 
		DiameterRequest request = null;
		synchronized (_pendingRequests)
		{
			
			Pending pending = _pendingRequests.remove(answer.getHopByHopId());
			if (pending != null)
			{
				pending.cancel();
				request = pending.getRequest();
			}
		}
		
		if (request != null)
		{
			answer.setRequest(request);		
			getNode().handle(answer);
		}
		else
			LOG.debug("Ignore answer {} as no corresponding request found", answer);
	}
	
	protected void receiveDWR(DiameterRequest dwr)
	{
		DiameterAnswer dwa = dwr.createAnswer(Common.DIAMETER_SUCCESS);
		try
		{
			dwr.getConnection().write(dwa);
		}
		catch (Exception e)
		{
			LOG.ignore(e);
		}
	}
	
	// --
	
	private void setState(State state)
	{
		LOG.debug(this + " " + _state + " > " + state);
		_state = state;
		
		if (_state == OPEN)
		{
			synchronized (_listeners)
			{
				@SuppressWarnings("unchecked")
				List<PeerStateListener> listeners = (List<PeerStateListener>) _listeners.clone();
				for (PeerStateListener l : listeners)
					l.onPeerOpened(this);
			}
		}
	}
	
	public void onPeerOpened(Peer peer)
	{
		DiameterConnection connection = getConnection();
		if (connection == null || !connection.isOpen())
		{
			LOG.warn("State is open but no open connection");
			return;
		}
			

		synchronized (_pendingRequests)
		{
			try
			{
				Iterator<Pending> it  = _waitingRequests.values().iterator();
				while (it.hasNext())
				{
					Peer.Pending pending = (Peer.Pending) it.next();
					_pendingRequests.put(pending.getRequest().getHopByHopId(), pending);
					it.remove();
					connection.write(pending.getRequest());
				}
			}
			catch (IOException e)
			{
				LOG.debug("Unable to sent waiting requests: {}", e);
			}
		}
	}
	
    public void watchdog()
    {
    	if (isOpen())
    	{
    		if ((System.currentTimeMillis() - _lastAccessed) > 2*_node.getTw())
    		{
    			LOG.warn("closing peer {} since watchdog timer expires", this);
    			close();
    		}
    		else if (((System.currentTimeMillis() - _lastAccessed) > _node.getTw()) && !_pending)
    		{
    			LOG.debug("sending DWR");
    			try
				{
					DiameterRequest request = new DiameterRequest(_node, Common.DWR, 0, null);
			    	DiameterConnection connection = getConnection();
					if (connection == null || !connection.isOpen())
						throw new IOException("connection not open");
					
					_pending = true;
					connection.write(request);
				} 
				catch (IOException e)
				{
					// Ignore exception as 
					LOG.ignore(e);
				}
    		}
    	}
    }
    	
	public String toString()
	{
		return _host + " (" + _state + ")";
	}
	
	// ==================== Events ====================
	
	/**
	 * Starts the peers
	 */
	public synchronized void start()
	{
		if (_host == null)
			throw new IllegalStateException("host not set");
		_state.start();
	}
	
	/**
	 * Handles an incoming connection request (CER)
	 * 
	 * @param cer	the incoming CER
	 */
	public synchronized void rConnCER(DiameterRequest cer)
	{
		_lastAccessed = System.currentTimeMillis();
		_state.rConnCER(cer);
	}
	
	/**
	 * The transport connection has been closed
	 * 
	 * @param connection the closed connection
	 */
	public synchronized void peerDisc(DiameterConnection connection)
	{
		_state.disc(connection);
	}
	
	/**
	 * The Diameter application has signaled that a connection should be 
	 * terminated (e.g., on system shutdown).
	 */
	public synchronized void stop() 
	{
		_stopped = true;
		if (_state == OPEN)
		{
			try 
			{
				setState(CLOSING);
				
				DiameterRequest dpr = new DiameterRequest(_node, Common.DPR, 0, null);
				dpr.add(Common.DISCONNECT_CAUSE, DisconnectCause.REBOOTING);
				getConnection().write(dpr);	
			} 
			catch (IOException e) 
			{
				LOG.warn("Unable to send DPR on shutdown", e);
			}
		} 
		else if (_state != CLOSING)
			setState(CLOSED);
	}
	
	protected void close() 
	{
		if (_rConnection != null)
			_rConnection.stop();
		else if (_iConnection != null)
			_iConnection.stop();
		
		_rConnection = _iConnection = null;
		setState(CLOSED);
		if (!_stopped)
			_node.scheduleReconnect(this);
	}
	
	/**
	 * An application-defined timer has expired while waiting for some event. 
	 */
	protected void timeout()
	{
		close();
	}
	
	// ==================== Actions ====================

	protected synchronized void iSndConnReq()
	{
		setState(WAIT_CONN_ACK);
		// cnx request is blocking so start in a new thread
		new Thread(new Runnable() 
		{
			public void run() 
			{
				try
				{
					_iConnection = _node.getConnection(Peer.this);
				}
				catch (IOException e)
				{
					LOG.ignore(e);
					LOG.debug("Failed to connect to peer {} due to {}", Peer.this, e);
				}
				synchronized (Peer.this)
				{
					if (_iConnection != null)
						_state.rcvConnAck();
					else
						_state.rcvConnNack();
				}
			}
		}).start();
	}
	
	protected boolean elect()
    {
        String other = getHost();
        String local = _node.getIdentity();
        
        boolean won = (local.compareTo(other) > 0);
        if (won)
            LOG.debug("Won election (" + local + ">" + other + ")");
        return won;      
    }
    
    protected void sendCER()
    {
    	DiameterRequest cer = new DiameterRequest(getNode(), Common.CER, 0, null);
		getNode().addCapabilities(cer);
		
		try
		{
			getConnection().write(cer);
		}
		catch (IOException e)
		{
			LOG.debug(e);
		}
    }
    
    protected void iDisc()
    {
        if (_iConnection != null) 
        {
            try { _iConnection.close(); } 
            catch (Exception e) { LOG.debug("Failed to disconnect " + _iConnection + " on peer " + this + ": " + e); }
            _iConnection = null;
        }
    }
    
    protected void rDisc()
    {
        if (_rConnection != null) 
        {
        	try { _rConnection.close(); } 
        	catch (Exception e) { LOG.debug("Failed to disconnect " + _rConnection + " on peer " + this + ": " + e); }
        	_rConnection = null;
        }
    }
    
    protected void sendCEA(DiameterRequest cer)
    {
    	DiameterAnswer cea = cer.createAnswer(Common.DIAMETER_SUCCESS);
		try
		{
			cea.send();
		}
		catch (IOException e)
		{
			LOG.debug(e);
		}
    	
    }
    
    @ManagedAttribute("Pendings requests")
    public int getPendings()
    {
    	synchronized (_pendingRequests)
		{
        	return _pendingRequests.size();
		}
    }
    
    @ManagedAttribute("Maximum pending requests since last reset")
    public int getMaxPendings()
    {
    	return _maxPendings.get();
    }
    
    @ManagedAttribute("Waiting requests")
    public int getWaitings()
    {
    	synchronized (_pendingRequests)
		{
        	return _waitingRequests.size();
		}
    }
    
    public void statsReset() 
    {
    	_maxPendings.set(0);
    }
    
    public void addListener(PeerStateListener l)
    {
    	synchronized (_listeners)
		{
    		if (!_listeners.contains(l))
    			_listeners.add(l);
		}
    }
    
    public void removeListener(PeerStateListener l)
    {
    	synchronized (_listeners)
		{
    		_listeners.remove(l);
		}
    }
    
	public String dump()
	{
		return ContainerLifeCycle.dump(this);
	}

	public void dump(Appendable out, String indent) throws IOException
	{
		 out.append(String.valueOf(this)).append("\n");
		 ContainerLifeCycle.dump(out,indent,Arrays.asList("port=" + _port, "address=" + _address));
	}
    
    // peer states 
    
	private abstract class State
	{
		private String _name;
		
		public State(String name)
		{
			_name = name;
		}
		
		public void start() { throw new IllegalStateException("start() in state " + _name); }
		public void rConnCER(DiameterRequest cer) { throw new IllegalStateException("rConnCER() in state " + _name); }
		public void rcvConnAck() { throw new IllegalStateException("rcvConnAck() in state " + _name); }
		public void rcvConnNack() { throw new IllegalStateException("rcvConnNack() in state " + _name); }
		public void rcvCEA(DiameterAnswer cea) { throw new IllegalStateException("rcvCEA() in state " + _name); }
		public void rcvDPR(DiameterRequest dpr) { throw new IllegalStateException("rcvDPR() in state " + _name); }
		public void rcvDPA(DiameterAnswer dpa) { throw new IllegalStateException("rcvDPA() in state " + _name); }
		public void disc(DiameterConnection connection) { throw new IllegalStateException("disc() in state " + _name); } 
		public String toString() { return _name; }
	}
	
	/**
	 * <pre>
	 *    state            event              action         next state
	 *    -----------------------------------------------------------------
	 *    Closed           Start            I-Snd-Conn-Req   Wait-Conn-Ack
	 *                     R-Conn-CER       R-Accept,        R-Open
	 *                                      Process-CER,
	 *                                      R-Snd-CEA
	 * </pre>
	 */    
	State CLOSED = new State("Closed")
	{
		public synchronized void start()
		{
			_rConnection = _iConnection = null;
			iSndConnReq();
		}
		
		public synchronized void rConnCER(DiameterRequest cer)
		{
			_rConnection = cer.getConnection();
			
			sendCEA(cer);
			setState(OPEN);
		}
		
		public synchronized void disc(DiameterConnection connection)
		{
		}
	};
	
	/**
	 * <pre>
	 * state            event              action         next state
	 * -----------------------------------------------------------------
	 * Wait-Conn-Ack    I-Rcv-Conn-Ack   I-Snd-CER        Wait-I-CEA
	 *                  I-Rcv-Conn-Nack  Cleanup          Closed
	 *                  R-Conn-CER       R-Accept,        Wait-Conn-Ack/
	 *                                   Process-CER      Elect
	 *                  Timeout          Error            Closed
	 * </pre>
	 */
	State WAIT_CONN_ACK = new State("Wait-Conn-Ack")
	{
		public synchronized void rcvConnAck()
		{
			sendCER();
			setState(WAIT_CEA);
		}

		@Override
		public synchronized void rcvConnNack()
		{
			_iConnection = null;
			close();
		}

		@Override
		public synchronized void rConnCER(DiameterRequest cer)
		{
			_rConnection = cer.getConnection();
			if (elect())
            {
                iDisc();
                sendCEA(cer);
                setState(OPEN);
            }
            else 
                setState(WAIT_RETURNS);
		}
		
	};
	
	/**
	 * <pre>
	 *    Wait-I-CEA       I-Rcv-CEA        Process-CEA      I-Open
	 *                     R-Conn-CER       R-Accept,        Wait-Returns
	 *                                      Process-CER,
	 *                                      Elect
	 *                     I-Peer-Disc      I-Disc           Closed
	 *                     I-Rcv-Non-CEA    Error            Closed
	 *                     Timeout          Error            Closed
	 * </pre>
	 */
	State WAIT_CEA = new State("Wait-CEA")
	{
		public synchronized void rcvCEA(DiameterAnswer cea)
		{
			setState(OPEN);
		}

		public synchronized void disc(DiameterConnection connection)
		{
			iDisc();
			close();
		}

		public synchronized void rConnCER(DiameterRequest cer)
		{
			_rConnection = cer.getConnection();
			if (elect())
            {
                iDisc();
                sendCEA(cer);
                setState(OPEN);
            }
            else 
                setState(WAIT_RETURNS);
		}
		
	};
	
	/**
	 * <pre>
	 * Wait-Conn-Ack/   I-Rcv-Conn-Ack   I-Snd-CER,Elect  Wait-Returns
     * Elect            I-Rcv-Conn-Nack  R-Snd-CEA        R-Open
     *                  R-Peer-Disc      R-Disc           Wait-Conn-Ack
     *                  R-Conn-CER       R-Reject         Wait-Conn-Ack/
     *                                                    Elect
     *                  Timeout          Error            Closed
     *</pre>
	 */
	State WAIT_CONN_ACK_ELECT = new State("Wait-Conn-Ack-Elect")
	{

		@Override
		public synchronized void disc(DiameterConnection connection)
		{
			iDisc();
			close();
		}

		@Override
		public synchronized void rConnCER(DiameterRequest cer)
		{
			_rConnection = null;
		}

		@Override
		public synchronized void rcvConnAck()
		{
			sendCER();
			if (elect())
			{
				iDisc();
				// TODO send CEA
				setState(OPEN);
			}
			else
				setState(WAIT_RETURNS);
		}

		@Override
		public synchronized void rcvConnNack()
		{
			// TODO send CEA
			setState(OPEN);
		}
		
	};
	
	/**
	 * <pre>
	 *  Wait-Returns     Win-Election     I-Disc,R-Snd-CEA R-Open
	 *                   I-Peer-Disc      I-Disc,          R-Open
	 *                                    R-Snd-CEA
	 *                   I-Rcv-CEA        R-Disc           I-Open
	 *                   R-Peer-Disc      R-Disc           Wait-I-CEA
	 *                   R-Conn-CER       R-Reject         Wait-Returns
	 *                   Timeout          Error            Closed
	 *</pre>
	 */
	State WAIT_RETURNS = new State("Wait-Returns")
	{

		@Override
		public synchronized void disc(DiameterConnection connection)
		{
			if (connection == _iConnection)
			{
				iDisc();
				// TODO sendCea
				setState(OPEN);
			}
			else if (connection == _rConnection)
			{
				rDisc();
				setState(WAIT_CEA);
			}
		}

		@Override
		public synchronized void rConnCER(DiameterRequest cer)
		{
			_rConnection = null;
		}

		@Override
		public synchronized void rcvCEA(DiameterAnswer cea)
		{
			rDisc();
			setState(OPEN);
		}
		
	};
	
	/**
	 * <pre>
	 *  I-Open      Send-Message     I-Snd-Message    I-Open
	 *              I-Rcv-Message    Process          I-Open
	 *              I-Rcv-DWR        Process-DWR,     I-Open
	 *                               I-Snd-DWA
	 *              I-Rcv-DWA        Process-DWA      I-Open
	 *              R-Conn-CER       R-Reject         I-Open
	 *              Stop             I-Snd-DPR        Closing
	 *              I-Rcv-DPR        I-Snd-DPA,       Closed
	 *                                 I-Disc
	 *              I-Peer-Disc      I-Disc           Closed
	 *              I-Rcv-CER        I-Snd-CEA        I-Open
	 *              I-Rcv-CEA        Process-CEA      I-Open
	 *</pre>
	 */
	State OPEN = new State("Open")
	{
		public synchronized void disc(DiameterConnection connection)
		{
			if (connection == getConnection())
			{
				close();
			}
		}

		@Override
		public synchronized void rcvDPR(DiameterRequest dpr)
		{
			try 
			{
				DiameterAnswer dpa = dpr.createAnswer(Common.DIAMETER_SUCCESS);
				dpr.getConnection().write(dpa);
			} catch (IOException e)
			{
				LOG.warn("Unable to send DPA");
			}
			
			setState(CLOSED);
			_rConnection = _iConnection = null;
			
			// Start reconnect task if Disconnect cause is rebooting
			if (dpr.get(Common.DISCONNECT_CAUSE) == DisconnectCause.REBOOTING)
			{
				_node.scheduleReconnect(Peer.this);
			}
		}	
	};
	
	/**
	 * <pre>
	 *  Closing     I-Rcv-DPA        I-Disc           Closed
	 *              R-Rcv-DPA        R-Disc           Closed
	 *              Timeout          Error            Closed
	 *              I-Peer-Disc      I-Disc           Closed
	 *              R-Peer-Disc      R-Disc           Closed
	 * </pre>
	 */
	State CLOSING = new State("Closing")
	{
		@Override
		public void disc(DiameterConnection connection)
		{
			setState(CLOSED);
		}

		@Override
		public void rcvDPA(DiameterAnswer dpa)
		{
			setState(CLOSED);
		}
		
		@Override
		public synchronized void rcvDPR(DiameterRequest dpr)
		{
			try 
			{
				DiameterAnswer dpa = dpr.createAnswer(Common.DIAMETER_SUCCESS);
				dpr.getConnection().write(dpa);
			} catch (IOException e) 
			{
				LOG.warn("Unable to send DPA");
			}
			
			setState(CLOSED);
			_rConnection = _iConnection = null;
		}	
	};
	
	class Pending implements Runnable
	{
		private DiameterRequest _request;
		private ScheduledFuture<?> _timeout;
		
		public Pending(DiameterRequest request)
		{
			_request = request;
			_timeout = _node.schedule(this, _node.getRequestTimeout());
		}
		
		public void cancel()
		{
			_timeout.cancel(false);
		}

		public DiameterRequest getRequest()
		{
			return _request;
		}
		
		public void run()
		{
			LOG.debug("Diameter request timeout for {}", _request);
			if (_node.getHandler() instanceof TimeoutHandler)
				((TimeoutHandler) _node.getHandler()).fireNoAnswerReceived(_request, _node.getRequestTimeout());
			
			synchronized (_pendingRequests)
			{
				Pending p = _pendingRequests.remove(_request.getHopByHopId());
				if (p == null)
					_waitingRequests.remove(_request.getHopByHopId());
			}
			
		}
	}
}
