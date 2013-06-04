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
package org.cipango.diameter.node;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import org.cipango.diameter.log.DiameterMessageListener;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject("Diameter connector")
public abstract class AbstractDiameterConnector extends ContainerLifeCycle implements DiameterConnector //, Buffers
{
	private static final Logger LOG = Log.getLogger(AbstractDiameterConnector.class);
	
	private static String __localhost;
    
    static
    {
        try
        {
            __localhost = InetAddress.getLocalHost().getHostAddress();
        }
        catch (Exception e)
        {
            LOG.ignore(e);
            __localhost = "127.0.0.1";
        }
    }
	
	private Node _node;
	
	private int _acceptors = 1;
	private int _acceptorPriorityOffset = 0;
	
	private String _host;
	private int _port;
	
	private Thread[] _acceptorThread;
	private ByteBufferPool _byteBufferPool;
	private int _messageBufferSize = 8192;
	
	protected AtomicLong _messagesReceived = new AtomicLong();
	protected AtomicLong _messagesSent = new AtomicLong();
	
	protected DiameterMessageListener _listener;
	
	public AbstractDiameterConnector()
	{
		 _port = getDefaultPort();
	     setHost( __localhost);
	}
	
	@Override
	protected void doStart() throws Exception
	{
		if (_node == null)
			throw new IllegalStateException("No node");
		
		open();
		
		_byteBufferPool = new ArrayByteBufferPool(256, 512, 65536);
			
		super.doStart();
		
		synchronized (this)
		{
			_acceptorThread = new Thread[getAcceptors()];
			for (int i = 0; i < _acceptorThread.length; i++)
			{
				_acceptorThread[i] = new Thread(new Acceptor(i));
				_acceptorThread[i].start();
			}
		}
		LOG.info("Started {}", this);
	}
	
	@Override
	protected void doStop() throws Exception
	{			
		super.doStop();
		
		close();
		
		Thread[] acceptors = null;
        synchronized(this)
        {
            acceptors = _acceptorThread;
            _acceptorThread = null;
        }
        if (acceptors != null)
        {
            for (int i = 0; i < acceptors.length; i++)
            {
                Thread thread = acceptors[i];
                if (thread != null)
                    thread.interrupt();
            }
        }	
	}
	
	public void setMessageListener(DiameterMessageListener listener)
	{	
		updateBean(_listener, listener);
		_listener = listener;
	}
	
	@ManagedAttribute(value="Diameter message listener", readonly=true)
	public DiameterMessageListener getMessageListener()
	{
		return _listener;
	}
	
	public ByteBuffer getBuffer(int size)
	{
		ByteBuffer buffer = _byteBufferPool.acquire(size, false);
		buffer.clear();
		return buffer;
	}
	
	public void returnBuffer(ByteBuffer buffer)
	{
		_byteBufferPool.release(buffer);
	}
	
	public int getMessageBufferSize()
	{
		return _messageBufferSize;
	}
		
	public int getAcceptors()
	{
		return _acceptors;
	}
	
	public void setHost(String host)
	{
		if (host == null)
    		host = __localhost;
		
		_host = host;
	}
	
	public String getHost()
	{
		return _host;
	}
	
	public void setPort(int port)
	{
		_port = port;
	}
	
	public int getPort()
	{
		return _port;
	}
	
	public void setNode(Node node)
	{
		_node = node;
	}
	
	public Node getNode()
	{
		return _node;
	}
	
	protected abstract void accept(int acceptorID) throws IOException, InterruptedException;
	
	protected abstract int getDefaultPort();
	
	@Override
	public String toString()
    {
        String name = this.getClass().getName();
        int dot = name.lastIndexOf('.');
        if (dot>0)
            name=name.substring(dot+1);
        
        return name+"@"+(getHost()==null?"0.0.0.0":getHost())+":"+(getLocalPort()<=0?getPort():getLocalPort());
    }
	
	@ManagedAttribute("Messages received")
	public long getMessageReceived()
	{
		return _messagesReceived.get();
	}
	
	@ManagedAttribute("Messages sent")
	public long getMessageSent()
	{
		return _messagesSent.get();
	}
	
	public void statsReset()
    {
        _messagesReceived.set(0);
        _messagesSent.set(0);
    }
	
	private class Acceptor implements Runnable
	{
		int _acceptor = 0;
		
		Acceptor(int id)
		{
			_acceptor = id;
		}
		public void run()
		{
			Thread current = Thread.currentThread();
			synchronized (AbstractDiameterConnector.this) 
			{
				if (_acceptorThread == null) 
					return;
				_acceptorThread[_acceptor] = current;
			}
			String name = _acceptorThread[_acceptor].getName();
			current.setName(name + " - Acceptor" + _acceptor + " " + AbstractDiameterConnector.this);
			int priority = current.getPriority();
			
			try
			{
				current.setPriority(priority - _acceptorPriorityOffset); 
				while (isRunning())
				{
					try
					{
						accept(_acceptor);
					}
					catch (IOException e)
					{
						LOG.ignore(e);
					}
					catch (Throwable t)
					{
						LOG.warn(t);
					}
				}
			}
			finally 
			{
				current.setPriority(priority);
				current.setName(name);
				try
				{
					if (_acceptor == 0)
						close();
				}
				catch (IOException e)
				{
					LOG.warn(e);
				}
				synchronized (AbstractDiameterConnector.this)
				{
					if (_acceptorThread != null)
						_acceptorThread[_acceptor] = null;
				}
			}
		}
	}
}
