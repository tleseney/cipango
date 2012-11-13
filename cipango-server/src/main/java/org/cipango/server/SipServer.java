package org.cipango.server;

import java.io.IOException;
import java.net.InetAddress;

import javax.servlet.ServletException;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import org.cipango.server.handler.AbstractSipHandler;
import org.cipango.server.nio.UdpConnector;
import org.cipango.server.processor.TransportProcessor;
import org.cipango.server.transaction.TransactionManager;
import org.cipango.sip.Via;
import org.eclipse.jetty.util.ArrayUtil;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
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
	private TransactionManager _transactionManager;
		
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
        _threadPool = pool != null? pool: new QueuedThreadPool();
        addBean(_threadPool);
	}
	
	@Override
	protected void doStart() throws Exception
	{
		LOG.info("cipango-" + __version);
		
		//_processor = new TransportProcessor(new SessionProcessor(new TransactionProcessor(new SipSessionProcessor())));
		_transactionManager = new TransactionManager();
		_processor = new TransportProcessor(_transactionManager);
		_transactionManager.setTransportProcessor((TransportProcessor) _processor);
		addBean(_transactionManager);
		addBean(_processor);
		
		_processor.setServer(this);
		_processor.start();
				
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
		updateBean(_handler, handler);
		_handler = handler;
	}
	
	public SipHandler getHandler()
	{
		return _handler;
	}
	
	public void process(final SipMessage message)
	{
		_threadPool.dispatch(new Runnable() 
		{
			public void run() 
			{
				try
				{
					_processor.doProcess(message);
				}
				catch (Exception e)
				{
					e.printStackTrace();
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
			e.printStackTrace();
		}
			//_handler.handle(message);
		
		/*
		if (message.isRequest())
		{	
			SipRequest request = (SipRequest) message;
			SipResponse response = (SipResponse) request.createResponse(200);
			
			((ServerTransaction) request.getTransaction()).send(response);

			/*
			ByteBuffer out = _buffers.getBuffer();
			
			int position = BufferUtil.flipToFill(out);
		
			new SipGenerator().generateResponse(out, response.getStatus(), response.getReasonPhrase(), response.getFields());
			
			BufferUtil.flipToFlush(out, position);
			
			try
			{
				message.getConnection().write(out);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			_buffers.returnBuffer(out);
			
			//System.out.println(BufferUtil.toString(out));
		} */
	}
	
	public boolean isLocalURI(URI uri)
	{
		if (uri.isSipURI())
			return false;
		
		SipURI sipURI = (SipURI) uri;
		
		return true; // TODO
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
		}
		catch (MessageTooLongException e)
		{
			LOG.warn(e);
		}
	}
	
	public void sendRequest(SipRequest request, SipConnection connection) throws IOException
	{
		try
		{
			connection.send(request);
		}
		catch (MessageTooLongException e)
		{
			if (!connection.getConnector().getTransport().isReliable())
			{
				LOG.debug("Message is too large.");
				try
				{
					SipConnection newConnection = _transactionManager.getTransportProcessor().getConnection(
							request, Transport.TCP, connection.getRemoteAddress(), connection.getRemotePort());
					if (newConnection.getConnector().getTransport().isReliable())
					{
						LOG.debug("Switching to TCP.");
						sendRequest(request, newConnection);
					}
				}
				catch (IOException io) 
				{
					LOG.warn("Failed to switch to TCP.");

					// 	Update via to ensure that right value is used in logs
					Via via = request.getTopVia();
					SipConnector connector = connection.getConnector();
					via.setTransport(connector.getTransport().getName());
					via.setHost(connector.getURI().getHost());
					via.setPort(connector.getURI().getPort());
				}
			}
			else
				LOG.warn(e);
		}
	}
	
	public TransactionManager getTransactionManager()
	{
		return _transactionManager;
	}

}
