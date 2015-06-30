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
package org.cipango.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;

public abstract class AbstractConnector extends AbstractLifeCycle implements DnsConnector
{
	private static final Logger LOG = Log.getLogger(AbstractConnector.class);
	private InetAddress _host;
	private int _timeout = Resolver.DEFAULT_TIMEOUT * 10;
	protected Map<Integer, MsgContainer> _queries = new HashMap<Integer, MsgContainer>();
	private Executor _executor;
	
	@Override
	protected void doStart() throws Exception
	{
		super.doStart();
		if (_executor == null)
			setExecutor(new ExecutorThreadPool(1, 10, 2000));
	}
	
	public String getHost()
	{
		if (_host == null)
			return null;
		return _host.getHostName();
	}
	
	public InetAddress getHostAddr()
	{
		return _host;
	}

	public void setHost(String host)
	{
		try
		{
			_host = InetAddress.getByName(host);
		}
		catch (UnknownHostException e) 
		{
			LOG.debug(e);
		}
	}
	
	public int getTimeout()
	{
		return _timeout;
	}

	public void setTimeout(int timeout)
	{
		_timeout = timeout;
	}
	
	protected void addQuery(DnsMessage query) 
	{
		synchronized (_queries)
		{
			if (!_queries.containsKey(query.getHeaderSection().getId()))
				_queries.put(query.getHeaderSection().getId(), new MsgContainer(query));
			else
			{
				int id = ThreadLocalRandom.current().nextInt() & 0xFFFF;
				LOG.warn("ID {} is already in use. Change ID to {}",  query.getHeaderSection().getId(), id);
				query.getHeaderSection().setId(id);
				addQuery(query);
			}
		}
	}
	
	protected void updateQueryOnAnswer(DnsMessage answer) 
	{
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
			LOG.warn("Drop DNS {}, as can not found a query with same ID", answer);
	}
	
	protected DnsMessage waitAnswer(DnsMessage request, int timeout)
	{
		MsgContainer messages;
		synchronized (_queries)
		{
			messages = _queries.get(request.getHeaderSection().getId());
		}
		synchronized (request)
		{
			if (messages == null || messages.getAnswer() == null)
				try { request.wait(timeout); } catch (InterruptedException e) {}				
		}
		synchronized (_queries)
		{

			messages = _queries.remove(request.getHeaderSection().getId());
		}
		if (messages == null)
			return null;
		return messages.getAnswer();
	}
	
	public Executor getExecutor()
	{
		return _executor;
	}

	public void setExecutor(Executor executor)
	{
		_executor = executor;
	}

	public static class MsgContainer
	{
		private DnsMessage _query;
		private DnsMessage _answer;
		
		public MsgContainer(DnsMessage query)
		{
			_query = query;
		}

		public DnsMessage getAnswer()
		{
			return _answer;
		}

		public void setAnswer(DnsMessage answer)
		{
			_answer = answer;
		}

		public DnsMessage getQuery()
		{
			return _query;
		}
	}

}
