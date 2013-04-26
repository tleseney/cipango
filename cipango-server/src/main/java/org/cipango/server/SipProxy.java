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
package org.cipango.server;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.servlet.sip.Address;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.ProxyBranch;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TooManyHopsException;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;

import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.Session;
import org.cipango.server.session.SessionHandler;
import org.cipango.server.session.SessionManager.ApplicationSessionScope;
import org.cipango.server.session.scoped.ScopedRunable;
import org.cipango.server.session.scoped.ScopedServerTransactionListener;
import org.cipango.server.transaction.ClientTransaction;
import org.cipango.server.transaction.ClientTransactionListener;
import org.cipango.server.transaction.ServerTransaction;
import org.cipango.server.transaction.ServerTransactionListener;
import org.cipango.server.transaction.Transaction;
import org.cipango.sip.AddressImpl;
import org.cipango.sip.ParameterableImpl;
import org.cipango.sip.SipGrammar;
import org.cipango.sip.SipHeader;
import org.cipango.util.TimerTask;
import org.cipango.util.TimerTask.Cancelable;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class SipProxy implements Proxy, ServerTransactionListener, Serializable
{    
	private static final Logger LOG = Log.getLogger(SipProxy.class);
	
	private static final long serialVersionUID = 1L;

	public static final int DEFAULT_TIMER_C = 180;
  
    public static int __maxForwards = 70;
    public static int __timerC = DEFAULT_TIMER_C;
    
    private boolean _started;
    
    private boolean _parallel = true;
    private boolean _recurse = true;
    private boolean _supervised = true;
    private boolean _noCancel = false;
        
    private int _proxyTimeout = __timerC;
    
    private SipURI _rrUri;
    private SipURI _pathUri;
    
    private ServerTransaction _tx;
    private SipResponse _best;
    
    private int _actives;
    
    private List<Branch> _branches = new ArrayList<SipProxy.Branch>(1);
    private List<Branch> _targets = new ArrayList<Branch>(1);

	public SipProxy(SipRequest request) throws TooManyHopsException
    {
		_tx = (ServerTransaction) request.getTransaction();
        _tx.setListener(new ScopedServerTransactionListener(request.session(), this));
                        
        int maxForwards = request.getMaxForwards();
        if (maxForwards == 0) 
            throw new TooManyHopsException();
        else if (maxForwards == -1) 
            request.setMaxForwards(70);
        
        if (LOG.isDebugEnabled()) 
        	LOG.debug("Created proxy for tx {}", _tx, null);
    }
	
	/**
	 * @see Proxy#cancel()
	 */
	public void cancel() 
	{
		cancel(null, null, null);
    }
	
	/**
	 * @see Proxy#cancel(String[], int[], String[])
	 */
	public void cancel(String[] protocol, int[] reasonCode, String[] reasonText)
	{                
		if (_tx.isCompleted())
			throw new IllegalStateException("Transaction has completed");
		
		doCancel(protocol, reasonCode, reasonText);
	}
	
	protected void doCancel(String[] protocol, int[] reasonCode, String[] reasonText)
	{
        for (Branch branch : _branches)
        {
        	branch.cancel(protocol, reasonCode, reasonText);
        }
	}
	
	/**
	 * @see Proxy#createProxyBranches(List)
	 */
	public List<ProxyBranch> createProxyBranches(List<? extends URI> list)
	{
		List<ProxyBranch> branches = new ArrayList<ProxyBranch>(list.size());
		
		for (URI uri : list)
		{
			Branch branch = addBranch(uri);
			if (branch != null)
				branches.add(branch);
		}
		return branches;
	}
	
	/**
	 * @see Proxy#getAddToPath()
	 */
	public boolean getAddToPath()
	{
		return _pathUri != null;
	}

	/**
	 * @see Proxy#getNoCancel
	 */
	public boolean getNoCancel()
	{
		return _noCancel;
	}
	
	/**
	 * @see Proxy#getOriginalRequest()
	 */
	public SipServletRequest getOriginalRequest()
    {
		return _tx.getRequest();
	}
	
	/**
	 * @see Proxy#getParallel()
	 */
	public boolean getParallel()
    {
		return _parallel;
	}
	
	/**
	 * @see Proxy#getPathURI()
	 */
	public SipURI getPathURI()
	{
		if (_pathUri == null)
			throw new IllegalStateException("addToPath is not enabled");
		return _pathUri;
	}

	/**
	 * @see Proxy#getProxyBranch(URI)
	 */
	public ProxyBranch getProxyBranch(URI uri)
	{
		Iterator<Branch> it = new BranchIterator(_branches);
		while (it.hasNext())
		{
			Branch branch = it.next();
			if (branch.getUri().equals(uri))
				return branch;
		}
		return null;
	}
	
	/**
	 * @see Proxy#getProxyBranches()
	 */
	public List<ProxyBranch> getProxyBranches()
	{
		return new ArrayList<ProxyBranch>(_branches);
	}
	
	/**
	 * @see Proxy#getProxyTimeout()
	 */
	public int getProxyTimeout()
	{
		return _proxyTimeout;
	}
	
	/**
	 * @see Proxy#getRecordRoute()
	 */
	public boolean getRecordRoute()
    {
		return _rrUri != null;
	}
	
	/**
	 * @see Proxy#getRecordRouteURI()
	 */
	public SipURI getRecordRouteURI()
    {
		if (_rrUri == null) 
			throw new IllegalStateException("Record-Routing is not enabled");
        
		return _rrUri;
	}
	
	/**
	 * @see Proxy#getRecurse()
	 */
	public boolean getRecurse()
    {
		return _recurse;
	}
	
	/**
	 * @see Proxy#getSequentialSearchTimeout()
	 * @deprecated
	 */
	public int getSequentialSearchTimeout() 
    {
		return getProxyTimeout();
	}
	
	/**
	 * @see Proxy#getStateful()
	 * @deprecated
	 */
	public boolean getStateful() 
    {
		return true;
	}
	
	/**
	 * @see Proxy#getSupervised()
	 */
	public boolean getSupervised() 
    {
		return _supervised;
	}
	
	/**
	 * @see Proxy#proxyTo(List)
	 */
	public void proxyTo(List<? extends URI> targets) 
    {
		
		for (URI uri : targets) 
		{
			if (!uri.isSipURI() && getOriginalRequest().getHeader(SipHeader.ROUTE.asString()) == null)
	 	 	 	throw new IllegalArgumentException("Cannot route " + uri);
			addBranch(uri);
		}
		startProxy();
	}
	
	/**
	 * @see Proxy#proxyTo(javax.servlet.sip.URI)
	 */
	public void proxyTo(URI uri) 
    {
		if (!uri.isSipURI() && getOriginalRequest().getHeader(SipHeader.ROUTE.asString()) == null)
 	 	 	throw new IllegalArgumentException("Cannot route " + uri);
		addBranch(uri);
		startProxy();
	}
	
	/**
	 * @see Proxy#setAddToPath(boolean)
	 */
	public void setAddToPath(boolean addToPath)
	{
		if (!addToPath)
			_pathUri = null;
		else if (_pathUri == null)
			_pathUri = newProxyURI(false);
	}
	
	/**
	 * @see Proxy#setNoCancel(boolean)
	 */
	public void setNoCancel(boolean b)
	{
		_noCancel = b;
	}

	/**
	 * @see Proxy#setOutboundInterface(InetAddress)
	 */
	public void setOutboundInterface(InetAddress address)
	{
		if (!_tx.getRequest().session().isValid())
			throw new IllegalStateException("Session not valid");
		if (address == null)
			throw new NullPointerException("Null address");
		
		// TODO
		
	}

	/**
	 * @see Proxy#setOutboundInterface(InetSocketAddress)
	 */
	public void setOutboundInterface(InetSocketAddress address)
	{
		if (!_tx.getRequest().session().isValid())
			throw new IllegalStateException("Session not valid");
		if (address == null)
			throw new NullPointerException("Null address");
		
		// TODO
	}

	/**
	 * @see Proxy#setParallel(boolean)
	 */
	public void setParallel(boolean parallel) 
    {
		_parallel = parallel;
	}
	
	/**
	 * @see Proxy#setProxyTimeout(int)
	 */
	public void setProxyTimeout(int seconds)
	{
		if (seconds <= 0)
			throw new IllegalArgumentException("Proxy timeout too low: " + seconds);
		_proxyTimeout = seconds;
	}

	/**
	 * @see Proxy#setRecordRoute(boolean)
	 */
	public void setRecordRoute(boolean recordRoute) 
    {
		if (_started)
			throw new IllegalStateException("Proxy has already been started");
		
		if (!recordRoute)
			_rrUri = null;
		else if (_rrUri == null)
			_rrUri = newProxyURI(true);
	}
	
	/**
	 * @see Proxy#setRecurse(boolean)
	 */
	public void setRecurse(boolean recurse) 
    {
		_recurse = recurse;
	}
	
	/**
	 * @see Proxy#setSequentialSearchTimeout(int)
	 * @deprecated
	 */
	public void setSequentialSearchTimeout(int seconds) 
    {
        setProxyTimeout(seconds);
	}
	
	/**
	 * @see Proxy#setStateful(boolean)
	 * @deprecated
	 */
	public void setStateful(boolean b) { }
	
	/**
	 * @see Proxy#setSupervised(boolean)
	 */
	public void setSupervised(boolean supervised) 
    {
		_supervised = supervised;
	}
	
	/**
	 * @see Proxy#startProxy()
	 */
	public void startProxy() 
    {
		_started = true;
		
		if (_tx.isCompleted())
        	throw new IllegalStateException("Transaction has completed");
		
		if (!_parallel && _actives > 0)
			return;
		
		// Need a scope here as this method can be called outside of a managed thread
		ApplicationSession session = _tx.getRequest().appSession();
    	ApplicationSessionScope scope = session.getSessionManager().openScope(session);
    	try
    	{
    		while (!_targets.isEmpty())
    		{
    			Branch branch = _targets.remove(0);

    			if (LOG.isDebugEnabled())
    				LOG.debug("Proxying to {} ", branch.getUri(), null);

    			branch.start();

    			if (!_parallel)
    				break;
    		}
    	}
    	finally
    	{
    		scope.close();
    	}
	}
	
	// ----------------------------------------------------------------
	
	private SipURI newProxyURI(boolean applicationId)
	{
		return newProxyURI(_tx.getRequest().getConnection().getConnector(), applicationId);
	}
	
	private SipURI newProxyURI(SipConnector connector, boolean applicationId)
	{		        
		SipURI rrUri = (SipURI) connector.getURI().clone();
		rrUri.setLrParam(true);
		
		if (applicationId)
		{
			ApplicationSession appSession = _tx.getRequest().appSession();
			rrUri.setParameter(SessionHandler.APP_ID, appSession.getId());
		}

		return rrUri;
	}
	
	private boolean isInTargetSet(URI uri)
	{
		if (_branches.isEmpty())
			return false;
		
		Iterator<Branch> it = new BranchIterator(_branches);
		while (it.hasNext())
		{
			if (it.next().getUri().equals(uri))
				return true;
		}
		return false;
	}
	
	protected Branch addTarget(URI uri)
	{
		URI target = uri.clone();
		if (target.isSipURI())
		{
			SipURI sipUri = (SipURI) target;
			sipUri.removeParameter("method");
			Iterator<String> it = sipUri.getHeaderNames();
			while (it.hasNext())
			{
				it.remove();
			}
		}
		if (isInTargetSet(target))
		{
			if (LOG.isDebugEnabled())
				LOG.debug("target {} is already in target set", target);
			return null;
		}
		else
		{
			if (LOG.isDebugEnabled())
				LOG.debug("adding target {} to target set", target);
			
			Branch branch = new Branch(target);
			_targets.add(branch);
			return branch;
		}
	}
	
	protected Branch addBranch(URI uri)
	{
		if (_tx.isCompleted())
			throw new IllegalStateException("transaction completed");
				
		Branch branch = addTarget(uri);
		if (branch != null)
		{
			branch.setRecurse(getRecurse());
			branch.setRecordRoute(getRecordRoute());
			branch.setAddToPath(getAddToPath());
			branch.setProxyBranchTimeout(getProxyTimeout());
			
			_branches.add(branch);
		}
		return branch;
	}
   
    public void handleCancel(ServerTransaction tx, SipRequest cancel)
    {
        cancel.setSession(_tx.getRequest().session());
		SipResponse response = new SipResponse(cancel ,SipServletResponse.SC_OK, null);
		((ServerTransaction) cancel.getTransaction()).send(response);

        cancel();
        try 
        {
            cancel.appSession().getContext().handle(cancel);
        } 
        catch (Exception e)
        {
            LOG.debug(e);
        }
    }
    
    public void transactionTerminated(Transaction transaction)
    {
		if (transaction.isServer() && transaction.getRequest().isInitial())
		{
			updateStateOnProxyComplete(true);
		}
    	_tx.getRequest().session().removeTransaction(transaction);
    }
	
	private void tryFinal()
    {
		assert (_actives == 0);
		
	    if (LOG.isDebugEnabled()) 
	    	LOG.debug("tryFinal, branches: {}, untried: {}", _branches, _targets);
        
    	if (_best == null)
    	{
			_best = (SipResponse) getOriginalRequest().createResponse(SipServletResponse.SC_REQUEST_TIMEOUT);
			_best.to().setParameter(AddressImpl.TAG, _tx.getRequest().appSession().newUASTag());
    	}
    	
        if (LOG.isDebugEnabled()) 
        	LOG.debug("final response is {}", _best, null);
        
        _best.setBranchResponse(false);
        invokeServlet(_best);
       
        if (_actives > 0)
        {
            if (LOG.isDebugEnabled()) 
            	LOG.debug("new branch(es) created in callback {}", _branches, null);
            return;
        }
        
        forward(_best);
		
		// Update state for all derived sessions
		if (_tx.getRequest().isInitial() && _tx.getRequest().isInvite())
			updateStateOnProxyComplete(false);
	}
	
    private void invokeServlet(SipResponse response)
    {
        if (_supervised)    
            response.session().invokeServlet(response);
    }
    
	private void forward(SipResponse response)
    {	
		if (response.getStatus() >= 300)
			response.session().updateState(response, false);
		// The transaction could be completed if the servlet has sent a virtual response.
		if (!_tx.isCompleted() || response.is2xx())
			_tx.send(response);
		response.setCommitted(true);
	}    
	
	
	/**
	 * 
	 * @param forceTerminated force state to terminated even if state is EARLY
	 */
	private void updateStateOnProxyComplete(boolean forceTerminated)
	{
		if (_tx.isCompleted())
		{
			Session session = _tx.getRequest().session();
			for (Session s : session.appSession().getDerivedSessions(session))
				s.updateStateOnProxyComplete(forceTerminated);
		}
	}
	
	static class TimeoutC extends ScopedRunable implements Runnable, Serializable, Cancelable
	{
		private static final long serialVersionUID = 1L;
		
		Branch _branch;
		
		public TimeoutC(Branch branch)
		{
			super(branch._request.appSession());
			_branch = branch;
		}
		
		public void doRun()
		{
			_branch.timeoutTimerC();
		}

		@Override
		public void cancel()
		{
			_session = null;
			_branch = null;
		}
	}
	
	static class BranchTimeoutTasdk extends TimeoutC
	{
    private static final long serialVersionUID = 2854010558145561212L;

    BranchTimeoutTasdk(Branch branch)
    {
      super(branch);
    }
    @Override
    public void doRun()
    {
      _branch.timeoutBranchTimeout();
    }
	  
	}
	
    class Branch implements ProxyBranch, ClientTransactionListener, Serializable
    {	
		private static final long serialVersionUID = 1L;
	
		private URI _uri;
    	private SipRequest _request;
    	private SipResponse _response;
    	
        private ClientTransaction _ctx;
        private boolean _provisional = false;
        private TimerTask _timerC;
        private TimerTask _branchTimeoutTask;

        private boolean _branchRecurse;
        
        private SipURI _branchPathUri;
        private SipURI _branchRRUri;
        
        private int _branchTimeout;
        
        private List<Branch> _recursedBranches;
        
        public Branch(URI uri)
        {
        	_uri = uri;
        	_request = new SipRequest((SipRequest) getOriginalRequest());
        	_request.setProxy(getProxy());
        	if (getOriginalRequest().isInitial())
        		_request.setRoutingDirective(SipApplicationRoutingDirective.CONTINUE, getOriginalRequest());
        }
        
        /**
         * @see ProxyBranch#cancel()
         */
        public void cancel()
        {
        	cancel(null, null, null);
        }
        
        /**
         * @see ProxyBranch#cancel(String[], int[], String[])
         */
        public void cancel(String[] protocol, int[] reasonCode, String[] reasonText)
        {
        	if (!_ctx.isCompleted())
        	{
	        	stopTimerC();
	        	stopBranchTimeout();
	            
	            SipRequest cancel = (SipRequest) _ctx.getRequest().createCancel();
	        	if (protocol != null)
	        	{
	        		for (int i = 0; i < protocol.length; i++)
	        		{
	        			ParameterableImpl reason = new ParameterableImpl();
	        			reason.setValue(protocol[i]);
	        			if (reasonCode[i] > 0)
	        				reason.setParameter("cause", String.valueOf(reasonCode[i]));
	        			if (reasonText[i] != null)
	        				reason.setParameter("reason", reasonText[i]);
	        			cancel.addParameterableHeader(SipHeader.REASON.asString(), reason, false);
	        		}
	        	}
	        	_ctx.cancel(cancel);
        	}
        	
        	if (_recursedBranches != null)
        	{
	        	for (Branch branch :_recursedBranches)
	        	{
	        		branch.cancel(protocol, reasonCode, reasonText);
	        	}
        	}
		}
        
        /**
         * @see ProxyBranch#getAddToPath()
         */
        public boolean getAddToPath() 
        {
			return  _branchPathUri != null;
		}
        
        /**
         * @see ProxyBranch#getPathURI()
         */
        public SipURI getPathURI() 
		{
			return _branchPathUri;
		}
        
        /**
         * @see ProxyBranch#getProxy()
         */
        public Proxy getProxy() 
		{
			return SipProxy.this;
		}
        
        /**
         * @see ProxyBranch#getProxyBranchTimeout()
         */
        public int getProxyBranchTimeout() 
		{
			return _branchTimeout;
		}
        
        /**
         * @see ProxyBranch#getRecordRoute()
         */
        public boolean getRecordRoute() 
		{
			return _branchRRUri != null;
		}

        /**
         * @see ProxyBranch#getRecordRouteURI()
         */
		public SipURI getRecordRouteURI() 
		{
			if (_branchRRUri == null)
				throw new IllegalStateException("Record-Routing is not enabled");
			
			return _branchRRUri;
		}
        
		/**
		 * @see ProxyBranch#getRecurse()
		 */
		public boolean getRecurse() 
		{
			return _branchRecurse;
		}
		
		/**
		 * @see ProxyBranch#getRecursedProxyBranches()
		 */
		public List<ProxyBranch> getRecursedProxyBranches() 
		{
			if (_recursedBranches == null)
				return Collections.emptyList();
			return new ArrayList<ProxyBranch>(_recursedBranches);
		}
		
		/**
		 * @see ProxyBranch#getRequest()
		 */
		public SipServletRequest getRequest() 
		{
			return _request;
		}

		/**
		 * @see ProxyBranch#getResponse()
		 */
		public SipServletResponse getResponse() 
		{
			return _response;
		}
		
		/**
		 * @see ProxyBranch#isStarted()
		 */
		public boolean isStarted() 
		{
			return _ctx != null;
		}
		
		/**
		 * @see ProxyBranch#setAddToPath(boolean)
		 */
		public void setAddToPath(boolean b) 
	    {
			if (!b)
				_branchPathUri = null;
			else if (_branchPathUri == null)
				_branchPathUri = (SipURI) (_pathUri != null ? _pathUri.clone() : newProxyURI(false));
		}
		
		/**
		 * @see ProxyBranch#setOutboundInterface(InetAddress)
		 */
		public void setOutboundInterface(InetAddress address) 
		{
			if (!_tx.getRequest().session().isValid())
				throw new IllegalStateException("Session not valid");
			if (address == null)
				throw new NullPointerException("Null address");
			
			// TODO
		}
		
		/**
		 * @see ProxyBranch#setOutboundInterface(InetSocketAddress)
		 */
		public void setOutboundInterface(InetSocketAddress address) 
		{
			if (!_tx.getRequest().session().isValid())
				throw new IllegalStateException("Session not valid");
			if (address == null)
				throw new NullPointerException("Null address");
			
			// TODO
		}

		/**
		 * @see ProxyBranch#setProxyBranchTimeout(int)
		 */
		public void setProxyBranchTimeout(int seconds) 
		{
			if (seconds <= 0 || seconds > _proxyTimeout)
				throw new IllegalArgumentException("Invalid branch timeout: " + seconds);
			_branchTimeout = seconds;
		}
		
		/**
		 * @see ProxyBranch#setRecordRoute(boolean)
		 */
		public void setRecordRoute(boolean b) 
		{
			if (isStarted())
				throw new IllegalStateException("Proxy branch is started");
			
			if (!b)
				_branchRRUri = null;
			else if (_branchRRUri == null) 
				_branchRRUri = (SipURI) (_rrUri != null ? _rrUri.clone() : newProxyURI(true));
		}
		
		/**
		 * @see ProxyBranch#setRecurse(boolean)
		 */
		public void setRecurse(boolean recurse) 
		{
			_branchRecurse = recurse;	
		}

        public URI getUri()
        {
        	return _uri;
        }
        
		protected void start()
		{
            int mf = _request.getMaxForwards();
            if (mf == -1)
                mf = __maxForwards;
            else
                mf--;
            
			_request.setMaxForwards(mf);
			_request.setRequestURI(_uri);
						
			if (_branchRRUri != null) 
				_request.addRecordRoute(new AddressImpl(_branchRRUri));
			
			if (_branchPathUri != null && _request.isRegister())
				_request.addAddressHeader(SipHeader.PATH.asString(), new AddressImpl(_branchPathUri), true);
				
			//_ctx = _request.getCallSession().getServer().sendRequest(_request, this);
			try
			{
				_ctx = _request.session().sendRequest(_request, this);
			
				if (_request.isInvite())
				{
					startTimerC();
					if (_branchTimeout < __timerC)
					{
					  // We need to do it only if branch timeout is shorter, otherwise branch would be cancelled by timer C.
					  startBranchTimeout();
					}
				}
			
				_actives++;
			}
			catch (Exception e)
			{
				LOG.debug(e);
				// TODO
			}
		}
        
        public void startTimerC()
        {
        	_timerC = _request.appSession().getSessionManager().schedule(new TimeoutC(this), __timerC * 1000);
        }
        private void startBranchTimeout()
        {
          _branchTimeoutTask = _request.appSession().getSessionManager().schedule(new TimeoutC(this), _branchTimeout * 1000);
        }
        
        public void updateTimerC()
        {
        	if (_timerC == null)
        		return; 
        	
        	_provisional = true;
        
        	_timerC.cancel();
        	_timerC = _request.appSession().getSessionManager().schedule(new TimeoutC(this), __timerC * 1000);
        }
        
        public void stopTimerC()
        {
        	if (_timerC != null)
        		_timerC.cancel();
        	_timerC = null;
        }
        public void stopBranchTimeout()
        {
          if (_branchTimeoutTask != null)
            _branchTimeoutTask.cancel();
          _branchTimeoutTask = null;
        }
        public void timeoutTimerC()
        {
        	_timerC = null;
        	LOG.debug("Timer C timed out for branch {}", _ctx.getBranch(), null);
        	if (_provisional)
        	{
        		cancel();
        	}
        	else 
        	{
        		SipResponse timeout = _ctx.create408();
        		_ctx.handleResponse(timeout);
        	}
        }
        final void timeoutBranchTimeout()
        {
          _branchTimeoutTask = null;
          LOG.debug("Branch timedout {}", this);
          if (_provisional)
          {
            cancel();
          }
          else 
          {
            SipResponse timeout = _ctx.create408();
            _ctx.handleResponse(timeout);
          }
        }

		public void handleResponse(SipResponse response) 
	    {   
			_response = response;
			
			int status = response.getStatus();
			
			if (status == SipServletResponse.SC_TRYING)
				return;
	        	        			
	        SipRequest request = _tx.getRequest();
	        
	        Session session = request.session();
	        
	        if (request.isInitial())
	        { 
	        	ApplicationSession appSession = session.appSession();
	        	// Search if there is a session with the same remote tag
	        	Session best = appSession.getSession(response);
	        	
	        	if (best == null)
	        	{
	        		// We need to create a derived session if original session has a remote tag
	        		// and the response could create a dialog
	        		if (session.getRemoteParty().getParameter(AddressImpl.TAG) != null	&& status < 300)
	        		{
	        			best = appSession.createDerivedSession(session);
						if (LOG.isDebugEnabled())
							LOG.debug("Create derived session {} from session {}", best, session);
	        		}
	        		else
	        			best = session;
	        	}
	        	session = best;
	        }
			
	        response.setSession(session);
	        
	        if (_tx.isCompleted() && !response.is2xx())
			{
	        	session.updateStateOnProxyComplete(response.getStatus() >= 300);
	        	
				if (LOG.isDebugEnabled())
					LOG.debug("Dropping response " + response.getStatus() + " since proxy is completed");
				return;
			}
	        
	        if (LOG.isDebugEnabled()) 
	        	LOG.debug("Got response {}", response);
	        
	        session.updateState(response, false);
	        if (request.isInitial())
				session.setRecordRoute(getRecordRoute());
	        
			response.removeTopVia();
			response.setProxyBranch(this);
			
			if (status < 200)
	        {
				if (response.isInvite())
					updateTimerC();
	            
				invokeServlet(response);
				forward(response);
			} 
	        else 
	        {
	        	_actives--;
	        	
	        	stopTimerC();
	        	stopBranchTimeout();
	            
				if ((300 <= status && status < 400) && _branchRecurse) 
	            {
					try 
	                {
						Iterator<Address> it = response.getAddressHeaders(SipHeader.CONTACT.asString());
						while (it.hasNext()) 
	                    {
							Address contact = (Address) it.next();
							if (contact.getURI().isSipURI()) 
							{
								Branch branch = addTarget(contact.getURI());
								if (branch != null)
								{
									if (_recursedBranches == null)
										_recursedBranches = new ArrayList<SipProxy.Branch>(1);
									_recursedBranches.add(branch);
									branch.setRecurse(_branchRecurse);
									branch.setRecordRoute(getRecordRoute());
									branch.setAddToPath(getAddToPath());
								}
							}
						}
					} 
	                catch (ServletParseException e)
	                {
						LOG.ignore(e);
					}
	            }
				
				if (_best == null || 
						(_best.getStatus() < 600 && (status < _best.getStatus() || status >= 600))) 
	            {
					_best = response;
				}
				
				if (status >= 600) 
	            {
					SipProxy.this.doCancel(null, null, null);
				}
				
				if (status < 300) 
	            {
					updateStateOnProxyComplete(false);	
					
	                invokeServlet(response);
					forward(response);
					
					SipProxy.this.doCancel(null, null, null);
				}
	            else 
	            {
	            	if (!_targets.isEmpty())
	            		startProxy();
	            	
	            	if (_actives > 0)
	            	{
	            		response.setBranchResponse(true);
	            		invokeServlet(response);
	            	}
	            	else
	            	{
	            		tryFinal();
	            	}
	            }
			}
		}
		
		public void transactionTerminated(Transaction transaction)
		{	
			transaction.getRequest().session().removeTransaction(transaction);
		}

		public void customizeRequest(SipRequest request, SipConnection connection)
		{
			SipConnector connector = _tx.getRequest().getConnection().getConnector();
			if (getRecordRoute() && connection.getConnector() != connector)
			{
				SipURI rrUri = newProxyURI(connection.getConnector(), true);
				rrUri.setParameter(SipGrammar.DRR, "2");
				_branchRRUri.setParameter(SipGrammar.DRR, "");
				if (connector.getTransport() == Transport.TCP)
					_branchRRUri.setTransportParam("tcp");
				if (connection.getConnector().getTransport() == Transport.TCP)
					rrUri.setTransportParam("tcp");
				request.addRecordRoute(new AddressImpl(rrUri));
			}
		}
	
    }
	
	class BranchIterator implements Iterator<Branch>
	{
		private Iterator<Branch> _it;
		
		private List<Branch> _branches;
		private Branch _next;
		private int _index;
		
		public BranchIterator(List<Branch> branches)
		{
			_branches = branches;
			_index = 0;
		}

		public boolean hasNext()
		{
			if (_next == null)
			{
				if (_it != null && _it.hasNext())
				{
					_next = _it.next();
				}
				else
				{
					if (_index < _branches.size())
					{
						Branch branch = _branches.get(_index++);
						_next = branch;
						if (branch._recursedBranches != null && !branch._recursedBranches.isEmpty())
						{
							_it = new BranchIterator(branch._recursedBranches);
						}
					}
				}
			}
			return _next != null;
		}

		public Branch next() 
		{
			if (hasNext())
			{
				Branch next = _next;
				_next = null;
				return next;
			}
			throw new NoSuchElementException();
		}

		public void remove() 
		{	
		}
	}
}
