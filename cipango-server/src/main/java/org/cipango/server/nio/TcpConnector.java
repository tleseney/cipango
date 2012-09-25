package org.cipango.server.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.cipango.server.AbstractSipConnector;
import org.cipango.server.SipConnection;
import org.cipango.server.SipConnector;
import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.SipServer;
import org.cipango.server.Transport;
import org.cipango.server.servlet.DefaultServlet;
import org.cipango.server.sipapp.SipAppContext;
import org.cipango.server.transaction.Transaction;
import org.cipango.sip.AddressImpl;
import org.cipango.sip.SipGenerator;
import org.cipango.sip.SipHeader;
import org.cipango.sip.SipMethod;
import org.cipango.sip.SipParser;
import org.cipango.sip.SipVersion;
import org.cipango.sip.Via;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class TcpConnector extends AbstractSipConnector
{
	private static final Logger LOG = Log.getLogger(TcpConnector.class);

	public static final int MINIMAL_BUFFER_LENGTH = 2048;
	public static final int DEFAULT_SO_TIMEOUT = 2 * Transaction.__T1 * 64;
	
	private ServerSocketChannel _channel;
	private ByteBuffer[] _inBuffers;
	private ByteBufferPool _outBuffers;
    private Map<String, TcpConnection> _connections;
    private int _connectionTimeout = DEFAULT_SO_TIMEOUT;
    
	@Override
	protected void doStart() throws Exception
	{
		_connections = new HashMap<String, TcpConnection>();
		_outBuffers = new ArrayByteBufferPool();
		_inBuffers = new ByteBuffer[getAcceptors()];
		for (int i = _inBuffers.length; i-->0;)
			_inBuffers[i] = BufferUtil.allocateDirect(MINIMAL_BUFFER_LENGTH);
		
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
		
		public TcpConnection(SocketChannel channel, int id) throws IOException
		{
			_buffer = _inBuffers[id];
			_channel = channel;
			_localAddr = (InetSocketAddress) _channel.getLocalAddress();
			_remoteAddr = (InetSocketAddress) _channel.getRemoteAddress();
			
			_channel.socket().setTcpNoDelay(true);
			_channel.socket().setSoTimeout(_connectionTimeout);
		}
		
		public void dispatch() throws IOException
        {
			try
			{
				getThreadPool().execute(this);
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
		public void send(SipMessage message)
		{
			SipResponse response = (SipResponse) message;
			ByteBuffer buffer = _outBuffers.acquire(MINIMAL_BUFFER_LENGTH, false);
			
			buffer.clear();
			
			new SipGenerator().generateResponse(buffer, response.getStatus(),
					response.getReasonPhrase(), response.getFields());

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

		public String toString()
		{
			return getRemoteAddress() + ":" + getRemotePort();
		}
	}
	
	public static class MessageBuilder implements SipParser.SipMessageHandler
	{
		protected SipConnection _connection;
		protected SipMessage _message;
		private SipServer _server;
		
		public MessageBuilder(SipServer server, SipConnection connection)
		{
			_server = server;
			_connection = connection;
		}
		
		public boolean startRequest(String method, String uri, SipVersion version)
		{
			SipRequest request = new SipRequest();
			
			SipMethod m = SipMethod.CACHE.get(method);
			request.setMethod(m, method);
			
			_message = request;
			return false;
		}

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
			_message.getFields().add(name, o, false);
			return false;
		}

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
					_connection.send(response);
				}
				reset();
				return true;
			}
			return false;
		}

		public boolean messageComplete(ByteBuffer content) 
		{
			_message.setConnection(_connection);
			_message.setTimeStamp(System.currentTimeMillis());
						
			_server.process(_message);
			
			reset();
			return true;
		}
		
		public void badMessage(int status, String reason)
		{
			reset();
		}

		protected void reset()
		{
			_message = null;
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
		
		TcpConnector connector = new TcpConnector();
		connector.setThreadPool(new QueuedThreadPool());
		connector.setHost(host);
		connector.setPort(5060);
		SipServer sipServer = new SipServer();
		
		sipServer.addConnector(connector);
		
		SipAppContext context = new SipAppContext();
		context.getSipServletHandler().addSipServlet(DefaultServlet.class.getName());
		
		sipServer.setHandler(context);
		
		sipServer.start();
	}
}
