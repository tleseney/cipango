package org.cipango.server.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.cipango.server.AbstractSipConnector;
import org.cipango.server.MessageTooLongException;
import org.cipango.server.SipConnection;
import org.cipango.server.SipConnector;
import org.cipango.server.SipMessage;
import org.cipango.server.SipMessageGenerator;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.SipServer;
import org.cipango.server.Transport;
import org.cipango.server.servlet.DefaultServlet;
import org.cipango.server.sipapp.SipAppContext;
import org.cipango.server.transaction.Transaction;
import org.cipango.sip.SipHeader;
import org.cipango.sip.SipParser;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@Deprecated
public class TcpConnector extends AbstractSipConnector
{
	private static final Logger LOG = Log.getLogger(TcpConnector.class);

	public static final int MINIMAL_BUFFER_LENGTH = 2048;
	public static final int DEFAULT_SO_TIMEOUT = 2 * Transaction.__T1 * 64;
	
	private ServerSocketChannel _channel;
	private ByteBufferPool _outBuffers;
    private Map<String, TcpConnection> _connections;
    private int _connectionTimeout = DEFAULT_SO_TIMEOUT;

    public TcpConnector(
    		@Name("sipServer") SipServer server)
    {
        this(server, null, Math.max(1,(Runtime.getRuntime().availableProcessors())/2));
    }

    public TcpConnector(
            @Name("sipServer") SipServer server,
            @Name("acceptors") int acceptors)
    {
        this(server, null, acceptors);
    }

    public TcpConnector(
            @Name("sipServer") SipServer server,
            @Name("executor") Executor executor,
            @Name("acceptors") int acceptors)
    {
    	super(server, executor, acceptors);
    }

	@Override
	protected void doStart() throws Exception
	{
		_connections = new HashMap<String, TcpConnection>();
		_outBuffers = new ArrayByteBufferPool(MINIMAL_BUFFER_LENGTH, 4096, 65536);

		super.doStart();
	}

	@Override
	public Transport getTransport()
	{
		return Transport.TCP;
	}

	@Override
	public void open() throws IOException
	{
		_channel = ServerSocketChannel.open();
		_channel.configureBlocking(true);
		_channel.socket().bind(new InetSocketAddress(InetAddress.getByName(getHost()), getPort()));
	}

	@Override
	public void close() throws IOException
	{
		_channel.close();
	}

	@Override
	protected void accept(int id) throws IOException
	{
		TcpConnection connection = new TcpConnection(_channel.accept(), id);
		addConnection(connection);
		connection.dispatch();
	}
	
	protected ServerSocketChannel getChannel()
	{
		return _channel;
	}
	
	public int getConnectionTimeout()
	{
		return _connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout)
	{
		_connectionTimeout = connectionTimeout;
	}
	

	@Override
	public SipConnection getConnection(InetAddress address, int port) throws IOException
	{
		synchronized (_connections)
		{
			TcpConnection cnx = _connections.get(address + ":" + port);
			if (cnx == null) 
			{
				cnx = null; // TODO
				addConnection(cnx);
				cnx.dispatch();
			}
			return cnx;
		}
	}
	
	@Override
	public InetAddress getAddress()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	protected void addConnection(TcpConnection connection)
	{
		LOG.debug("Opened connection {}", connection);
		synchronized (_connections)
		{
			_connections.put(connection.toString(), connection);
		}
	}
	
	protected void removeConnection(TcpConnection connection) 
	{
		LOG.debug("Closing connection {}", connection);
		synchronized (_connections) 
		{
			_connections.remove(connection.toString());
		}
	}
	
	class TcpConnection implements SipConnection, Runnable
	{
		private ByteBuffer _buffer;
		private SocketChannel _channel;
		private InetSocketAddress _localAddr;
		private InetSocketAddress _remoteAddr;
	    private final SipMessageGenerator _sipGenerator;
		
