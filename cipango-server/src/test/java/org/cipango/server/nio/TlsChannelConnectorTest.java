package org.cipango.server.nio;

import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLSocket;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.Before;
import org.junit.BeforeClass;

public class TlsChannelConnectorTest extends AbstractConnectorTest
{
	private static SslContextFactory __sslCtxFactory=new SslContextFactory();
	
	@BeforeClass
	public static void initSslEngine() throws Exception
	{
		File keystore = new File(TlsChannelConnectorTest.class.getResource("/org/cipango/server/nio/keystore").getFile());
		__sslCtxFactory.setKeyStorePath(keystore.getAbsolutePath());
		__sslCtxFactory.setKeyStorePassword("storepwd");
		__sslCtxFactory.setKeyManagerPassword("keypwd");
		__sslCtxFactory.start();
	}
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		TlsChannelConnector connector = new TlsChannelConnector(_server, __sslCtxFactory); 
		connector.setHost("localhost");
		connector.setPort(5040);

		startServer(connector);
	}

	@Override
	protected void send(String message) throws Exception
	{
		SSLSocket socket = __sslCtxFactory.newSslSocket();
		socket.connect(new InetSocketAddress(_connector.getAddress(), _connector.getPort()));
		socket.setSoTimeout(10000);
		socket.setTcpNoDelay(true);
		socket.setSoLinger(false, 0);

		OutputStream os = socket.getOutputStream();

		os.write(message.getBytes("UTF-8"));
		os.flush();
		socket.close();
	}

}
