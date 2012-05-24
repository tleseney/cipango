package org.cipango.server;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.servlet.ServletException;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import org.cipango.server.nio.UdpConnector;

import org.cipango.server.processor.TransportProcessor;
import org.cipango.server.sipapp.SipContextHandlerCollection;
import org.cipango.server.transaction.ServerTransaction;
import org.cipango.server.transaction.TransactionManager;
import org.cipango.sip.SipGenerator;
import org.eclipse.jetty.io.Buffers;
import org.eclipse.jetty.io.PooledBuffers;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.LazyList;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

public class SipServer extends AbstractLifeCycle
{
	private static final Logger LOG = Log.getLogger(SipServer.class);

	private static final String __version;
	
	private ThreadPool _threadPool;
	private SipConnector[] _connectors;
	
	private SipHandler _handler;
	private SipProcessor _processor;
		
	private Buffers _buffers = new PooledBuffers(Buffers.Type.INDIRECT, 0, Buffers.Type.INDIRECT, 65535, Buffers.Type.INDIRECT, 10);
	static 
	{
		if (SipServer.class.getPackage() != null && SipServer.class.getPackage().getImplementationVersion() != null)
			__version = SipServer.class.getPackage().getImplementationVersion();
		else
			__version = System.getProperty("cipango.version", "3.x.y-SNAPSHOT");
	}
	
	public SipServer()
	{
	}
	
	public SipServer(int port)
	{
		SipConnector connector = new UdpConnector();
		connector.setPort(port);
		setConnectors(new SipConnector[] {connector});
	}
	
	public void setThreadPool(ThreadPool threadPool)
	{
		_threadPool = threadPool;
	}
	
	@Override
	protected void doStart() throws Exception
	{
		LOG.info("cipango-" + __version);
		
		if (_threadPool == null)
			setThreadPool(new QueuedThreadPool());
		
		if (_threadPool instanceof LifeCycle)
			((LifeCycle) _threadPool).start();
		
		//_processor = new TransportProcessor(new SessionProcessor(new TransactionProcessor(new SipSessionProcessor())));
		_processor = new TransportProcessor(new TransactionManager());
		
		_processor.setServer(this);
		_processor.start();
		
		_handler.start();
		
		MultiException mex = new MultiException();
		
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
	
	public SipConnector[] getConnectors()
    {
        return _connectors;
    }
	
	public void addConnector(SipConnector connector)
    {
        setConnectors((SipConnector[])LazyList.addToArray(getConnectors(), connector, SipConnector.class));
    }
	
	public void setConnectors(SipConnector[] connectors)
    {
        if (connectors!=null)
        {
            for (int i=0; i < connectors.length; i++)
                connectors[i].setServer(this);
        }

        //_container.update(this, _connectors, connectors, "connector");
        _connectors = connectors;
    }
	
	public ThreadPool getThreadPool()
	{
		return _threadPool;
	}
	
	public void setHandler(SipHandler handler)
	{
		_handler = handler;
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
	
	public void sendResponse(SipResponse response, SipConnection received)
	{
		if (received == null)
		{
			
		}
		send(response, received);
	}
	
	public void send(SipMessage message, SipConnection connection)
	{
		connection.send(message);
	}
}
