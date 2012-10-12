package org.cipango.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Executor;

import javax.servlet.sip.SipURI;

import org.cipango.sip.SipURIImpl;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject("SIP connector")
public abstract class AbstractSipConnector extends ContainerLifeCycle  implements SipConnector
{
	private static final Logger LOG = Log.getLogger(AbstractSipConnector.class);
	
	private int _port;
	private String _host;
	private SipURI _uri;
	
	private Thread[] _acceptors;
	
	private final SipServer _server;
    private final Executor _executor;

	public AbstractSipConnector(SipServer server, Executor executor, int acceptors)
	{
        _server = server;
        _executor = executor != null? executor: _server.getThreadPool();

        addBean(_server,false);
        addBean(_executor);
        
        // Don't manage executor if inherited from the server.
        if (executor == null)
            unmanage(_executor);

        if (acceptors <= 0)
            acceptors = Math.max(1, (Runtime.getRuntime().availableProcessors()) / 2);
        if (acceptors > 2 * Runtime.getRuntime().availableProcessors())
            LOG.warn("{}: Acceptors should be <= 2*availableProcessors", this);
        _acceptors = new Thread[acceptors];
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
	
	public Executor getExecutor()
	{
		return _executor;
	}
	
	public SipServer getServer()
	{
		return _server;
	}
	
	@ManagedAttribute(value="Acceptors", readonly=true)
	public int getAcceptors()
	{
		return _acceptors.length;
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

        super.doStart();

        open();
        
        synchronized(this)
        {
            for (int i = 0; i < _acceptors.length; i++)
            	getExecutor().execute(new Acceptor(i));
        }
        
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
                if (_acceptors == null)
                    return;
                _acceptors[_acceptor] = current;
            }
            String name = _acceptors[_acceptor].getName();
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
                    if (_acceptors != null)
                    	_acceptors[_acceptor] = null;
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
