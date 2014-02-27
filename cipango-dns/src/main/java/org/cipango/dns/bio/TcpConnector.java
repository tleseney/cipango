// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
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
package org.cipango.dns.bio;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.cipango.dns.AbstractConnector;
import org.cipango.dns.DnsConnection;
import org.cipango.dns.DnsMessage;
import org.cipango.dns.util.BufferUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class TcpConnector extends AbstractConnector
{	
	public static final int MIN_BOUNDED_PORT = 1024;
	public static final int MAX_BOUNDED_PORT = 65535;
	public static final int MAX_PACKET_SIZE = 65536;
	private static final Logger LOG = Log.getLogger(TcpConnector.class);
	private Map<String, Connection> _connections = new HashMap<>(3);
	private int _minPort;
	private int _maxPort;
	private int _currentPort;

	@Override
	protected void doStart() throws Exception
	{
		super.doStart();
		if(_maxPort == 0 && _minPort != 0)
            _maxPort = MAX_BOUNDED_PORT;
        if(_minPort == 0 && _maxPort != 0)
            _minPort = MIN_BOUNDED_PORT;
        
        if(_minPort > _maxPort && _minPort != 0)
            throw new IllegalArgumentException("minPort > maxPort");
        
        _currentPort = _minPort - 1;
	}
	
	public DnsConnection getConnection(InetAddress host, int port) throws IOException
	{
		synchronized (_connections) // TODO check blocked
		{
			Connection cnx = _connections.get(key(host, port));
			if (cnx == null || !cnx.isOpen()) 
			{
				cnx = new Connection(host, port);
				_connections.put(key(host, port), cnx);
				new Thread(cnx, "DNS TCP acceptor").start(); //TODO use thread pool
			}
			return cnx;
		}
	}
	
	protected DnsConnection newConnection(InetAddress host, int port) throws IOException {
		return new Connection(host, port);
	}
	
	private String key(InetAddress addr, int port) 
	{
		return addr.getHostAddress() + ":" + port;
	}
	
	public int getMinPort()
	{
		return _minPort;
	}

	public void setMinPort(int minPort)
	{
		_minPort = minPort;
		_currentPort = _minPort - 1;
	}

	public int getMaxPort()
	{
		return _maxPort;
	}

	public void setMaxPort(int maxPort)
	{
		_maxPort = maxPort;
	}

	public int getCurrentPort()
	{
		return _currentPort;
	}
	
	@Override
	public boolean isTcp()
	{
		return true;
	}
				
	public class Connection implements DnsConnection, Runnable
	{
		private Socket _socket;
		
		public Connection(InetAddress remoteAddr, int remotePort) throws IOException
		{
			_socket = newSocket();
			_socket.setSoTimeout(getTimeout()); // FIXME 
			_socket.connect(new InetSocketAddress(remoteAddr, remotePort));
			
			LOG.debug("Create the new socket {} for DNS TCP connector", _socket);
		}
		
		protected Socket newSocket() throws BindException 
		{
			if (getHostAddr() != null || getMinPort() != 0)
			{
				Socket socket = null;
				int range = _maxPort - _minPort;
				int testedPort = _currentPort;
		    	for (int i = 0; i <= range; i++)
		    	{
		    		try
		    		{
		    			if (testedPort == _maxPort)
		    				testedPort = _minPort;
		    			else
		    				testedPort++;
		    			socket = new Socket();
		    		    socket.bind(new InetSocketAddress(getHostAddr(), testedPort)); 
		    		    _currentPort = testedPort;
		    		    break;
		    		}
		    		catch (IOException e)
		    		{
		    		}
		    	}
		    	if (socket == null || !socket.isBound())
		    		throw new BindException("Could not found available local port between " + _minPort + " and " + _maxPort);
		    	return socket;
			}
			else
				return new Socket();
		}
		
		public void send(DnsMessage message) throws IOException
		{
			ByteBuffer buffer = ByteBuffer.allocate(MAX_PACKET_SIZE);
			buffer.position(2);
			message.encode(buffer);
			int size = buffer.position() + 2;
			buffer.position(0);
			BufferUtil.put16(buffer, size);
						
			addQuery(message);
			
			_socket.getOutputStream().write(buffer.array(), 0, size + 2);
			_socket.getOutputStream().flush();
		}
		
		public DnsMessage waitAnswer(DnsMessage request, int timeout)
		{
			return TcpConnector.this.waitAnswer(request, timeout);
		}
		
		public Socket getSocket()
		{
			return _socket;
		}
		
		public void run()
		{
			while (_socket != null && !_socket.isClosed())
			{
				try
				{
					int size1 = _socket.getInputStream().read();
					int size2 = _socket.getInputStream().read();
					int size = (size1 & 0xff) << 8 | (size2 & 0xff);
					byte[] buffer = new byte[size];
					int read = 0;
					while (read < size) {
						int read2 = _socket.getInputStream().read(buffer, read, size - read);
						if (read2 != -1)
							read += read2;
					}
					
					DnsMessage answer = new DnsMessage();
					answer.decode(ByteBuffer.wrap(buffer));
					updateQueryOnAnswer(answer);
				}
				catch (IOException e)
				{
					close();
				}
			}
			close();
			LOG.debug("DNS acceptor done");
		}
		
		public boolean isOpen() {
			return _socket != null && !_socket.isClosed();
		}
		
		private void close()
		{
			try {
    			if (_socket != null && !_socket.isClosed())
    				_socket.close();
    			_socket = null;
			} catch (Exception e) {
				LOG.warn("Failed to close socket: " + _socket, e);
			}
		}
	}


}
