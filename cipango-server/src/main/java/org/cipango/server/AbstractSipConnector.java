package org.cipango.server;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.servlet.sip.SipURI;

import org.cipango.sip.AddressImpl;
import org.cipango.sip.SipHeader;
import org.cipango.sip.SipMethod;
import org.cipango.sip.SipParser;
import org.cipango.sip.SipURIImpl;
import org.cipango.sip.SipVersion;
import org.cipango.sip.URIFactory;
import org.cipango.sip.Via;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
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
        
        for (int i = 0; i < _acceptors.length; i++)
        	getExecutor().execute(new Acceptor(i));
        
        LOG.info("Started {}", this);
	}
	
    protected void doStop() throws Exception 
    {
        try { close(); } catch(IOException e) { LOG.warn(e); }

        if (_server == null || getExecutor() != _server.getThreadPool())
        {
        	if (_executor instanceof LifeCycle)
        		((LifeCycle) _executor).stop();
        }

        // Tell the acceptors we are stopping
        interruptAcceptors();

        super.doStop();

        LOG.info("Stopped {}", this);
    }
    
    protected void interruptAcceptors()
    {
        for (Thread thread : _acceptors)
            if (thread != null)
                thread.interrupt();
    }
    
    public void join() throws InterruptedException
    {
        join(0);
    }

    public void join(long timeout) throws InterruptedException
    {
        for (Thread thread : _acceptors)
            if (thread != null)
                thread.join(timeout);
    }
    
	public SipURI getURI()
	{
		return _uri;
	}
	
	@Override
	public String toString()
	{
		String name = getClass().getSimpleName();
		
		return name + "@" + getHost() + ":" + getPort();
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
                   	_acceptors[_acceptor] = null;
                }
            }
        }
	}
	
	public static class MessageBuilder implements SipParser.SipMessageHandler
	{
		protected SipServer _server;
		protected SipConnection _connection;
		protected SipMessage _message;

		public MessageBuilder(SipServer server, SipConnection connection)
		{
			_server = server;
			_connection = connection;
		}
		
		@Override
		public boolean startRequest(String method, String uri,
				SipVersion version) throws ParseException
		{
			SipRequest request = new SipRequest();
			
			request.setMethod(SipMethod.CACHE.get(method), method);
			request.setRequestURI(URIFactory.parseURI(uri));
			
			_message = request;
			
			return false;
		}

		@Override
		public boolean startResponse(SipVersion version, int status,
				String reason) throws ParseException
		{
			SipResponse response = new SipResponse();
			
			response.setStatus(status, reason);
			
			_message = response;
			
			return false;
		}

		@Override
		public boolean parsedHeader(SipHeader header, String name, String value)
		{
			Object o = value;
			
			try
			{	
				if (header != null)
				{
					switch (header.getType())
					{
					case VIA:
						Via via = new Via(value);
						via.parse();
						o = via;
						break;
					case ADDRESS:
						AddressImpl addr = new AddressImpl(value);
						addr.parse();
						o = addr;
						break;
					default:
						break;
					}
				}
			}
			catch (ParseException e)
			{
				LOG.warn(e);
				return true;
			}
			
			if (header != null)
				name = header.asString();
			if (o == null) // FIXME where this case should be handle
				o = "";
			
			_message.getFields().add(name, o, false);
			
			return false;
		}

		@Override
		public boolean headerComplete()
		{
			return false;
		}

		@Override
		public boolean messageComplete(ByteBuffer content)
		{
			_message.setConnection(_connection);
			_message.setTimeStamp(System.currentTimeMillis());
			
			if (_server != null)
				_server.process(_message);

			reset();
        	return true;
		}

		@Override
		public void badMessage(int status, String reason)
		{
			LOG.debug("Bad message: {} {}", status, reason);

//			if (_message != null && _message.isRequest())
//			{
//				SipRequest request = (SipRequest) _message;
//				SipResponse response = (SipResponse) request.createResponse(
//						status, reason);
//				_connection.send(response);
//			}
			reset();
		}
		
		public SipMessage getMessage()
		{
			return _message;
		}
		
		protected void reset()
		{
			_message = null;
		}
	}
}
