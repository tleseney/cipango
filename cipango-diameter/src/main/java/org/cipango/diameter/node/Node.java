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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.cipango.diameter.AVP;
import org.cipango.diameter.ApplicationId;
import org.cipango.diameter.Dictionary;
import org.cipango.diameter.app.DiameterContext;
import org.cipango.diameter.base.Common;
import org.cipango.diameter.bio.DiameterSocketConnector;
import org.cipango.diameter.log.BasicMessageLog;
import org.cipango.diameter.router.DefaultRouter;
import org.cipango.diameter.router.DiameterRouter;
import org.cipango.diameter.util.AAAUri;
import org.cipango.server.session.SessionManager.ApplicationSessionScope;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ArrayUtil;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.Dumpable;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;


/**
 * A Diameter node is a host process that implements the Diameter protocol, 
 * and acts either as a Client, Agent or Server.
 * Can be used standalone or linked to a {@link Server}.
 */
@ManagedObject("Diameter node")
public class Node extends ContainerLifeCycle implements DiameterHandler, Dumpable
{
	private static final Logger LOG = Log.getLogger(Node.class);
	
	public static String[] __dictionaryClasses = 
	{
		"org.cipango.diameter.base.Common", 
		"org.cipango.diameter.base.Accounting",
		"org.cipango.diameter.ims.IMS", 
		"org.cipango.diameter.ims.Cx", 
		"org.cipango.diameter.ims.Sh",
		"org.cipango.diameter.ims.Zh"
	};
	
	public static final String DEFAULT_REALM = "cipango.org";
	public static final String DEFAULT_PRODUCT_NAME = "cipango";
	public static final int NEXCOM_OID = 26588;
	
	public static final long DEFAULT_TW = 30000;
	public static final long DEFAULT_TC = 30000;
	public static final long DEFAULT_REQUEST_TIMEOUT = 10000;
	public static final long DEFAULT_REDIRECT_TIMEOUT = 1000;
		
	private String _realm = DEFAULT_REALM;
	private String _identity;
	private int _vendorId = NEXCOM_OID;
	private String _productName = DEFAULT_PRODUCT_NAME;
	
	private long _tw = DEFAULT_TW;
	private long _tc = DEFAULT_TC;
	private long _requestTimeout = DEFAULT_REQUEST_TIMEOUT;
	
	private DiameterConnector[] _connectors;
	
	private Peer[] _peers;
		
	private DiameterHandler _handler;
	private SessionManager _sessionManager;
	
	private ScheduledExecutorService _scheduler;

	private Set<ApplicationId> _supportedApplications = new HashSet<ApplicationId>();
	
	private DiameterRouter _router;
	
	protected final AtomicLong _statsStartedAt = new AtomicLong(-1L);
		
	public Node()
	{
		setHandler(new DiameterContext());
	}
	
	public Node(int port) throws IOException
	{
		setHandler(new DiameterContext());
		DiameterSocketConnector connector = new DiameterSocketConnector();
		connector.setHost(InetAddress.getLocalHost().getHostAddress());
		connector.setMessageListener(new BasicMessageLog());
		connector.setPort(port);
		setConnectors(new DiameterConnector[] {connector});
	}
		
	public void setConnectors(DiameterConnector[] connectors)
	{
		if (connectors != null)
		{
			for (int i = 0; i < connectors.length; i++)
				connectors[i].setNode(this);
		}
		updateBeans(_connectors, connectors);
		_connectors = connectors;
	}
	
	@ManagedAttribute(value="Connectors", readonly=true)
	public DiameterConnector[] getConnectors()
    {
        return _connectors;
    }
	
	public void addConnector(DiameterConnector connector)
    {
        setConnectors((DiameterConnector[])ArrayUtil.addToArray(getConnectors(), connector, DiameterConnector.class));
    }
	
	public synchronized void addPeer(Peer peer)
	{
		peer.setNode(this);
		
		addBean(peer);
		
		Peer[] peers = (Peer[]) ArrayUtil.addToArray(_peers, peer, Peer.class);
				
		_peers = peers;
		
		if( isStarted())
			_router.peerAdded(peer);
		
	}
	
	public synchronized void removePeer(Peer peer)
	{		
		Peer[] peers = (Peer[]) ArrayUtil.removeFromArray(_peers, peer);
		peers = peers.clone();
		
		_peers = peers;
		
		peer.stop();
		
		if( isStarted())
			_router.peerRemoved(peer);
		
		removeBean(peer);
	}
	
	@ManagedAttribute(value="Peers", readonly=true)
	public Peer[] getPeers()
	{
		return _peers;
	}
	
