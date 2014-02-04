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
import org.cipango.dns.Resolver;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class UdpConnector extends AbstractConnector
{
	public static final int MAX_PACKET_SIZE = 512;
	
	private static final Logger LOG = Log.getLogger(UdpConnector.class);
	private DatagramSocket _socket;
	private Acceptor _acceptor;
	private int _timeout = Resolver.DEFAULT_TIMEOUT * 10;
	

	@Override
	protected void doStop() throws Exception
	{
		super.doStop();
		if (_socket != null)
			_socket.close();
		_socket = null;
		_acceptor = null;
	}
	
	public DnsConnection newConnection(InetAddress host, int port)
	{
		return new Connection(host, port);
	}
	
	public synchronized DatagramSocket getDatagramSocket() throws SocketException
	{
		if (_socket == null || _socket.isClosed())
		{
			_socket = new DatagramSocket(getPort(), getHostAddr());
			_socket.setSoTimeout(_timeout); // FIXME 

			LOG.debug("Create the new datagram socket {} for DNS connector", _socket.getLocalSocketAddress());
			_acceptor = new Acceptor();
			new Thread(_acceptor, "DNS acceptor").start();
			
		}
		return _socket;
	}
	
	public int getTimeout()
	{
		return _timeout;
	}

	public void setTimeout(int timeout)
	{
		_timeout = timeout;
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
					DatagramPacket packet = new DatagramPacket(new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE);
					_datagramSocket.receive(packet);
					DnsMessage answer = new DnsMessage();
					answer.decode(ByteBuffer.wrap(packet.getData()));
					MsgContainer msgContainer;
					
					synchronized (_queries)
					{
						msgContainer = _queries.get(answer.getHeaderSection().getId());
					}
					
					if (msgContainer != null)
					{
						synchronized (msgContainer.getQuery())
						{
							msgContainer.setAnswer(answer);
							msgContainer.getQuery().notify();
						}
					}
					else
						LOG.warn("Drop DNS Answser {}, as can not found a query with same ID", answer);
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
			ByteBuffer buffer = ByteBuffer.allocate(MAX_PACKET_SIZE);
			message.encode(buffer);
			DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.position(), _remoteAddr, _remotePort);
			
			synchronized (_queries)
			{
				_queries.put(message.getHeaderSection().getId(), new MsgContainer(message));
			}
			
			_socket = getDatagramSocket();
			_socket.send(packet);
		}
		
		public DnsMessage waitAnswer(DnsMessage request, int timeout)
		{
			synchronized (request)
			{
				try { request.wait(timeout); } catch (InterruptedException e) {}				
			}
			MsgContainer messages;
			synchronized (_queries)
			{

				messages = _queries.remove(request.getHeaderSection().getId());
			}
			if (messages == null)
				return null;
			return messages.getAnswer();
		}
		
		public DatagramSocket getSocket()
		{
			return _socket;
		}
		
	}
}
