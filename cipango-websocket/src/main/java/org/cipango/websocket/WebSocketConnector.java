// ========================================================================
// Copyright 2011-2013 NEXCOM Systems
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
package org.cipango.websocket;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.sip.SipURI;

import org.cipango.server.AbstractSipConnector.MessageBuilder;
import org.cipango.server.MessageTooLongException;
import org.cipango.server.SipConnection;
import org.cipango.server.SipConnector;
import org.cipango.server.SipMessage;
import org.cipango.server.SipServer;
import org.cipango.server.Transport;
import org.cipango.sip.SipParser;
import org.cipango.sip.SipURIImpl;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

public class WebSocketConnector extends ContainerLifeCycle implements SipConnector
{
	private static final Logger LOG = Log.getLogger(WebSocketConnection.class);
	
	private InetAddress _localAddr;
	private Map<String, WebSocketConnection> _connections;
	
	private int _port;
	private String _host;
	private SipURI _sipUri;
    private final SipServer _server;
	
	public WebSocketConnector(SipServer server)
	{
		_server = server;
		
		addBean(_server,false);
		
		_connections = new HashMap<String, WebSocketConnection>();
		WebSocketConnection.__connector = this; //FIXME
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
		
		_sipUri = new SipURIImpl(_host, _port);
    	_sipUri.setTransportParam(getTransport().getName().toLowerCase());
    	
        open();        
        LOG.info("Started {}", this);
    }
	
	public void open() throws IOException
	{
	}

	public void close() throws IOException
	{
		// FIXME close connections ???
	}
	
	public String getHost()
	{
		return _host;
	}

	public int getPort()
	{
		return _port;
	}

	public Transport getTransport()
	{
// FIXME
//		if (_httpConnector.isSecure())
//			return Transport.WSS;
		return Transport.WS;
	}


	@Override
	public void setPort(int port)
	{
		if (isRunning())
			throw new IllegalStateException("running");
		_port = port;
	}
	
	public void setHost(String host)
	{
		if (isRunning())
			throw new IllegalStateException("running");
		_host = host;
	}

	@Override
	public SipURI getURI()
	{
		return _sipUri;
	}

	@Override
	public InetAddress getAddress()
	{
		return _localAddr;
	}
	
	public SipConnection getConnection(InetAddress addr, int port) throws IOException
	{
		synchronized (_connections)
		{
			return _connections.get(key(addr, port));
		}
	}
	
	public WebSocketConnection addConnection(WebSocketConnection connection)
	{
		synchronized (_connections)
		{
			_connections.put(key(connection), connection);
		}
		return connection;
	}
	
	public void removeConnection(WebSocketConnection connection)
	{
		synchronized (_connections)
		{
			_connections.put(key(connection), connection);
		}
	}
	
	private String key(WebSocketConnection connection) 
	{
		return key(connection.getRemoteAddress(), connection.getRemotePort());
	}
	
	private String key(InetAddress addr, int port) 
	{
		return addr.getHostAddress() + ":" + port;
	}

	public static class WebSocketConnection extends WebSocketAdapter implements SipConnection
	{		

		private static WebSocketConnector __connector;
		
		public WebSocketConnection()
		{
		}
		
		public SipConnector getConnector()
		{
			return __connector;
		}

		public InetAddress getLocalAddress()
		{
			return getSession() == null ? null : getSession().getLocalAddress().getAddress();
		}

		public int getLocalPort()
		{
			return getSession() == null ? -1 : getSession().getLocalAddress().getPort();
		}

		public InetAddress getRemoteAddress()
		{
			return getSession() == null ? null : getSession().getRemoteAddress().getAddress();
		}

		public int getRemotePort()
		{
			return getSession() == null ? -1 : getSession().getRemoteAddress().getPort();
		}

		@Override
		public void onWebSocketClose(int statusCode, String reason)
		{
			super.onWebSocketClose(statusCode, reason);

			__connector.removeConnection(this);
		}


		public void onWebSocketText(String data)
		{
			ByteBuffer buffer = ByteBuffer.wrap(data.getBytes());
			
			try
			{

				MessageBuilder builder = new MessageBuilder(__connector._server, this);
				SipParser parser = new SipParser(builder);
				
				parser.parseNext(buffer);
			}
			catch (Throwable t) 
			{
				LOG.warn(t);
				//if (handler.hasException())
					//Log.warn(handler.getException());
	        
				if (LOG.isDebugEnabled())
					LOG.debug("Buffer content: \r\n" + data);

			}
		}

		@Override
		public void send(SipMessage message) throws MessageTooLongException
		{
			// TODO Auto-generated method stub
			
		}


		@Override
		public void write(ByteBuffer buffer) throws IOException
		{
			getSession().getRemote().sendString(buffer.toString());
		}

		@Override
		public Transport getTransport()
		{
			return __connector.getTransport();
		}

		@Override
		public void onWebSocketConnect(Session session)
		{
			super.onWebSocketConnect(session);
			__connector.addConnection(this);
			
		}

		@Override
		public boolean isOpen()
		{
			return super.isConnected();
		}
	
	}
	

}
