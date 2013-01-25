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
package org.cipango.server.dns;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.cipango.server.SipResponse;
import org.cipango.sip.SipHeader;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.Dumpable;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject("Black list")
public class BlackListImpl implements BlackList, Dumpable
{
	private static final Logger LOG = Log.getLogger(BlackListImpl.class);
	
	/** 5 minutes */
	public static final long DEFAULT_BLACK_LIST_DURATION = 300;
	
	public enum Criteria 
	{
		IP_ADDRESS, IP_PORT, IP_PORT_TRANSPORT;
	}
	
	
	private long _blackListDuration;
	private ConcurrentHashMap<String, ExpirableHop> _map = new ConcurrentHashMap<String, ExpirableHop>();
	private Criteria _criteria = Criteria.IP_ADDRESS;
	
	private long _scavengePeriod = 60000;
	private long _nextScavenge = System.currentTimeMillis() + _scavengePeriod;
	
	public BlackListImpl()
	{
		setBlackListDuration(DEFAULT_BLACK_LIST_DURATION);
	}
	
	@Override
	public boolean isBlacklisted(Hop hop)
	{
		if (System.currentTimeMillis() < _nextScavenge)
			scavenge();
		
		String key = getKey(hop);		
		ExpirableHop expirableHop = _map.get(key);
		if (expirableHop == null)
			return false;
		if (expirableHop.isExpired())
		{
			LOG.debug("The hop {} is no more blacklisted", expirableHop.getHop());
			_map.remove(key);
			return false;
		}
		return true;
	}
	
	private String getKey(Hop hop)
	{
		String criteria;
		switch (_criteria)
		{
		case IP_ADDRESS:
			criteria = hop.getAddress().getHostAddress();
			break;
		case IP_PORT:
			criteria = hop.getAddress().getHostAddress() + ":" + hop.getPort();
			break;
		case IP_PORT_TRANSPORT:
			criteria = hop.getAddress().getHostAddress() + ":" + hop.getPort() + "/" + hop.getTransport();
			break;
		default:
			criteria = hop.getAddress().getHostAddress();
			break;
		}
		return criteria;
	}
	
	@Override
	public void hopFailed(Hop hop, Reason reason, SipResponse response)
	{
		blackListHop(hop, getBlacklistDuration(response));
	}
	
	protected long getBlacklistDuration(SipResponse response)
	{
		if (response == null)
			return _blackListDuration;
		
		int retryAfter = -1;
		String sRetryAfter= response.getHeader(SipHeader.RETRY_AFTER.asString());
		if (sRetryAfter != null)
		{
			try
			{
				retryAfter = Integer.parseInt(sRetryAfter);
			}
			catch (Exception e)
			{
				LOG.debug("Failed to parse Retry-After header", e);
				return _blackListDuration;
			}
			if (retryAfter >= 0)
				return retryAfter * 1000;
			
			LOG.debug("Negative Retry-After header in 503 response, blacklist server for " + getBlackListDuration() + "s");
			return _blackListDuration;
		}
		LOG.debug("No Retry-After header in 503 response, blacklist server for " + getBlackListDuration() + "s");
		return _blackListDuration;
	}
	
	protected void blackListHop(Hop hop, long duration)
	{
		LOG.debug("The hop {} is now blacklisted for {} seconds", hop, duration / 1000);
		_map.putIfAbsent(getKey(hop), new ExpirableHop(hop, duration));
	}
	
	@ManagedOperation(value="Remove hops that are no more blacklisted", impact="ACTION")
	public void scavenge()
	{
		_nextScavenge = System.currentTimeMillis() + _scavengePeriod;
		
		Iterator<ExpirableHop> it = _map.values().iterator();
		while (it.hasNext())
		{
			ExpirableHop expirableHop = it.next();
			if (expirableHop.isExpired())
			{
				LOG.debug("The hop {} is no more blacklisted", expirableHop.getHop());
				it.remove();
			}
		}
	}
	
	/**
	 * Returns the black list duration in seconds.
	 */
	@ManagedAttribute("Black list duration in seconds")
	public long getBlackListDuration()
	{
		return _blackListDuration / 1000;
	}

	/**
	 * Sets the black list duration in seconds.
	 */
	public void setBlackListDuration(long blackListDuration)
	{
		_blackListDuration = blackListDuration * 1000;
	}

	@ManagedAttribute("Criteria")
	public Criteria getCriteria()
	{
		return _criteria;
	}

	public void setCriteria(Criteria criteria)
	{
		_criteria = criteria;
	}
	
	public void setBlackListCriteria(String criteria)
	{
		_criteria = Criteria.valueOf(criteria.toUpperCase());
	}

	@ManagedAttribute("Scavenge period")
	public long getScavengePeriod()
	{
		return _scavengePeriod;
	}

	public void setScavengePeriod(long scavengePeriod)
	{
		_scavengePeriod = scavengePeriod;
	}

	@Override
	public String dump()
	{
		return ContainerLifeCycle.dump(this);
	}

	@Override
	public void dump(Appendable out, String indent) throws IOException
	{
		out.append(toString()).append('\n');
		for (Entry<String, ExpirableHop> entry : _map.entrySet())
		{
			out.append(indent).append("  - ");
			out.append(entry.getKey()).append("@").append(String.valueOf(entry.getValue().getRemaining() / 1000)).append("s\n");
		}
		
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "{c=" + _criteria + ", d=" + getBlackListDuration() + ", size=" + _map.size() + "}";
	}
	
	protected static class ExpirableHop
	{
		private Hop _hop;
		private long _expirationDate;
		
		public ExpirableHop(Hop hop, long duration)
		{
			_hop = hop;
			_expirationDate = System.currentTimeMillis() + duration;
		}

		public Hop getHop()
		{
			return _hop;
		}
		
		
		public boolean isExpired()
		{
			return System.currentTimeMillis() > _expirationDate;
		}
		
		public long getRemaining()
		{
			return _expirationDate - System.currentTimeMillis();
		}
		
		public String toString()
		{
			return _hop.toString() + "@" + (getRemaining() / 1000) + "s";
		}
	}



}