		public TcpConnection(SocketChannel channel, int id) throws IOException
		{
            _buffer = BufferUtil.allocateDirect(MINIMAL_BUFFER_LENGTH);

			_channel = channel;
			_localAddr = (InetSocketAddress) _channel.getLocalAddress();
			_remoteAddr = (InetSocketAddress) _channel.getRemoteAddress();
			
			_channel.socket().setTcpNoDelay(true);
			_channel.socket().setSoTimeout(_connectionTimeout);
			_sipGenerator = new SipMessageGenerator();
		}
		
		public void dispatch() throws IOException
        {
			try
			{
				getExecutor().execute(this);
			}
			catch (RejectedExecutionException e)
            {
                LOG.warn("dispatch failed for {}", this);
                close();
            }
        }
		
		@Override
		public SipConnector getConnector()
		{
			return TcpConnector.this;
		}

		@Override
		public Transport getTransport()
		{
			return Transport.TCP;
		}

		@Override
		public InetAddress getLocalAddress()
		{
			return _localAddr.getAddress();
		}

		@Override
		public int getLocalPort()
		{
			return _localAddr.getPort();
		}

		@Override
		public InetAddress getRemoteAddress()
		{
			return _remoteAddr.getAddress();
		}

		@Override
		public int getRemotePort()
		{
			return _remoteAddr.getPort();
		}

		@Override
		public void send(SipMessage message) throws MessageTooLongException
		{
			ByteBuffer buffer = _outBuffers.acquire(MINIMAL_BUFFER_LENGTH, false);
			
			buffer.clear();
			
			_sipGenerator.generateMessage(buffer, message);

			buffer.flip();
			try
			{
				_channel.write(buffer);
			}
			catch (Exception e)
			{
				LOG.warn(e);
			}
			
			_outBuffers.release(buffer);
		}

		@Override
		public void write(ByteBuffer buffer) throws IOException
		{
			_channel.write(buffer);	
		}

		@Override
		public void run()
		{
			MessageBuilder builder = new MessageBuilder(getServer(), this);
			SipParser parser = new SipParser(builder);
			int read = 0;
			
			try
			{
				while (read >= 0)
				{
					_buffer.clear();
					read = _channel.read(_buffer);
					if (read <= 0)
						break;

					_buffer.flip();

					while (_buffer.hasRemaining())
					{
						if (parser.parseNext(_buffer))
							parser.reset();
					}
				}
			}
			catch (IOException e)
			{
				LOG.ignore(e);
			}
			finally
			{
				removeConnection(this);
				try
				{
					_channel.close();
				}
				catch (IOException e)
				{
					LOG.ignore(e);
				}
			}
		}
		
		@Override
		public boolean isOpen()
		{
			return _channel.isOpen();
		}

		public String toString()
		{
			return getRemoteAddress() + ":" + getRemotePort();
		}

	}
	
	public static class MessageBuilder extends AbstractSipConnector.MessageBuilder
	{
		public MessageBuilder(SipServer server, SipConnection connection)
		{
			super(server, connection);
		}

		@Override
		public boolean headerComplete()
		{
			if (!_message.getFields().containsKey(SipHeader.CONTENT_LENGTH.toString()))
			{
				// RFC3261 18.3.
				if (_message.isRequest())
				{
					SipRequest request = (SipRequest) _message;
					SipResponse response = (SipResponse) request.createResponse(
							400, "Content-Length is mandatory");
					try
					{
						_connection.send(response);
					}
					catch (MessageTooLongException e)
					{
						LOG.warn(e);
					}
				}
				reset();
				return true;
			}
			return false;
		}
		
		@Override
		public void badMessage(int status, String reason)
		{
			// TODO: close the connection.
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
		TcpConnector connector = new TcpConnector(sipServer);
		connector.setHost(host);
		connector.setPort(5060);
		
		sipServer.addConnector(connector);
		
		SipAppContext context = new SipAppContext();
		context.getServletHandler().addServlet(DefaultServlet.class.getName());
		
		sipServer.setHandler(context);
		
		sipServer.start();
	}


}
