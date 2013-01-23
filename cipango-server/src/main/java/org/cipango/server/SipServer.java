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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletException;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import org.cipango.server.dns.Hop;
import org.cipango.server.log.AccessLog;
import org.cipango.server.nio.UdpConnector;
import org.cipango.server.processor.TransportProcessor;
import org.cipango.server.transaction.RetryableTransactionManager;
import org.cipango.server.transaction.TransactionManager;
import org.cipango.sip.Via;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ArrayUtil;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

@ManagedObject("SIP server")
public class SipServer extends ContainerLifeCycle
{
	private static final Logger LOG = Log.getLogger(SipServer.class);

	private static final String __version;
	
	private final ThreadPool _threadPool;
	private SipConnector[] _connectors;
	
	private SipHandler _handler;
	private SipProcessor _processor;
	private TransportProcessor _transportProcessor;
	private TransactionManager _transactionManager;
	private AccessLog _accessLog;
    private final AtomicLong _messagesReceived = new AtomicLong();
    private final AtomicLong _messagesSent = new AtomicLong();
    private Server _server;
		
	static 
	{
		if (SipServer.class.getPackage() != null && SipServer.class.getPackage().getImplementationVersion() != null)
			__version = SipServer.class.getPackage().getImplementationVersion();
		else
			__version = System.getProperty("cipango.version", "3.x.y-SNAPSHOT");
	}
	
	public SipServer()
	{
		this(null);
	}
	
	public SipServer(@Name("port") int port)
	{
		this(null);
		SipConnector connector = new UdpConnector(this);
		connector.setPort(port);
		setConnectors(new SipConnector[] {connector});
	}
	
	public SipServer(@Name("threadpool") ThreadPool pool)
	{
		this(pool, null);
	}
	
	public SipServer(@Name("threadpool") ThreadPool pool, TransactionManager transactionManager)
	{
        _threadPool = pool != null? pool: new QueuedThreadPool();
        addBean(_threadPool);
        
        _transactionManager = transactionManager != null ? transactionManager : new RetryableTransactionManager();
		_transportProcessor = new TransportProcessor(_transactionManager);
		_transactionManager.setTransportProcessor(_transportProcessor);
		addBean(_transactionManager);
		addBean(_transportProcessor);
		
		_processor = _transportProcessor;
		_processor.setServer(this);
	}
	
	@Override
	protected void doStart() throws Exception
	{
		LOG.info("cipango-" + __version);
				
		MultiException mex = new MultiException();

        try
        {
            super.doStart();
        }
        catch(Throwable e)
        {
            mex.add(e);
        }
               
		_handler.start();
        
		if (_connectors != null)
		{
			for (int i = 0; i <  _connectors.length; i++)
			{
				try 
				{
					_connectors[i].start();
				}
				catch (Exception e)
				{
					mex.add(e);
				}
			}
		}
				
		mex.ifExceptionThrow();
	}
	
	protected void doStop() throws Exception
	{
		MultiException mex = new MultiException();
		
		_handler.stop();
		
		if (_connectors != null)
		{
			for (int i = _connectors.length; i-->0;)
			{
				try
				{
					_connectors[i].stop();
				}
				catch (Exception e)
				{
					mex.add(e);
				}
			}
		}
			
		super.doStop();
		
		mex.ifExceptionThrow();
	}
	
	@ManagedAttribute("Cipango version")
	public String getVersion()
	{
		return __version;
	}
	
	@ManagedAttribute("SIP connectors")
	public SipConnector[] getConnectors()
    {
        return _connectors;
    }
	
	public void addConnector(SipConnector connector)
    {
        setConnectors((SipConnector[])ArrayUtil.addToArray(getConnectors(), connector, SipConnector.class));
    }
	
	public void setConnectors(SipConnector[] connectors)
    {
        updateBeans(_connectors, connectors);
        _connectors = connectors;
    }
	
	@ManagedAttribute("Thread pool")
	public ThreadPool getThreadPool()
	{
		return _threadPool;
	}
	
