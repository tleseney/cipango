package org.cipango.server;

import java.io.IOException;
import java.net.InetAddress;

import javax.servlet.sip.SipURI;

import org.cipango.sip.SipURIImpl;
import org.eclipse.jetty.util.component.AggregateLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.ThreadPool;

public abstract class AbstractSipConnector extends AggregateLifeCycle implements SipConnector
{
	private static final Logger LOG = Log.getLogger(AbstractSipConnector.class);
	
	private int _port;
	private String _host;
	private SipURI _uri;
	
	private int _acceptors = 1;
	private Thread[] _acceptorThreads;
	
	private SipServer _server;
	private ThreadPool _threadPool;

	public AbstractSipConnector()
	{
		this((Runtime.getRuntime().availableProcessors() + 1) / 2);
	}
	
	public AbstractSipConnector(int acceptors)
	{
		_acceptors = acceptors;
	}
	
	public int getPort()
	{
		return _port;
	}
	
	public void setPort(int port)
	{
		if (isRunning())
			throw new IllegalStateException("running");
		_port = port;
	}
	
	public String getHost()
	{
		return _host;
	}
	
	public void setHost(String host)
	{
		if (isRunning())
			throw new IllegalStateException("running");
		_host = host;
	}
	
	public void setThreadPool(ThreadPool pool)
	{
		_threadPool = pool;
	}
	
	public ThreadPool getThreadPool()
	{
		return _threadPool;
	}
	
	public SipServer getServer()
	{
		return _server;
	}
	
	public void setServer(SipServer server)
	{
		_server = server;
	}
	
	public int getAcceptors()
	{
		return _acceptors;
	}
	
	public void setAcceptors(int acceptors)
	{
		_acceptors = acceptors;
	}
	
	@Override
	protected void doStart() throws Exception
	{
		if (_port <= 0)
			_port = getTransport().getDefaultPort();
		
		if (_host == null)
		{
			try
			{
				_host = InetAddress.getLocalHost().getHostAddress();
			}
			catch (Exception e)
			{
				LOG.ignore(e);
				_host = "127.0.0.1";
			}
		}
		
		_uri = new SipURIImpl(_host, _port);
		
		if (_threadPool == null && getServer() != null)
        	_threadPool = getServer().getThreadPool();
        
        if (_threadPool instanceof LifeCycle)
        {
        	if (getServer() == null || _threadPool != getServer().getThreadPool())
        		((LifeCycle) _threadPool).start();
        }

        open();
        
        synchronized(this)
        {
            _acceptorThreads = new Thread[getAcceptors()];

            for (int i = 0; i < _acceptorThreads.length; i++)
            {
                if (!_threadPool.dispatch(new Acceptor(i)))
                {
                    LOG.warn("insufficient maxThreads configured for {}", this);
                    break;
                }
            }
        }
        
        super.doStart();
        LOG.info("Started {}", this);
	}
	
	public SipURI getURI()
	{
		return _uri;
	}
	
	protected abstract void accept(int id) throws IOException;
	
	class Acceptor implements Runnable
	{
		int _acceptor = 0;
		
		Acceptor(int id)
		{
			_acceptor = id;
		}
		
		public void run() 
        {
        	Thread current = Thread.currentThread();
            synchronized(AbstractSipConnector.this)
            {
                if (_acceptorThreads == null)
                    return;
                _acceptorThreads[_acceptor] = current;
            }
            String name = _acceptorThreads[_acceptor].getName();
            current.setName(name + " - Acceptor" + _acceptor + " " + AbstractSipConnector.this);
            int oldPriority = current.getPriority();
            
            try 
            {
                while (isRunning())
                {
                    try 
                    {
                        accept(_acceptor);
                    } 
                    catch (IOException ioe) 
                    {
                        LOG.ignore(ioe);
                    } 
                    catch (Exception e) 
                    {
                        LOG.warn(e);
                    }
                }
            } 
            finally 
            {
            	current.setPriority(oldPriority);
                current.setName(name);
                try
                {
                    if (_acceptor == 0)
                        close();
                }
                catch (IOException e)
                {
                    LOG.warn(e);
                }
                
                synchronized(AbstractSipConnector.this)
                {
                    if (_acceptorThreads != null)
                    	_acceptorThreads[_acceptor] = null;
                }
            }
        }
	}
	
	@Override
	public String toString()
	{
		String name = getClass().getSimpleName();
		
		return name + "@" + getHost() + ":" + getPort();
	}
}
