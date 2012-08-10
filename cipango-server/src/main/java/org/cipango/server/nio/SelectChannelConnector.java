package org.cipango.server.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.cipango.server.AbstractSipConnector;
import org.cipango.server.Transport;
import org.cipango.server.transaction.Transaction;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.SelectChannelEndPoint;
import org.eclipse.jetty.io.SelectorManager;
import org.eclipse.jetty.io.StandardByteBufferPool;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class SelectChannelConnector extends AbstractSipConnector
{
	private static final Logger LOG = Log.getLogger(SelectChannelConnector.class);

	public static final int DEFAULT_SO_TIMEOUT = 2 * Transaction.__T1 * 64;

    private ScheduledExecutorService _scheduler;
    private final SelectorManager _manager;
	private ServerSocketChannel _acceptChannel;
    private int _localPort = -1;
    
    private volatile ByteBufferPool _byteBufferPool;
    private volatile int _acceptQueueSize = 128;
    private volatile long _idleTimeout = DEFAULT_SO_TIMEOUT;
    private volatile boolean _reuseAddress = true;
    private volatile int _soLingerTime = -1;

    public SelectChannelConnector()
    {
        this(Math.max(1,(Runtime.getRuntime().availableProcessors())/4),
             Math.max(1,(Runtime.getRuntime().availableProcessors())/4));
    }

    public SelectChannelConnector(int acceptors, int selectors)
    {
        super(acceptors);
		_byteBufferPool = new StandardByteBufferPool();
        _manager = new ConnectorSelectorManager(selectors);
        addBean(_manager, true);
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
    
    public ScheduledExecutorService getScheduler()
    {
        return _scheduler;
    }

    
	@Override
	protected void doStart() throws Exception
	{
        if (_scheduler == null)
        {
            _scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory()
            {
                @Override
                public Thread newThread(Runnable r)
                {
                    return new Thread(r, "Timer-" + getPort());
                }
            });
        }
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception
    {
    	_scheduler.shutdownNow();
        _scheduler = null;
        super.doStop();
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
                
                InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(getHost()), getPort());
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
    
    private final class ConnectorSelectorManager extends SelectorManager
    {
        private ConnectorSelectorManager(int selectSets)
        {
            super(selectSets);
        }

        @Override
        protected void execute(Runnable task)
        {
            getThreadPool().execute(task);
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
        	return new SelectSipConnection(SelectChannelConnector.this, endpoint);
		}
    }
}
