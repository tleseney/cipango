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

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.cipango.dns.record.OptRecord;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject("DNS resolver")
public class Resolver
{
	public static final int DEFAULT_PORT = 53;
	public static final int DEFAULT_TIMEOUT = 1000;
	
	private static final Logger LOG = Log.getLogger(Resolver.class);
	private DnsClient _dnsClient;
	private InetAddress _host;
	private int _port = DEFAULT_PORT;
	private long _timeout = DEFAULT_TIMEOUT;
	private int _attempts = 2;
	
	private OptRecord _queryOpt;
		
	public Resolver() {
	}
	
	public Resolver(String host) throws UnknownHostException {
		setHost(host);
	}
	
	
	public DnsMessage resolve(DnsMessage query) throws IOException
	{
		DnsConnection c = _dnsClient.getDefaultConnector().getConnection(_host, _port);
		
		if (_queryOpt != null && query.getAdditionalSection().get(OptRecord.class) == null)
			query.getAdditionalSection().add(_queryOpt); // TODO clone
		
		int timeout = (int) _timeout;
		for (int i = 0; i < _attempts; i++)
		{
			c.send(query);
			DnsMessage answer;
			
			answer = c.waitAnswer(query, timeout);
			if (answer != null)
			{
				if (answer.getHeaderSection().isTruncated() && !_dnsClient.getDefaultConnector().isTcp())
				{
					DnsConnector tcpConnector =  _dnsClient.getTcpConnector();
					if (tcpConnector == null)
						LOG.debug("Ignore truncated flag as there is no TCP connector");
					else {
						c = tcpConnector.getConnection(_host, _port);
						c.send(query);
						answer = c.waitAnswer(query, timeout);
						if (answer == null)
							throw new SocketTimeoutException("No response received for query " + query.getQuestionSection());
						return answer;
					}
				}
				return answer;
			}
			timeout *= 2;
		}
		throw new SocketTimeoutException("No response received for query " + query.getQuestionSection());
		
	}

	@ManagedAttribute(value="Host", readonly=true)
	public InetAddress getHost()
	{
		return _host;
	}

	public void setHost(String host) throws UnknownHostException
	{
		_host = InetAddress.getByName(host);
	}

	public void setHost(InetAddress host)
	{
		_host = host;
	}

	@ManagedAttribute(value="Port", readonly=true)
	public int getPort()
	{
		return _port;
	}

	public void setPort(int port)
	{
		_port = port;
	}

	@ManagedAttribute("Timeout")
	public long getTimeout()
	{
		return _timeout;
	}

	public void setTimeout(long timeout)
	{
		_timeout = timeout;
	}

	@ManagedAttribute("Attempts")
	public int getAttempts()
	{
		return _attempts;
	}

	public void setAttempts(int attempts)
	{
		_attempts = attempts;
	}

	public DnsClient getDnsClient()
	{
		return _dnsClient;
	}

	public void setDnsClient(DnsClient dnsClient)
	{
		_dnsClient = dnsClient;
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "@" + _host.getHostAddress() + ":" + _port;
	}

	public OptRecord getQueryOpt()
	{
		return _queryOpt;
	}

	public void setQueryOpt(OptRecord queryOpt)
	{
		_queryOpt = queryOpt;
	}


}
