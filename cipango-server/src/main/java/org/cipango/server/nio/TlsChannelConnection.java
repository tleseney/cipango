package org.cipango.server.nio;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLEngine;

import org.cipango.server.MessageTooLongException;
import org.cipango.server.SipConnection;
import org.cipango.server.SipConnector;
import org.cipango.server.SipMessage;
import org.cipango.server.SipMessageGenerator;
import org.cipango.server.Transport;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.FutureCallback;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class TlsChannelConnection extends SslConnection implements SipConnection
{
	private static final Logger LOG = Log.getLogger(SelectSipConnection.class);

	private final TlsChannelConnector _connector;
	private final EndPoint _endpoint;
    private final SipMessageGenerator _sipGenerator;
    private final ByteBufferPool _bufferPool;

	public TlsChannelConnection(ByteBufferPool byteBufferPool, Executor executor, EndPoint endpoint,
			SSLEngine sslEngine, TlsChannelConnector connector)
	{
		super(byteBufferPool, executor, endpoint, sslEngine);
		_connector = connector;
		_endpoint = endpoint;
		_bufferPool = byteBufferPool;
        _sipGenerator = new SipMessageGenerator();
	}

	@Override
	public SipConnector getConnector()
	{
		return _connector;
	}

	@Override
	public Transport getTransport()
	{
		return Transport.TLS;
	}

	@Override
	public InetAddress getLocalAddress()
	{
		return _endpoint.getLocalAddress().getAddress();
	}

	@Override
	public int getLocalPort()
	{
		return _endpoint.getLocalAddress().getPort();
	}

	@Override
	public InetAddress getRemoteAddress()
	{
		return _endpoint.getRemoteAddress().getAddress();
	}

	@Override
	public int getRemotePort()
	{
		return _endpoint.getRemoteAddress().getPort();
	}

	@Override
	public void send(SipMessage message) throws MessageTooLongException
	{
		ByteBuffer buffer = null;
		int bufferSize = SelectSipConnection.MINIMAL_BUFFER_LENGTH;
		
		while (true)
		{
			buffer = _bufferPool.acquire(bufferSize, false);
			buffer.clear();

			try
			{
				_sipGenerator.generateMessage(buffer, message);
				break;
			}
			catch (MessageTooLongException e)
			{
				if (bufferSize < SelectSipConnection.MAXIMAL_BUFFER_LENGTH)
					bufferSize += SelectSipConnection.MINIMAL_BUFFER_LENGTH + message.getContentLength();
				else
					throw e;
			}
		}
		
		buffer.flip();
		try
		{
			write(buffer);
		}
		catch (Exception e)
		{
			LOG.warn(e);
		}
		
		_bufferPool.release(buffer);
	}

	@Override
	public void write(ByteBuffer buffer) throws IOException
	{
		try
		{
	        FutureCallback fcb = new FutureCallback();
	        if (BufferUtil.hasContent(buffer))
	        	getDecryptedEndPoint().write(fcb, buffer);
	        fcb.get();
		}
		catch (InterruptedException x)
        {
            throw (IOException)new InterruptedIOException().initCause(x);
        }
        catch (ExecutionException x)
        {
            Throwable cause = x.getCause();
            if (cause instanceof IOException)
                throw (IOException)cause;
            else if (cause instanceof Exception)
                throw new IOException(cause);
            else
                throw (Error)cause;
        }
	}

	@Override
	public boolean isOpen()
	{
		return getEndPoint().isOpen() && !getSSLEngine().isInboundDone();
	}

	@Override
	public void onClose()
	{
		_connector.removeConnection(this);
		super.onClose();
	}
}