	public void setHandler(SipHandler handler)
	{
		if (handler != null)
			handler.setServer(this);

		// FIXME Could not use the method updateBean as the handler might be already started if it is a SipAppContext
		if (_handler != handler)
		{
			if (_handler != null)
				removeBean(_handler);
			
			if (handler != null)
				addBean(handler, true);
		}
		_handler = handler;
	}
	
	@ManagedAttribute(value="Handler", readonly=true)
	public SipHandler getHandler()
	{
		return _handler;
	}
	
	public void process(final SipMessage message)
	{
		_threadPool.execute(new Runnable() 
		{
			public void run() 
			{
				try
				{
					messageReceived(message);
					_processor.doProcess(message);
				}
				catch (Exception e)
				{
					LOG.warn("Failed to handle message", e);
					// TODO 500
				}
			}
		});
	}
	
	public void handle(SipMessage message) throws IOException, ServletException
	{
		try 
		{
			_handler.handle(message);
		}
		catch (Exception e)
		{
			LOG.debug(e);
		}
	}
	
    public boolean isLocalURI(URI uri)
    {
        if (!uri.isSipURI())
            return false;
        
        SipURI sipUri = (SipURI) uri;

        if (!sipUri.getLrParam())
            return false;

        String host = sipUri.getHost();
        
        // Normalize IPv6 address
		if (host.indexOf("[") != -1) 
		{
			try
			{
				host = InetAddress.getByName(host).getHostAddress();
			}
			catch (UnknownHostException e)
			{
				LOG.ignore(e);
			}
		}
		
        for (int i = 0; i < _connectors.length; i++)
        {
            SipConnector connector = _connectors[i];
            
            String connectorHost = connector.getURI().getHost();
            
            boolean samePort = connector.getPort() == sipUri.getPort() || sipUri.getPort() == -1;
            if (samePort)
            {
	            if ((connectorHost.equals(host) || connector.getAddress().getHostAddress().equals(host))) 
	            {
	            	if (sipUri.getPort() != -1)
	            		return true;
	            	
	            	// match on host address and port is not set ==> NAPTR case
	            	if (connector.getAddress().getHostAddress().equals(host)
	            			&& connector.getPort() != connector.getTransport().getDefaultPort())
	            	{
	            		return false;
	            	}
	            	return true;
	            }
            }
        }
        return false;
    }
	
	public void sendResponse(SipResponse response, SipConnection connection) throws IOException
	{
		try
		{
	    	if (connection == null || !connection.getConnector().getTransport().isReliable()
	    			|| !connection.isOpen())
	    	{
	    		Via via = response.getTopVia();
	    		
	    		SipConnector connector = null;
	    		InetAddress address = null;
	    		
	    		if (connection != null)
	    		{
	    			connector = connection.getConnector();
	    			address = connection.getRemoteAddress();
	    		}
	    		else
	    		{
	    			if (via.hasMAddr())
	    				address = InetAddress.getByName(via.getMAddr());
	    			else
	    				address = InetAddress.getByName(via.getHost());
	    			connector = _transactionManager.getTransportProcessor().findConnector(
	    					Transport.valueOf(via.getTransport()), address);
	    		}
	    		
				int port = -1;
		        if (via.hasRPort()) 
		        {
		            port = via.getRPort();
		        } 
		        else 
		        {
		            port = via.getPort();
		            if (port == -1) 
		                port = connection.getConnector().getTransport().getDefaultPort();
		        }
		        connection = connector.getConnection(address, port);
		        
		        if (connection == null)
		        	throw new IOException("Could not found any SIP connection to " 
		        			+ address + ":" + port + "/" + connector.getTransport());
	    	}
			connection.send(response);
			messageSent(response, connection);
		}
		catch (MessageTooLongException e)
		{
			if (connection instanceof UdpConnector.UdpConnection)
			{
				try
				{
					LOG.debug("Failed to send response has it is bigger than MTU, try with UDP maximum size");
					((UdpConnector.UdpConnection) connection).send(response, true);
					messageSent(response, connection);
					return;
				}
				catch (MessageTooLongException e1)
				{
				}
			}

			// TODO  apply procedure described in RFC 3263 §5
			LOG.warn(e);
		}
	}
	
