package org.cipango.server.nio;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

import org.cipango.server.AbstractSipConnector;
import org.cipango.server.SipConnection;
import org.cipango.server.SipConnector;
import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.SipServer;
import org.cipango.server.Transport;
import org.cipango.sip.SipGenerator;
import org.cipango.sip.SipHeader;
import org.cipango.sip.SipParser;
import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.FutureCallback;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class SelectSipConnection extends AbstractConnection implements SipConnection
{
	private static final Logger LOG = Log.getLogger(SelectSipConnection.class);
    
	public static final int MINIMAL_BUFFER_LENGTH = 2048;
	
    private final ByteBufferPool _bufferPool;
	private SelectChannelConnector _connector;
	private EndPoint _endpoint;
	private SipParser _parser;
    ByteBuffer _buffer;
	
	public SelectSipConnection(SelectChannelConnector connector, EndPoint endpoint)
	{
		super(endpoint, connector.getExecutor());
		_connector = connector;
		_endpoint = endpoint;
        _bufferPool = _connector.getByteBufferPool();

        MessageBuilder builder = new MessageBuilder(getServer(), this);
        _parser = new SipParser(builder);
	}

	public SipServer getServer()
	{
		return _connector.getServer();
	}
	
    @Override
    public void onOpen()
    {
        super.onOpen();
        fillInterested();
    }
    
	@Override
	public void onFillable()
	{
        LOG.debug("{} onReadable", this);
        try
        {
            while (true)
            {
                // Fill the request buffer with data only if it is totally empty.
                if (BufferUtil.isEmpty(_buffer))
                {
                    if (_buffer == null)
                        _buffer = _bufferPool.acquire(MINIMAL_BUFFER_LENGTH, true);

                    int filled = getEndPoint().fill(_buffer);

                    LOG.debug("{} filled {}", this, filled);

                    // If we failed to fill
                    if (filled == 0)
                    {
                        // Schedule another attempt.
                        releaseBuffer();
                        fillInterested();
                        return;
                    }
                    else if (filled < 0)
                    {
                    	_parser.reset();
                    	getEndPoint().shutdownOutput();
                    	releaseBuffer();
                    	return;
                    }
                }

                // Parse the buffer
                if (_parser.parseNext(_buffer))
                	_parser.reset();
            }
        }
        catch(IOException e)
        {
//            if (_parser.isIdle())
//                LOG.debug(e);
//            else
        	LOG.warn(this.toString(), e);
        	getEndPoint().close();
        }
        catch(Exception e)
        {
            LOG.warn(this.toString(), e);
            getEndPoint();
        }
	}
    
	@Override
	public SipConnector getConnector()
	{
		return _connector;
	}

	@Override
	public Transport getTransport()
	{
		return Transport.TCP;
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
	public void send(SipMessage message)
	{
		SipResponse response = (SipResponse) message;
		ByteBuffer buffer = _bufferPool.acquire(MINIMAL_BUFFER_LENGTH, false);
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
		
		_bufferPool.release(buffer);
	}

	@Override
	public synchronized void write(ByteBuffer buffer) throws IOException
	{
		try
		{
	        FutureCallback<Void> fcb = new FutureCallback<Void>();
	        if (BufferUtil.hasContent(buffer))
	        	getEndPoint().write(null, fcb, buffer);
	        else
	        	fcb.completed(null);
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
	public String toString()
	{
		return getRemoteAddress() + ":" + getRemotePort();
	}

    private void releaseBuffer()
    {
        if (_buffer != null && !_buffer.hasRemaining())
        {
            _bufferPool.release(_buffer);
            _buffer = null;
        }
    }
    
	class MessageBuilder extends AbstractSipConnector.MessageBuilder
	{
		public MessageBuilder(SipServer server, SipConnection connection)
		{
			super(server, connection);
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
	}
}
