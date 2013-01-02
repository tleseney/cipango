package org.cipango.server.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import org.cipango.server.AbstractSipConnector;
import org.cipango.server.SipConnection;
import org.cipango.server.SipServer;
import org.cipango.server.Transport;
import org.cipango.server.servlet.DefaultServlet;
import org.cipango.server.sipapp.SipAppContext;
import org.cipango.server.transaction.Transaction;
import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.SelectChannelEndPoint;
import org.eclipse.jetty.io.SelectorManager;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.TimerScheduler;

public class SelectChannelConnector extends AbstractSipConnector
{
	private static final Logger LOG = Log.getLogger(SelectChannelConnector.class);

	public static final int DEFAULT_SO_TIMEOUT = 2 * Transaction.__T1 * 64;

    private final Scheduler _scheduler;
    private final ByteBufferPool _byteBufferPool;
    private final SelectorManager _manager;

    private ServerSocketChannel _acceptChannel;
    private int _localPort = -1;
    
    private volatile int _acceptQueueSize = 128;
    private volatile long _idleTimeout = DEFAULT_SO_TIMEOUT;
    private volatile boolean _reuseAddress = true;
    private volatile int _soLingerTime = -1;
    private InetAddress _address;

    private final ConcurrentMap<String, SipConnection> _connections;

    public SelectChannelConnector(
    		@Name("sipServer") SipServer server)
    {
        this(server,
        		Math.max(1,(Runtime.getRuntime().availableProcessors())/4),
        		Math.max(1,(Runtime.getRuntime().availableProcessors())/4));
    }

    public SelectChannelConnector(
            @Name("sipServer") SipServer server,
            @Name("acceptors") int acceptors,
            @Name("selectors") int selectors)
    {
        this(server, null, null, null, acceptors, selectors);
    }
    
    public SelectChannelConnector(
            @Name("sipServer") SipServer server,
            @Name("executor") Executor executor,
            @Name("scheduler") Scheduler scheduler,
            @Name("bufferPool") ByteBufferPool pool,
            @Name("acceptors") int acceptors,
            @Name("selectors") int selectors)
    {
    	super(server, executor, acceptors);

		_scheduler = scheduler != null? scheduler: new TimerScheduler();
		_byteBufferPool = pool != null? pool: new ArrayByteBufferPool(
				SelectSipConnection.MINIMAL_BUFFER_LENGTH, 4096, 65536);
        _manager = new ConnectorSelectorManager(getExecutor(), _scheduler, selectors);

        _connections = new ConcurrentHashMap<String, SipConnection>();
        addBean(_manager, true);
        addBean(_scheduler, true);
    }
    
    public int getLocalPort()
    {
        synchronized(this)
        {
            return _localPort;
        }
    }
    
    public ByteBufferPool getByteBufferPool()
    {
        return _byteBufferPool;
    }
    
    /**
     * @return Returns the acceptQueueSize.
     */
    public int getAcceptQueueSize()
    {
        return _acceptQueueSize;
    }

    /**
     * @param acceptQueueSize The acceptQueueSize to set.
     */
    public void setAcceptQueueSize(int acceptQueueSize)
    {
        _acceptQueueSize = acceptQueueSize;
    }
    
    /**
     * @return Returns the maxIdleTime.
     */
    public long getIdleTimeout()
    {
        return _idleTimeout;
    }

    /**
     * @param idleTimeout The idleTimeout to set.
     * @see AbstractConnector
     */
    public void setIdleTimeout(long idleTimeout)
    {
        _idleTimeout = idleTimeout;
    }
    
    /**
     * @return True if the the server socket will be opened in SO_REUSEADDR mode.
     */
    public boolean getReuseAddress()
    {
        return _reuseAddress;
    }

    /**
     * @param reuseAddress True if the the server socket will be opened in SO_REUSEADDR mode.
     */
    public void setReuseAddress(boolean reuseAddress)
    {
        _reuseAddress = reuseAddress;
    }
    
    /**
     * @return Returns the soLingerTime.
     */
    public int getSoLingerTime()
    {
        return _soLingerTime;
    }
    
    /**
     * @param soLingerTime The soLingerTime to set or -1 to disable.
     */
    public void setSoLingerTime(int soLingerTime)
    {
        _soLingerTime = soLingerTime;
    }
    
    public Scheduler getScheduler()
    {
        return _scheduler;
    }

	@Override
	public Transport getTransport()
	{
		return Transport.TCP;
	}
	
	@Override
	public void open() throws IOException
	{
        synchronized(this)
        {
            if (_acceptChannel == null)
            {
                _acceptChannel = ServerSocketChannel.open();
                _acceptChannel.configureBlocking(true);
                _acceptChannel.socket().setReuseAddress(getReuseAddress());
                
                _address = InetAddress.getByName(getHost());
                InetSocketAddress addr = new InetSocketAddress(_address, getPort());
                _acceptChannel.socket().bind(addr, getAcceptQueueSize());
                _localPort = _acceptChannel.socket().getLocalPort();
                if (_localPort <= 0)
                    throw new IOException("Server channel not bound");
            }
        }
	}

