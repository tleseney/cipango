// ========================================================================
// Copyright 2008-2009 NEXCOM Systems
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

package org.cipango.server.bio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.cipango.server.AbstractSipConnector;
import org.cipango.server.SipConnection;
import org.cipango.server.SipConnector;
import org.cipango.server.SipMessage;
import org.cipango.server.SipResponse;
import org.cipango.server.SipServer;
import org.cipango.server.Transport;
import org.cipango.server.nio.TcpConnector.MessageBuilder;
import org.cipango.server.servlet.DefaultServlet;
import org.cipango.server.sipapp.SipAppContext;
import org.cipango.server.transaction.Transaction;
import org.cipango.sip.SipGenerator;
import org.cipango.sip.SipParser;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@Deprecated
public class TcpConnector extends AbstractSipConnector //implements Buffers
{
	private static final Logger LOG = Log.getLogger(TcpConnector.class);

	
	public static final int MINIMAL_BUFFER_LENGTH = 2048;
	public static final int MAX_TCP_MESSAGE = 1024 * 400;
	
	public static final int DEFAULT_SO_TIMEOUT = 2 * Transaction.__T1 * 64;
	    
    private ServerSocket _serverSocket;
    private InetAddress _addr;
    private Map<String, TcpConnection> _connections;
    private int _connectionTimeout = DEFAULT_SO_TIMEOUT;
    private int _backlogSize = 50;
    
	private ByteBuffer[] _inBuffers;
	private ByteBufferPool _outBuffers;

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
    
	protected void doStart() throws Exception 
	{
		_connections = new HashMap<String, TcpConnection>();
		
		_outBuffers = new ArrayByteBufferPool();
		_inBuffers = new ByteBuffer[getAcceptors()];
		for (int i = _inBuffers.length; i-->0;)
			_inBuffers[i] = BufferUtil.allocateDirect(MINIMAL_BUFFER_LENGTH);
		
		super.doStart();
	}
	
	protected void doStop() throws Exception
	{
		super.doStop();
		
		Object[]  connections = _connections.values().toArray();
		for (Object o : connections)
		{
			TcpConnection connection =  (TcpConnection) o;
			try
			{
				connection.close();
			} 
			catch (Exception e) 
			{
				LOG.ignore(e);
			}
		}
	}


	@Override
	public Transport getTransport()
	{
		return Transport.TCP;
	}

	
	public InetAddress getAddr()
	{
		return _addr;
	}
	
	public void open() throws IOException
	{
		_serverSocket = newServerSocket();
		_addr = _serverSocket.getInetAddress();
	}
	
	public int getLocalPort()
	{
		if (_serverSocket==null || _serverSocket.isClosed())
            return -1;
        return _serverSocket.getLocalPort();
	}
	
	public Object getConnection()
	{
		return _serverSocket;
	}
	
	public ServerSocket newServerSocket() throws IOException
	{
		if (getHost() == null) 
			return new ServerSocket(getPort(), _backlogSize);
		else
			return new ServerSocket(
					getPort(), 
					_backlogSize, 
					InetAddress.getByName(getHost()));
	}
	
	public void close() throws IOException 
	{
		if (_serverSocket != null)
			_serverSocket.close();
		_serverSocket = null;
	}
	
	public void accept(int acceptorId) throws IOException
	{
		Socket socket = _serverSocket.accept();
		TcpConnection connection = newConnection(socket);
		addConnection(connection);
		connection.dispatch();
	}
	
	protected TcpConnection newConnection(Socket socket) throws IOException
	{
		return new TcpConnection(socket);
	}
	
	protected void addConnection(TcpConnection connection)
	{
		synchronized (_connections)
		{
			_connections.put(key(connection.getRemoteAddress(), connection.getRemotePort()), connection);
		}
	}

	protected ServerSocket getServerSocket()
	{
		return _serverSocket;
	}

	public SipConnection getConnection(InetAddress addr, int port) throws IOException 
	{
		synchronized (_connections) // TODO check blocked
		{
			TcpConnection cnx = _connections.get(key(addr, port));
			if (cnx == null) 
			{
				cnx = newConnection(addr, port);
				addConnection(cnx);
				cnx.dispatch();
			}
			return cnx;
		}
	}
	
	
	protected TcpConnection newConnection(InetAddress addr, int port) throws IOException
	{
		return new TcpConnection(new Socket(addr, port));
	}
	
	protected Map<String, TcpConnection> getConnections()
	{
		return _connections;
	}
	
	public void connectionOpened(TcpConnection connection)
	{
		
	}
	
	public void connectionClosed(TcpConnection connection) 
	{
		synchronized (_connections) 
		{
			_connections.remove(key(connection.getRemoteAddress(), connection.getRemotePort()));
		}
	}
	
	private String key(InetAddress addr, int port) 
	{
		return addr.getHostAddress() + ":" + port;
	}
	
	public int getBacklogSize()
	{
		return _backlogSize;
	}

	public void setBacklogSize(int backlogSize)
	{
		_backlogSize = backlogSize;
	}
	
	public int getConnectionTimeout()
	{
		return _connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout)
	{
		_connectionTimeout = connectionTimeout;
	}
	
	public class TcpConnection implements SipConnection, Runnable
	{
		private Socket _socket;
		
		public TcpConnection(Socket socket) throws IOException 
		{
			_socket = socket;
			socket.setTcpNoDelay(true);
			socket.setSoTimeout(_connectionTimeout);
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
		
		public void close()
		{
			if (_socket != null)
				try
				{
					_socket.close();
				}
				catch (IOException e)
				{
					LOG.ignore(e);
				}
			connectionClosed(this);
		}
		
		public InetAddress getLocalAddress()
		{
			return _socket.getLocalAddress();
		}
		
		public InetAddress getRemoteAddress()
		{
			return _socket.getInetAddress();
		}
		
		public int getLocalPort()
		{
			return _socket.getLocalPort();
		}
		
		public int getRemotePort()
		{
			return _socket.getPort();
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
				write(buffer);
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
			synchronized (this)
			{
				_socket.getOutputStream().write(buffer.array(), buffer.position(), buffer.limit() - buffer.position());
				_socket.getOutputStream().flush();
				// FIXME modify buffer
			}
		}
		
		public void run()
		{
			MessageBuilder builder = new MessageBuilder(getServer(), this);
			SipParser parser = new SipParser(builder);
			
			int read = 0;
			ByteBuffer buffer = ByteBuffer.allocate(MINIMAL_BUFFER_LENGTH);
			try
			{
				
				while (read >= 0)
				{
					buffer.clear();
					read = _socket.getInputStream().read(buffer.array());
					if (read <= 0)
						break;
					
					buffer.limit(read);
					
					while (buffer.hasRemaining())
					{
						if (parser.parseNext(buffer))
							parser.reset();
					}
				}
			}
			catch (IOException e)
			{
				LOG.warn(e);
			}
			finally
			{
				close();
			}
		}
		
		public SipConnector getConnector() 
		{
			return TcpConnector.this;
		}
		
		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("TCP Connection ");
			sb.append(getLocalAddress()).append(":").append(getLocalPort());
			sb.append(" - ");
			sb.append(getRemoteAddress()).append(":").append(getRemotePort());
			return sb.toString();
		}

		@Override
		public Transport getTransport()
		{
			return Transport.TCP;
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
		context.getSipServletHandler().addSipServlet(DefaultServlet.class.getName());
		
		sipServer.setHandler(context);
		
		sipServer.start();
	}
}
