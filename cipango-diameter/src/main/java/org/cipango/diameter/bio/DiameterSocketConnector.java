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
package org.cipango.diameter.bio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.cipango.diameter.AVP;
import org.cipango.diameter.AVPList;
import org.cipango.diameter.Dictionary;
import org.cipango.diameter.Factory;
import org.cipango.diameter.ResultCode;
import org.cipango.diameter.base.Common;
import org.cipango.diameter.io.Codecs;
import org.cipango.diameter.node.AbstractDiameterConnector;
import org.cipango.diameter.node.DiameterAnswer;
import org.cipango.diameter.node.DiameterConnection;
import org.cipango.diameter.node.DiameterMessage;
import org.cipango.diameter.node.Peer;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * TCP Diameter Connector using BIO
 */
public class DiameterSocketConnector extends AbstractDiameterConnector
{
	private static final Logger LOG = Log.getLogger(DiameterSocketConnector.class);
	
	public static final int DEFAULT_PORT = 3868;
	
	private ServerSocketChannel _serverChannel;
		
	@Override
	public void open() throws IOException
	{
		_serverChannel = ServerSocketChannel.open();
		_serverChannel.configureBlocking(true);
		_serverChannel.socket().bind(new InetSocketAddress(InetAddress.getByName(getHost()), getPort()));
	}
	
	@Override
	public void close() throws IOException
	{
		if (_serverChannel != null)
			_serverChannel.close();
		_serverChannel = null;
	}
	
	
	protected ServerSocket newServerSocket() throws IOException
	{
		ServerSocket ss = new ServerSocket();
		if (getHost() == null)
			ss.bind(new InetSocketAddress(getPort()));
		else
			ss.bind(new InetSocketAddress(getHost(), getPort()));
		return ss;
	}
	
	@Override
	public void accept(int acceptorID) throws IOException, InterruptedException
	{
		SocketChannel socket = _serverChannel.accept();
		Connection connection = new Connection(socket);
		new Thread(connection, "Connection-" + acceptorID).start();
	}
	
	@Override
	public DiameterConnection getConnection(Peer peer) throws IOException
	{
		int port = peer.getPort();
		if (port == 0)
			port = DEFAULT_PORT;
		
		SocketChannel socket;
		
		if (peer.getAddress() != null)
			socket = SocketChannel.open(new InetSocketAddress(peer.getAddress(), port));
		else 
			socket = SocketChannel.open(new InetSocketAddress(peer.getHost(), port));
		
		Connection connection = new Connection(socket);
		connection.setPeer(peer);
		
		new Thread(connection, "Connection-" + peer.getHost()).start();

		return connection;
	}
	
	@Override
	public int getLocalPort()
	{
		return getPort();
	}
	
	@Override
	public InetAddress getLocalAddress()
	{
		if (_serverChannel == null || !_serverChannel.isOpen())
			return null;
		try
		{
			return ((InetSocketAddress) _serverChannel.getLocalAddress()).getAddress();
		}
		catch (IOException e)
		{
			return null;
		}
	}

	@Override
	protected int getDefaultPort()
	{
		return DEFAULT_PORT;
	}
		
	public class Connection implements Runnable, DiameterConnection
	{
		private SocketChannel _channel;
		private Peer _peer;
		
		public Connection(SocketChannel channel) throws IOException
		{
			_channel = channel;
		}
		
		@Override
		public void setPeer(Peer peer)
		{
			_peer = peer;
		}
		
		@Override
		public Peer getPeer()
		{
			return _peer;
		}
		
		@Override
		public void stop()
		{
			try { close(); } catch (IOException e) { LOG.ignore(e); }
		}
		
		@Override
		public void write(DiameterMessage message) throws IOException
		{
			ByteBuffer buffer = getBuffer(getMessageBufferSize());
			buffer = Codecs.__message.encode(buffer, message);
			
			buffer.flip();
			_channel.write(buffer);
			
			returnBuffer(buffer);
			
			if (getNode().isStatsOn())
				_messagesSent.incrementAndGet();
			
			if (_listener != null)
				_listener.messageSent(message, this);
		}
		
		@Override
		public boolean isOpen()
		{
			return _channel.isOpen();
		}
		
		@Override
		public void run()
		{
			try
			{
				ByteBuffer fb = ByteBuffer.allocate(4);
				
				while (isStarted() && isOpen())
				{
					fb.clear();
					int read = _channel.read(fb);
					if (read == -1)
						throw new EofException();
					
					int length = 
						(fb.get(1) & 0xff) << 16
						| (fb.get(2) & 0xff) << 8
						| (fb.get(3) & 0xff);
					
					fb.flip();
										
					ByteBuffer b = ByteBuffer.allocate(length);

					int totalRead = 4;
					b.put(fb);
					
					while (totalRead < length)
					{
						read = _channel.read(b);
						if (read == -1)
							throw new EofException();
						totalRead += read;
					}

					b.flip();		

				
					DiameterMessage message = Codecs.__message.decode(b);
					message.setConnection(this);
					message.setNode(getNode());
					
					if (getNode().isStatsOn())
						_messagesReceived.incrementAndGet();
					
					// TODO move the following code at a better place. Need to be done before _listener.messageReceived(message, this);
					if (!message.isRequest())
					{
						int code;
						int vendorId = Common.IETF_VENDOR_ID;
						
						AVP<Integer> avp = message.getAVPs().get(Common.RESULT_CODE);
						if (avp != null)
						{
							code = avp.getValue();
						}
						else
						{
							AVPList expRc = message.get(Common.EXPERIMENTAL_RESULT);
							code = expRc.getValue(Common.EXPERIMENTAL_RESULT_CODE);
							vendorId = expRc.getValue(Common.VENDOR_ID);
						}
						
						ResultCode rc = Dictionary.getInstance().getResultCode(vendorId, code);
						if (rc == null)
							rc = Factory.newResultCode(vendorId, code, "Unknown");
						
						((DiameterAnswer) message).setResultCode(rc);
					}
					
					if (_listener != null)
						_listener.messageReceived(message, this);
					
					getNode().receive(message);
				}
			}
			catch (EofException e)
			{
				LOG.debug(e);
				try { close(); } catch (IOException e2) { LOG.ignore(e2); }
			}
			catch (IOException e)
			{
				LOG.debug(e); // TODO
				try { close(); } catch (IOException e2) { LOG.ignore(e2); }
			}
			catch (Throwable t)
			{
				LOG.warn("handle failed", t);
				try { close(); } catch (IOException e2) { LOG.ignore(e2); }
			}
			finally
			{
				if (_peer != null)
					_peer.peerDisc(this);
			}
		}

		@Override
		public void close() throws IOException
		{
			_channel.close();
		}

		@Override
		public InetSocketAddress getLocalAddress()
		{
			try
			{
				return (InetSocketAddress) _channel.getLocalAddress();
			}
			catch (IOException e)
			{
				LOG.ignore(e);
				return null;
			}
		}

		@Override
		public InetSocketAddress getRemoteAddress()
		{
			try
			{
				return (InetSocketAddress) _channel.getRemoteAddress();
			}
			catch (IOException e)
			{
				LOG.ignore(e);
				return null;
			}
		}
	}

}
