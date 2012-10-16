// ========================================================================
// Copyright 2010 NEXCOM Systems
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.concurrent.Executor;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.cipango.server.SipConnection;
import org.cipango.server.SipServer;
import org.cipango.server.Transport;
import org.cipango.server.nio.TcpConnector.MessageBuilder;
import org.cipango.server.servlet.DefaultServlet;
import org.cipango.server.sipapp.SipAppContext;
import org.cipango.sip.SipVersion;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.ssl.SslContextFactory;

@Deprecated
public class TlsConnector  extends TcpConnector
{
	private static final Logger LOG = Log.getLogger(TlsConnector.class);
	private SslContextFactory _sslContextFactory;
	private boolean _allowRenegotiate = true;
	private int _handshakeTimeout = 0; // 0 means use maxIdleTime
	
    public TlsConnector(
    		@Name("sipServer") SipServer server,
    		@Name("sslContextFactory") SslContextFactory sslContextFactory)
    {
        this(server, null, Math.max(1,(Runtime.getRuntime().availableProcessors())/2), sslContextFactory);
    }

    public TlsConnector(
            @Name("sipServer") SipServer server,
            @Name("acceptors") int acceptors,
    		@Name("sslContextFactory") SslContextFactory sslContextFactory)
    {
        this(server, null, acceptors, sslContextFactory);
    }

    public TlsConnector(
            @Name("sipServer") SipServer server,
            @Name("executor") Executor executor,
            @Name("acceptors") int acceptors,
            @Name("sslContextFactory") SslContextFactory sslContextFactory)
    {
    	super(server, executor, acceptors);

    	_sslContextFactory = sslContextFactory;
        addBean(_sslContextFactory);
	}
	

	@Override
	protected void doStart() throws Exception
	{
		_sslContextFactory.start();
		super.doStart();
	}

	@Override
	public Transport getTransport()
	{
		return Transport.TLS;
	}
	
	public SslContextFactory getSslContextFactory()
	{
		return _sslContextFactory;
	}
	
	public ServerSocket newServerSocket() throws IOException
	{
		return _sslContextFactory.newSslServerSocket(getHost(), getPort(), getBacklogSize());
	}

	@Override
	protected TcpConnection newConnection(Socket socket) throws IOException
	{
		return new TlsConnection((SSLSocket) socket);
	}
	
	/**
	 * Set the time in milliseconds for so_timeout during ssl handshaking
	 * 
	 * @param msec
	 *            a non-zero value will be used to set so_timeout during ssl
	 *            handshakes. A zero value means the maxIdleTime is used
	 *            instead.
	 */
	public void setHandshakeTimeout(int msec)
	{
		_handshakeTimeout = msec;
	}

	public int getHandshakeTimeout()
	{
		return _handshakeTimeout;
	}
	
    /**
     * Return the chain of X509 certificates used to negotiate the SSL Session.
     * <p>
     * Note: in order to do this we must convert a javax.security.cert.X509Certificate[], as used by
     * JSSE to a java.security.cert.X509Certificate[],as required by the Servlet specs.
     * 
     * @param sslSession the javax.net.ssl.SSLSession to use as the source of the cert chain.
     * @return the chain of java.security.cert.X509Certificates used to negotiate the SSL
     *         connection. <br>
     *         Will be null if the chain is missing or empty.
     */
    private static X509Certificate[] getCertChain(SSLSession sslSession)
    {
        try
        {
            javax.security.cert.X509Certificate javaxCerts[] = sslSession.getPeerCertificateChain();
            if (javaxCerts == null || javaxCerts.length == 0)
                return null;

            int length = javaxCerts.length;
            X509Certificate[] javaCerts = new X509Certificate[length];

            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
            for (int i = 0; i < length; i++)
            {
                byte bytes[] = javaxCerts[i].getEncoded();
                ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
                javaCerts[i] = (X509Certificate) cf.generateCertificate(stream);
            }

            return javaCerts;
        }
        catch (SSLPeerUnverifiedException pue)
        {
            return null;
        }
        catch (Exception e)
        {
            LOG.warn(Log.EXCEPTION, e);
            return null;
        }
    }