	@Override
	protected void doStart() throws Exception 
	{
		for (int i = 0; i < __dictionaryClasses.length; i++)
		{
			Dictionary.getInstance().load(Loader.loadClass(getClass(), __dictionaryClasses[i]));
		}
			
		if (_identity == null) 
			_identity = InetAddress.getLocalHost().getHostName();
		
		_sessionManager = new SessionManager();
		
		_sessionManager.setNode(this);
		addBean(_sessionManager);
		
		if (_connectors != null)
		{
			for (int i = 0; i < _connectors.length; i++)
			{
				_connectors[i].start();
			}
		}
		
		_scheduler = new ScheduledThreadPoolExecutor(1);
		
		if (_router == null)
			_router = new DefaultRouter();
		
		if (_router instanceof LifeCycle)
			((LifeCycle) _router).start();
		
		synchronized (this)
		{
			if (_peers != null)
			{
				for (Peer peer : _peers)
				{
					try 
					{
						peer.start(); 
						_router.peerAdded(peer);
					}
					catch (Exception e) 
					{ 
						LOG.warn("failed to start peer: " + peer, e);
					}
				}
			}
		}	
		
		super.doStart();
		
		_scheduler.scheduleAtFixedRate(new WatchdogTimeout(), 5000, 5000, TimeUnit.MILLISECONDS);
		LOG.info("Started {}", this);
	}
	
	@Override
	protected void doStop() throws Exception 
	{	
		MultiException mex = new MultiException();
		
		synchronized (this)
		{
			if (_peers != null)
			{
				for (Peer peer : _peers)
				{
					try 
					{
						peer.stop(); 
					}
					catch (Exception e) 
					{ 
						LOG.warn("failed to stop peer: " + peer, e);
					}
				}
				
				// Wait at most 1 seconds for DPA reception
				try 
				{
					boolean allClosed = false;
					int iter = 20;
					while (iter-- > 0 && allClosed)
					{
						allClosed = true;
						for (Peer peer : _peers)
						{
							if (!peer.isClosed())
							{
								allClosed = false;
								LOG.info("Wait 50ms for " + peer + " closing");
								Thread.sleep(50);
								break;
							}
						} 
					}
				} 
				catch (Exception e) 
				{
					LOG.ignore(e);
				}
			}
		}	
		
		if (_scheduler != null)
			_scheduler.shutdown();
		
		for (int i = 0; _connectors != null && i < _connectors.length; i++)
		{
			if (_connectors[i] instanceof LifeCycle) 	
			{
				try
				{
					((LifeCycle) _connectors[i]).stop();
				}
				catch (Exception e)
				{
					mex.add(e);
				}
			}
		}
				
		if (_router != null && _router instanceof LifeCycle)
			((LifeCycle) _router).stop();
		
		super.doStop();
		mex.ifExceptionThrow();
	}
	
	public void setIdentity(String identity)
	{
		_identity = identity;
	}
	
	@ManagedAttribute("Identity")
	public String getIdentity()
	{
		return _identity;
	}
	
	public void setRealm(String realm)
	{
		_realm = realm;
	}
	
	@ManagedAttribute("Realm")
	public String getRealm()
	{
		return _realm;
	}
	
	public int getVendorId()
	{
		return _vendorId;
	}
	
	@ManagedAttribute("Product name")
	public String getProductName()
	{
		return _productName;
	}

	public void setProductName(String productName)
	{
		_productName = productName;
	}
	
	@ManagedAttribute("Session manager")
	public SessionManager getSessionManager()
	{
		return _sessionManager;
	}
	
	public DiameterConnection getConnection(Peer peer) throws IOException
	{
		return _connectors[0].getConnection(peer);
	}
	
	public Peer getPeer(String host)
	{
		synchronized (this)
		{
			if (_peers != null)
			{
				for (Peer peer : _peers)
				{
					if (peer.getHost().equals(host))
						return peer;
				}
			}
		}
		return null;
	}
	
	@ManagedAttribute(value="Diameter router", readonly=true)
	public DiameterRouter getDiameterRouter()
	{
		return _router;
	}

	public void setDiameterRouter(DiameterRouter router)
	{
		updateBean(_router, router);
		_router = router;
	}
	
	public void send(DiameterRequest request) throws IOException
	{
		Peer peer = _router.getRoute(request);
		
		if (peer == null && request.getDestinationHost() != null)
		{
			peer = new Peer(request.getDestinationHost());
			peer.start();
			addPeer(peer);
		}
			
		if (peer != null)
			peer.send(request);
		else
			throw new IOException("Router found no peer and no destination host set");
	}
	