	@Override
	public void close() throws IOException
	{
		synchronized(this)
		{
            if (_acceptChannel != null)
            {
                if (_acceptChannel.isOpen())
                    _acceptChannel.close();
            }
            _acceptChannel = null;
            _localPort = -1;
		}
	}

	@Override
	protected void accept(int id) throws IOException
	{
        ServerSocketChannel server;
        synchronized(this)
        {
            server = _acceptChannel;
        }

        if (server != null && server.isOpen() && _manager.isStarted())
        {
            SocketChannel channel = server.accept();
            channel.configureBlocking(false);
            Socket socket = channel.socket();
            configure(socket);
            _manager.accept(channel);
        }
	}

    protected void configure(Socket socket)
    {
        try
        {
            socket.setTcpNoDelay(true);
            if (_soLingerTime >= 0)
                socket.setSoLinger(true, _soLingerTime / 1000);
            else
                socket.setSoLinger(false, 0);
        }
        catch (Exception e)
        {
            LOG.ignore(e);
        }
    }
    
    public SipConnection getConnection(InetAddress address, int port) throws IOException
	{
		SipConnection connection = _connections.get(getKey(address, port));
		
		if (connection == null || !connection.isOpen())
		{
			// Could not use the synchronized method _connections.putIfAbsent as a connection mean a
			// new Socket, so use synchronized manually
			synchronized (this)
			{
				connection = _connections.get(getKey(address, port));
				if (connection == null || !connection.isOpen())
					connection = newConnection(address, port);
			}
		}
		return connection;
	}
    
    protected void removeConnection(SipConnection connection)
    {
    	if (connection.getConnector() == this)
    	{
	    	String key = getKey(connection.getRemoteAddress(), connection.getRemotePort());
	    	SipConnection c = _connections.remove(key);
	    	if (c == null) // Happens in TLS as there are 2 connection objects
	    		LOG.debug("Could not remove unknown connection {}", connection);
	    	else if (c != connection)
	    		LOG.warn("Try to remove connection {} but remove {}", connection, c);
	    	else
	    		LOG.debug("Removed connection {} on {}", connection, this);
    	}
    }
	
    protected SipConnection newConnection(InetAddress address, int port) throws IOException
    {
    	SocketChannel channel = SocketChannel.open(); 
    	channel.connect(new InetSocketAddress(address, port));
    	channel.configureBlocking(false);
    	configure(channel.socket());
    	synchronized (channel)
		{
    		_manager.accept(channel);
    		try
			{
				channel.wait(getIdleTimeout());
			}
			catch (InterruptedException e) {}
		}
    	return _connections.get(getKey(address, port));
    }
    
    protected Connection newConnection(EndPoint endpoint)
    {
    	SelectSipConnection connection = new SelectSipConnection(this, endpoint);
    	return connection;
    }
    
	protected String getKey(InetAddress address, int port)
	{
		return address.getHostAddress() + ":" + port;
	}
	    
    protected String getKey(Connection connection)
	{
    	InetSocketAddress addr = connection.getEndPoint().getRemoteAddress();
		return getKey(addr.getAddress(), addr.getPort());
	}
    
	@Override
	public InetAddress getAddress()
	{
		return _address;
	}
    
    private final class ConnectorSelectorManager extends SelectorManager
    {
        private ConnectorSelectorManager(Executor executor, Scheduler scheduler, int selectSets)
        {
            super(executor, scheduler, selectSets);
        }

        @Override
		protected SelectChannelEndPoint newEndPoint(SocketChannel channel,
				ManagedSelector selectSet, SelectionKey selectionKey)
				throws IOException
		{
			return new SelectChannelEndPoint(channel, selectSet, selectionKey,
					getScheduler(), getIdleTimeout());
		}

        @Override
		public Connection newConnection(SocketChannel channel,
				EndPoint endpoint, Object attachment)
		{
        	Connection connection = SelectChannelConnector.this.newConnection(endpoint);
        	_connections.put(getKey(connection), (SipConnection) connection);
        	synchronized (channel)
			{
				channel.notify();
			}
        	return connection;
		}
        
        @Override
        protected void doStop() throws Exception
        {
        	for (SipConnection connection : _connections.values())
        	{
        		connection.toString();
        		if (connection instanceof AbstractConnection)
        			((AbstractConnection) connection).close();
        	}
        	_connections.clear();
            super.doStop();
        }
    }
    
	public static void main(String[] args) throws Exception 
	{
		String host = null;
		try
		{
			host = InetAddress.getLocalHost().getHostAddress();
		}
		catch (Exception e)
		{
			LOG.ignore(e);
			host = "127.0.0.1";
		}
		
		SipServer sipServer = new SipServer();
		
		SelectChannelConnector connector = new SelectChannelConnector(sipServer);
		connector.setHost(host);
		connector.setPort(5060);
				
		sipServer.addConnector(connector);
		
		SipAppContext context = new SipAppContext();
		context.getServletHandler().addServlet(DefaultServlet.class.getName());
		
		sipServer.setHandler(context);
		
		sipServer.start();
	}
}
