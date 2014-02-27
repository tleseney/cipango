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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

import org.cipango.dns.AbstractConnector;
import org.cipango.dns.DnsConnection;
import org.cipango.dns.DnsMessage;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class UdpConnector extends AbstractConnector
{
	public static final int DEFAULT_MAX_PACKET_SIZE = 512;
	
	private static final Logger LOG = Log.getLogger(UdpConnector.class);
	private DatagramSocket _socket;
	private Acceptor _acceptor;
	private int _maxPacketSize = DEFAULT_MAX_PACKET_SIZE;
	private int _port;

	@ManagedAttribute(value="Port", readonly=true)
	public int getPort()
	{
		return _port;
	}

	public void setPort(int port)
	{
		if (port < 0 || port > 65536)
			throw new IllegalArgumentException("Invalid port: " + port);
		_port = port;
	}
	
	@Override
	protected void doStop() throws Exception
	{
		super.doStop();
		if (_socket != null)
			_socket.close();
		_socket = null;
		_acceptor = null;
	}
	
	public DnsConnection getConnection(InetAddress host, int port)
	{
		return new Connection(host, port);
	}
	
	public synchronized DatagramSocket getDatagramSocket() throws SocketException
	{
		if (_socket == null || _socket.isClosed())
		{
			_socket = new DatagramSocket(getPort(), getHostAddr());
			_socket.setSoTimeout(getTimeout()); // FIXME 

			LOG.debug("Create the new datagram socket {} for DNS connector", _socket.getLocalSocketAddress());
			_acceptor = new Acceptor();
			new Thread(_acceptor, "DNS acceptor").start();
			
		}
		return _socket;
	}
		
	public int getMaxPacketSize()
	{
		return _maxPacketSize;
	}

	public void setMaxPacketSize(int maxPacketSize)
	{
		if (_maxPacketSize < DEFAULT_MAX_PACKET_SIZE)
			throw new IllegalArgumentException("Invalid max packet size: " + maxPacketSize);
		_maxPacketSize = maxPacketSize;
	}
	
	@Override
	public boolean isTcp()
	{
		return false;
	}
	
	
	public class Acceptor implements Runnable
	{
		private DatagramSocket _datagramSocket;
		
		public Acceptor()
		{
			_datagramSocket = _socket;
		}
		
		public void run()
		{
			while (_datagramSocket != null && _datagramSocket == _socket && !_datagramSocket.isClosed())
			{
				try
				{
					DatagramPacket packet = new DatagramPacket(new byte[_maxPacketSize], _maxPacketSize);
					_datagramSocket.receive(packet);
					DnsMessage answer = new DnsMessage();
					answer.decode(ByteBuffer.wrap(packet.getData()));

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
		
		private void close()
		{
			if (_datagramSocket != null && !_datagramSocket.isClosed())
				_datagramSocket.close();
			_datagramSocket = null;
		}
		
	}

	
	public class Connection implements DnsConnection
	{
		private InetAddress _remoteAddr;
		private int _remotePort;
		private DatagramSocket _socket;
		
		public Connection(InetAddress remoteAddr, int remotePort)
		{
			_remoteAddr = remoteAddr;
			_remotePort = remotePort;
		}
				
		public void send(DnsMessage message) throws IOException
		{
			int maxSize = message.getMaxUdpSize();
			if (maxSize > _maxPacketSize)
				setMaxPacketSize(maxSize);
			
			ByteBuffer buffer = ByteBuffer.allocate(maxSize);
			message.encode(buffer);
			DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.position(), _remoteAddr, _remotePort);
			
			addQuery(message);
			
			_socket = getDatagramSocket();
			_socket.send(packet);
		}
		
		public DnsMessage waitAnswer(DnsMessage request, int timeout)
		{
			return UdpConnector.this.waitAnswer(request, timeout);
		}
		
		public DatagramSocket getSocket()
		{
			return _socket;
		}
		
	}
}