	public void receive(DiameterMessage message) throws IOException
	{
		Peer peer = message.getConnection().getPeer();
		
		if (peer == null)
		{
			if (message.getCommand() != Common.CER)
			{
				LOG.debug("non CER as first message: " + message.getCommand());
				message.getConnection().stop();
				return;
			}
			if (message.getCommand() == Common.CER)
			{
				String originHost = message.getOriginHost();
				
				if (originHost == null)
				{
					LOG.debug("No Origin-Host in CER");
					message.getConnection().stop();
					return;
				}
				String realm = message.getOriginRealm();
				if (realm == null)
				{
					LOG.debug("No Origin-Realm in CER");
					message.getConnection().stop();
					return;
				}
				
				peer = getPeer(originHost);
				
				if (peer == null)
				{
					LOG.warn("Unknown peer " + originHost);
					peer = new Peer(originHost);
					peer.setNode(this);
					addPeer(peer);
				}
				message.getConnection().setPeer(peer);
				peer.rConnCER((DiameterRequest) message);
			}
		}
		else 
		{
			peer.receive(message);
		}
	}
	
	public DiameterHandler getHandler()
	{
		return _handler;
	}
	
	public void setHandler(DiameterHandler handler)
	{
		_handler = handler;
	}
	
	public void handle(DiameterMessage message) throws IOException
	{
		
		// System.out.println("Node.handle(): Got message: " + message);
		String sessionId = message.getSessionId();		
		if (sessionId != null)			
			message.setSession(_sessionManager.get(sessionId));
				
		if (message instanceof DiameterAnswer)
		{
			DiameterAnswer answer = (DiameterAnswer) message;
			if (Common.DIAMETER_REDIRECT_INDICATION.equals(answer.getResultCode()))
			{
				new RedirectHandler(answer).run();
				return;
			}
		}
		
		ApplicationSessionScope scope = null;
		try
		{
			scope = _sessionManager.openScope(message.getApplicationSession());
			if (_handler != null)
				_handler.handle(message);
		}
		finally
		{
			if (scope != null)
				scope.close();
		}
	}
	
	public void addSupportedApplication(ApplicationId id)
	{
		_supportedApplications.add(id);
	}
	
	public void addCapabilities(DiameterMessage message)
	{	
		for (DiameterConnector connector : _connectors)
		{
			message.add(Common.HOST_IP_ADDRESS, connector.getLocalAddress());
		}
		
		message.add(Common.VENDOR_ID, getVendorId());
		message.add(Common.PRODUCT_NAME, getProductName());
				
		for (ApplicationId id : _supportedApplications)
		{
			if (id.isVendorSpecific())
			{
				for (Integer i : id.getVendors())
				{
					message.add(Common.SUPPORTED_VENDOR_ID, i);
				}
			}
		}
		
		for (ApplicationId id : _supportedApplications)
		{
			message.getAVPs().add(id.getAVP());
		}
		
		message.add(Common.FIRMWARE_REVISION, 1);
	}
	
	public String toString()
	{
		return _identity + "(" + _supportedApplications + ")";
	}
	
	@ManagedAttribute("Tw timer in milliseconds")
	public long getTw()
	{
		return _tw;
	}

	public void setTw(long tw)
	{
		if (tw < 6000)
			throw new IllegalArgumentException("Tw MUST NOT be set lower than 6 seconds");
		_tw = tw;
	}

	@ManagedAttribute("Tc timer in milliseconds")
	public long getTc()
	{
		return _tc;
	}

	public void setTc(long tc)
	{
		_tc = tc;
	}
	
	public ScheduledFuture<?> schedule(Runnable runnable, long ms)
	{
		if (isRunning())
			return _scheduler.schedule(runnable, ms, TimeUnit.MILLISECONDS);
		return null;
	}
	
	public void scheduleReconnect(Peer peer)
	{
		schedule(new ConnectPeerTimeout(peer), _tc);
	}
	
	@ManagedOperation("Reset all statistics")
	public void allStatsReset()
	{
		getSessionManager().statsReset();
		for (int i = 0; _connectors != null && i < _connectors.length; i++)
			if (_connectors[i] instanceof AbstractDiameterConnector)
				((AbstractDiameterConnector) _connectors[i]).statsReset();
		
		synchronized (this)
		{
			for (int i = 0; _peers != null && i < _peers.length; i++)
				_peers[i].statsReset();
		}
	}
		
	@ManagedOperation("Reset statistics")
	public void statsReset()
    {
        updateNotEqual(_statsStartedAt,-1,System.currentTimeMillis());

        if (getSessionManager() != null)
        	getSessionManager().statsReset();
        
		for (int i = 0; _connectors != null && i < _connectors.length; i++)
			if (_connectors[i] instanceof AbstractDiameterConnector)
				((AbstractDiameterConnector) _connectors[i]).statsReset();
		
		synchronized (this)
		{
			for (int i = 0; _peers != null && i < _peers.length; i++)
				_peers[i].statsReset();
		}
    }
	
