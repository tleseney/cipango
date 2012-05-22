package org.cipango.server.processor;

import java.util.HashMap;
import java.util.Map;

import org.cipango.server.SipMessage;
import org.cipango.server.SipProcessor;
import org.cipango.server.session.CallSessionManager;
import org.cipango.server.session.CallSessionManager.SessionScope;
import org.eclipse.jetty.util.LazyList;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class SessionProcessor extends SipProcessorWrapper
{
	private final Logger LOG = Log.getLogger(SessionProcessor.class);

	private Map<String, Queue> _queues = new HashMap<String, Queue>();
	private CallSessionManager _sessionManager;

	public SessionProcessor(SipProcessor processor)
	{
		super(processor);
	}
	
	protected String getCallSessionID(SipMessage message)
	{
		return message.getCallId();
	}
	
	@Override
	protected void doStart() throws Exception
	{
		_sessionManager = getServer().getSessionManager();
		if (_sessionManager == null)
			throw new IllegalStateException("no session manager");
		
		super.doStart();
	}
	
	@Override
	public void doProcess(SipMessage message)
	{
		String id = getCallSessionID(message);
		
		Queue queue = null;
		synchronized (_queues)
		{
			queue = _queues.get(id);
			if (queue == null)
			{
				queue = new Queue(id);
				_queues.put(id, queue);
			}
			queue.add(message);
		}
		queue.doProcess();
	}
	
	class Queue
	{
		private String _id;
		private Object _messages;
		private boolean _processing = false;
		
		public Queue(String id)
		{
			_id = id;
		}
		
		public synchronized void add(SipMessage message)
		{
			_messages = LazyList.add(_messages, message);
		}
		
		public synchronized SipMessage poll()
		{
			if (LazyList.size(_messages) == 0)
				return null;
			SipMessage message = (SipMessage) LazyList.get(_messages, 0);
			_messages = LazyList.remove(_messages, 0);
			return message;
		}
		
		private boolean isDone()
		{
			synchronized (_queues)
			{
				synchronized (this)
				{
					if (LazyList.size(_messages) == 0)
					{
						_queues.remove(_id);
						return true;
					}
				}
			}
			return false;
		}
		
		public void doProcess()
		{
			synchronized (this)
			{
				if (_processing)
					return;
				_processing = true;
			}
			
			do 
			{
				SessionScope scope = _sessionManager.openScope(_id);
				
				if (scope.getCallSession() == null)
				{
					// TODO schedule later
					return;
				}
				try
				{
					SipMessage message;
					
					while ((message = poll()) != null)
					{
						try
						{
							message.setCallSession(scope.getCallSession());
							SessionProcessor.super.doProcess(message);
						}
						catch (Exception e)
						{
							e.printStackTrace();
							LOG.debug(e);
						}
					}
				}
				finally
				{
					scope.close();
				}
			}
			while (!isDone());
		}
	}
}