	protected TlsConnection newConnection(InetAddress addr, int port) throws IOException
	{
		SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(addr, port);
		return new TlsConnection(sslsocket);
	}
	
	
	public class TlsConnection extends TcpConnection
	{
		private SSLSocket _socket;

		public TlsConnection(SSLSocket socket) throws IOException
		{
			super(socket);
			_socket = socket;
		}
		
		public SSLSocket getSocket()
		{
			return _socket;
		}
		
		public void run()
		{
			try
			{
				int handshakeTimeout = getHandshakeTimeout();
				int oldTimeout = _socket.getSoTimeout();
				if (handshakeTimeout > 0)
					_socket.setSoTimeout(handshakeTimeout);

				_socket.addHandshakeCompletedListener(new HandshakeCompletedListener()
				{
					boolean handshook = false;

					public void handshakeCompleted(HandshakeCompletedEvent event)
					{
						if (handshook)
						{
							if (!_allowRenegotiate)
							{
								LOG.warn("SSL renegotiate denied: " + _socket);
								try
								{
									_socket.close();
								}
								catch (IOException e)
								{
									LOG.warn(e);
								}
							}
						}
						else
							handshook = true;
					}
				});
				_socket.startHandshake();

				if (handshakeTimeout > 0)
					_socket.setSoTimeout(oldTimeout);

				super.run();
				
			}
			catch (SSLException e)
			{
				LOG.warn(e);
				close();
	
			}
			catch (IOException e)
			{
				LOG.debug(e);
				close();
			}
		}
				
		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("TLS Connection ");
			sb.append(getLocalAddress()).append(":").append(getLocalPort());
			sb.append(" - ");
			sb.append(getRemoteAddress()).append(":").append(getRemotePort());
			return sb.toString();
		}

	}

	public class TlsMessageBuilder extends MessageBuilder
	{

		public TlsMessageBuilder(SipServer server, SipConnection connection)
		{
			super(server, connection);
		}

		@Override
		public boolean messageComplete(ByteBuffer content)
		{
			try
			{
				TlsConnection tlsConnection = (TlsConnection) _connection;
				SSLSession sslSession = tlsConnection.getSocket().getSession();
	            X509Certificate[] certs = (X509Certificate[]) sslSession.getValue(X509Certificate.class.getName());
	            if (certs == null)
	            {
	                certs = getCertChain(sslSession);
	                if (certs == null)
	                	certs = new X509Certificate[0];
	                sslSession.putValue(X509Certificate.class.getName(), certs);
	            }

				if (certs.length > 0)
					_message.setAttribute("javax.servlet.request.X509Certificate", certs);
				else if (_sslContextFactory.getNeedClientAuth()) // Sanity check
					throw new IllegalStateException("no client auth");

			}
			catch (Exception e)
			{
				LOG.warn(Log.EXCEPTION, e);
			}
			return super.messageComplete(content);
		}

		@Override
		public boolean startRequest(String method, String uri, SipVersion version) throws ParseException
		{
			LOG.warn("startRequest {}", method);
			return super.startRequest(method, uri, version);
		}

		@Override
		public void badMessage(int status, String reason)
		{
			LOG.warn("Bad message " + reason, new Exception());
			super.badMessage(status, reason);
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
		
		SslContextFactory factory = new SslContextFactory();
		factory.setKeyStorePath("C:\\projects\\cipango\\cipango-distribution\\target\\distribution\\etc\\keystore");
		factory.setKeyStorePassword("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");
		factory.setKeyManagerPassword("OBF:1u2u1wml1z7s1z7a1wnl1u2g");
		//factory.setIncludeCipherSuites("TLS_RSA_WITH_AES_128_CBC_SHA256", "TLS_RSA_WITH_3DES_EDE_CBC_SHA");

		SipServer sipServer = new SipServer();
		TlsConnector connector = new TlsConnector(sipServer, factory);
		connector.setHost(host);
		connector.setPort(5061);
			
		sipServer.addConnector(connector);
		
		SipAppContext context = new SipAppContext();
		context.getSipServletHandler().addSipServlet(DefaultServlet.class.getName());
		
		sipServer.setHandler(context);
		
		sipServer.start();
	}
}