	private void updateNotEqual(AtomicLong valueHolder, long compare, long value)
    {
        long oldValue = valueHolder.get();
        while (compare != oldValue)
        {
            if (valueHolder.compareAndSet(oldValue,value))
                break;
            oldValue = valueHolder.get();
        }
    }
	
	public void setStatsOn(boolean on)
    {
        if (on && _statsStartedAt.get() != -1)
            return;

        LOG.debug("Statistics on = " + on + " for " + this);

        statsReset();
        _statsStartedAt.set(on?System.currentTimeMillis():-1);
    }
	
	@ManagedAttribute("Stats on")
	public boolean isStatsOn()
    {
        return _statsStartedAt.get() != -1;
    }
	
	@ManagedAttribute("Statistics start time")
	public long getStatsStartedAt()
	{
		return _statsStartedAt.get();
	}
	
	public long getRequestTimeout()
	{
		return _requestTimeout;
	}

	public void setRequestTimeout(long requestTimeout)
	{
		_requestTimeout = requestTimeout;
	}
	
	public void addDictionary(String classname) throws ClassNotFoundException
	{
		Dictionary.getInstance().load(Loader.loadClass(getClass(), classname));
	}
	
	public String dump()
	{
		return ContainerLifeCycle.dump(this);
	}

	public void dump(Appendable out, String indent) throws IOException
	{
		out.append("Node ").append(_identity).append(' ').append(getState()).append('\n');
		List<Object> l = new ArrayList<Object>();
		l.add("Realm=" + _realm);
		l.add("ProductName=" + _productName);
		l.add(_router);
		l.add(_sessionManager);
		
		ContainerLifeCycle.dump(out,indent,l, Arrays.asList(_peers), Arrays.asList(_connectors), _supportedApplications);
	}
	
	class ConnectPeerTimeout implements Runnable
	{
		private Peer _peer;
		
		public ConnectPeerTimeout(Peer peer)
		{
			_peer = peer;
		}
		
		public void run()
		{
			try
			{
				if (isStarted())
				{
					if (!_peer.isStopped() && !_peer.isOpen())
					{
						LOG.debug("restarting peer: " + _peer);
						_peer.start();
					}
				}
			}
			catch (Exception e)
			{
				LOG.warn("failed to reconnect to peer {} : {}", _peer, e);
			}
		}
	}
	
	class WatchdogTimeout implements Runnable
	{
		public void run()
		{
			if (_peers != null)
			{
				for (Peer peer: _peers)
					peer.watchdog();
			}
		}
	}
	
	class RedirectHandler implements Runnable, PeerStateListener
	{
		private DiameterAnswer _answer;
		private Peer _peer;
		private Iterator<AVP<String>> _it;
		private ScheduledFuture<?> _timeout;
		
		public RedirectHandler(DiameterAnswer answer)
		{
			_answer = answer;
			_it = _answer.getAVPs().getAVPs(Common.REDIRECT_HOST);
			 if (!_it.hasNext())
             	LOG.warn("Missing required REDIRECT_HOST AVP in redirect response: {}", _answer);
		}
				
		public void run()
		{
			try 
            {   
				if (_peer != null)
				{
					if (_peer.isOpen())
					{
						_peer.send(_answer.getRequest());
						return;
					}
					_peer.removeListener(this);
					_peer = null;
				}
				while (_it.hasNext())
				{
                	AAAUri uri = new AAAUri(_it.next().getValue());
                    Peer peer = getPeer(uri.getFQDN());
                    if (peer != null) 
                    {
                        if (peer.isOpen())
                        {
                        	LOG.debug("Redirecting request to: " + peer);
                			peer.send(_answer.getRequest());
                			return;
                        }
                    }
                    else
                    {
                    	peer = new Peer(uri.getFQDN());
                    	peer.setPort(uri.getPort());
                    	peer.addListener(this);
            			peer.start();
            			addPeer(peer);
            			_peer = peer;
            			_timeout = schedule(this, DEFAULT_REDIRECT_TIMEOUT);
                    	LOG.debug("Redirecting request to new peer: " + peer);
                        return;
                    }
				}
            } 
            catch (Exception e)
            {
                LOG.warn("Failed to redirect request", e);
            }	
			if (getHandler() instanceof DiameterContext)
				((DiameterContext) getHandler()).fireNoAnswerReceived(_answer.getRequest(), getRequestTimeout());

		}

		public void onPeerOpened(Peer peer)
		{
			_timeout.cancel(false);
			peer.removeListener(this);
			try
			{
				peer.send(_answer.getRequest());
			}
			catch (Exception e)
			{
				LOG.warn("Failed to redirect request", e);
			}
		}
	}
}