	public void sendRequest(SipRequest request, SipConnection connection) throws IOException
	{
		try
		{
			connection.send(request);
			messageSent(request, connection);
		}
		catch (MessageTooLongException e)
		{
			if (!connection.getConnector().getTransport().isReliable())
			{
				LOG.debug("Message is too large.");
				ListIterator<Hop> hops = request.getHops();
				
				try
				{
					request.setHops(null); // This ensure that hops will be resolved with TCP for transport
					SipConnection newConnection = _transactionManager.getTransportProcessor().getConnection(request, Transport.TCP);
					if (newConnection.getConnector().getTransport().isReliable())
					{
						LOG.debug("Switching to TCP.");
						sendRequest(request, newConnection);
					}
				}
				catch (IOException io) 
				{
					LOG.warn("Failed to switch to TCP.");
					request.setHops(hops);
					// TODO force sending on initial transport
					
					// 	Update via to ensure that right value is used in logs
					Via via = request.getTopVia();
					SipConnector connector = connection.getConnector();
					via.setTransport(connector.getTransport().getName());
					via.setHost(connector.getURI().getHost());
					via.setPort(connector.getURI().getPort());
					if (connection instanceof UdpConnector.UdpConnection)
					{
						try
						{
							((UdpConnector.UdpConnection) connection).send(request, true);
							messageSent(request, connection);
						}
						catch (MessageTooLongException e1)
						{
							throw new IOException(e);
						}
					}
				}
			}
			else
				LOG.warn(e);
		}
	}
	
	protected void messageReceived(SipMessage message)
	{
		_messagesReceived.incrementAndGet();
		if (_accessLog != null)
			_accessLog.messageReceived(message, message.getConnection());
	}
	
	protected void messageSent(SipMessage message, SipConnection connection)
	{
		_messagesSent.incrementAndGet();
		if (_accessLog != null)
			_accessLog.messageSent(message, connection);
	}
	
	public TransactionManager getTransactionManager()
	{
		return _transactionManager;
	}

	public TransportProcessor getTransportProcessor()
	{
		return _transportProcessor;
	}

	@ManagedAttribute(value="Access log", readonly=true)
	public AccessLog getAccessLog()
	{
		return _accessLog;
	}

	public void setAccessLog(AccessLog accessLog)
	{
		updateBean(_accessLog, accessLog);
		_accessLog = accessLog;
	}

	@ManagedAttribute("Messages received")
	public long getMessagesReceived()
	{
		return _messagesReceived.get();
	}

	@ManagedAttribute("Messages sent")
	public long getMessagesSent()
	{
		return _messagesSent.get();
	}
	
	@ManagedOperation(value="Reset statistics", impact="ACTION")
	public void statsReset()
	{
		_messagesReceived.set(0);
		_messagesSent.set(0);
	}
	
	public void setServer(Server server)
	{
		if (server != null && _server != server)
		{
			if (isStarted())
				throw new IllegalStateException("Started");
			
			_server = server;
			// Transport should be started after servlet application init
			// So sip server is started once jetty server started. This is done using lifecycle listener
			_server.addLifeCycleListener(new ServerListener());
			// Not managed to prevent automatic start.
			_server.addBean(this, false);
		}
	}

	
	private class ServerListener extends AbstractLifeCycleListener
	{

		@Override
		public void lifeCycleStarted(LifeCycle event)
		{
			try
			{
				// In order to be expose by JMX, the SIP server should be managed
				_server.manage(SipServer.this);
				SipServer.this.start();
			}
			catch (RuntimeException e)
			{
				throw e;
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		@Override
		public void lifeCycleStopping(LifeCycle event)
		{
			try
			{
				SipServer.this.stop();
			}
			catch (RuntimeException e)
			{
				throw e;
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}
}
