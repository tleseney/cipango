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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.cipango.dns.bio.TcpConnector;
import org.cipango.dns.bio.UdpConnector;
import org.cipango.dns.record.ARecord;
import org.cipango.dns.record.AaaaRecord;
import org.cipango.dns.record.PtrRecord;
import org.cipango.dns.record.Record;
import org.cipango.util.StringScanner;
import org.eclipse.jetty.util.ArrayUtil;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

@ManagedObject("DNS Service")
public class DnsService extends ContainerLifeCycle implements DnsClient
{
	private static final Logger LOG = Log.getLogger(DnsService.class);
	private Cache _cache;
	private Name[] _searchList;
	private Resolver[] _resolvers;
	private DnsConnector[] _connectors;
	private DnsConnector _tcpConnector;
	private boolean _preferIpv6 = false;
	
	private Map<Name, InetAddress[]> _staticHostsByName = new HashMap<Name, InetAddress[]>();
	private Map<Name, String> _staticHostsByAddr = new HashMap<Name, String>();
	
	private Executor _executor;

	@SuppressWarnings("restriction")
	@Override
	protected void doStart() throws Exception
	{
		if (_executor == null) {
			QueuedThreadPool executor = new QueuedThreadPool(10, 1, 2000);
			executor.setName("qtp-dns");
			executor.setDaemon(true);
			setExecutor(executor);
		}
		
		if (_resolvers == null || _resolvers.length == 0)
		{
			sun.net.dns.ResolverConfiguration resolverConfiguration = sun.net.dns.ResolverConfiguration
					.open();
			List<String> servers = resolverConfiguration.nameservers();
			int attempts = resolverConfiguration.options().attempts();
			int retrans = resolverConfiguration.options().retrans();

			for (String server : servers)
			{
				Resolver resolver = new Resolver();
				resolver.setHost(server);
				if (attempts != -1)
					resolver.setAttempts(attempts);
				if (retrans != -1)
					resolver.setTimeout(retrans);
				addResolver(resolver);
			}
		}

		if (_connectors == null || _connectors.length == 0)
		{
			setConnectors(new DnsConnector[] { new UdpConnector(), new TcpConnector() });
		}
		
		for (DnsConnector connector : _connectors)
			if (connector.getExecutor() == null)
				connector.setExecutor(_executor);

		if (_searchList == null)
		{
			sun.net.dns.ResolverConfiguration resolverConfiguration = sun.net.dns.ResolverConfiguration
					.open();
			List<Name> searchList = new ArrayList<Name>();
			for (Object name : resolverConfiguration.searchlist())
				searchList.add(new Name((String) name));
			_searchList = searchList.toArray(new Name[searchList.size()]);
		}

		if (_cache == null)
			setCache(new Cache());
		
		addEtcHosts();
		
		super.doStart();
	}
	
	@Override
	protected void doStop() throws Exception
	{
		if (_connectors != null)
		{
			for (int i = 0; i < _connectors.length; i++)
				if (_connectors[i] instanceof LifeCycle)
					((LifeCycle) _connectors[i]).stop();
		}
		super.doStop();
	}
	
	public InetAddress[] lookupAllHostAddr(String host) throws UnknownHostException
	{
		try
		{
			Name name = new Name(host);
			
			InetAddress[] array = _staticHostsByName.get(name);
	
			if (array != null)
				return array;
			
			List<Record> records = null;
			if (_preferIpv6)
				records = lookup(new AaaaRecord(name));
			if (records == null)
				records = lookup(new ARecord(name));
			if (records == null && !_preferIpv6)
				records = lookup(new AaaaRecord(name));
			if (records == null)
				throw new UnknownHostException(host);
	
			array = new InetAddress[records.size()];
			for (int i = 0; i < records.size(); i++)
			{
				if (records.get(i) instanceof ARecord)
				{
					ARecord a = (ARecord) records.get(i);
					array[i] = a.getAddress();
				}
				else
				{
					AaaaRecord aaaa = (AaaaRecord) records.get(i);
					array[i] = aaaa.getAddress();
				}
			}
	
			return array;
		}
		catch (Exception e)
		{
			if (e instanceof UnknownHostException)
				throw (UnknownHostException) e;
			LOG.debug(e);
			throw new UnknownHostException(host);
		}
	}
	
	public String getHostByAddr(byte[] addr) throws UnknownHostException {
		try
		{
			Name name = PtrRecord.getReverseName(InetAddress.getByAddress(addr));
			String reverse = _staticHostsByAddr.get(name);
			if (reverse != null)
				return reverse;
			
    		PtrRecord ptrRecord = new PtrRecord(name);
    		List<Record> records = new Lookup(this, ptrRecord).resolve();
    		PtrRecord result = (PtrRecord) records.get(0);
    		return result.getPrtdName().toString();
		}
		catch (IOException e) {
			if (e instanceof UnknownHostException)
				throw (UnknownHostException) e;
			LOG.debug(e);
			throw new UnknownHostException();
		}
	}

	public List<Record> lookup(Record record) throws IOException
	{
		return new Lookup(this, record).resolve();

	}

	@ManagedAttribute(value="Cache", readonly=true)
	public Cache getCache()
	{
		return _cache;
	}

	public void setCache(Cache cache)
	{
		updateBean(_cache, cache);
		_cache = cache;
	}

	@ManagedAttribute(value="Search list")
	public Name[] getSearchList()
	{
		return _searchList;
	}

	public void setSearchList(Name[] searchList)
	{
		_searchList = searchList;
	}

	public DnsMessage resolve(DnsMessage query) throws IOException
	{
		IOException e = null;
		for (Resolver resolver : _resolvers)
		{
			try
			{
				return resolver.resolve(query);
			}
			catch (IOException e1)
			{
				e = e1;
			}
		}
		if (e == null)
			throw new IOException("No resovler");
		else
			throw e;
	}

	public void addResolver(Resolver resolver)
	{
		setResolvers(ArrayUtil.addToArray(getResolvers(), resolver, Resolver.class));
	}

	public void addConnector(DnsConnector connector)
	{
		setConnectors(ArrayUtil.addToArray(getConnectors(), connector, DnsConnector.class));
	}

	public DnsConnector getDefaultConnector()
	{
		if (_connectors == null || _connectors.length == 0)
			return null;
		return _connectors[0];
	}
	
	public DnsConnector getTcpConnector() {
		return _tcpConnector;
	}

	@ManagedAttribute(value="Resolvers", readonly=true)
	public Resolver[] getResolvers()
	{
		return _resolvers;
	}

	@ManagedAttribute(value="Connectors", readonly=true)
	public DnsConnector[] getConnectors()
	{
		return _connectors;
	}

	public void setConnectors(DnsConnector[] connectors)
	{
		updateBeans(_connectors, connectors);
		_tcpConnector = null;
		if (connectors != null)
		{
			for (int i = 0; i < connectors.length; i++)
			{
				if (connectors[i].getExecutor() == null)
					connectors[i].setExecutor(_executor);
				if (connectors[i].isTcp())
				{
					_tcpConnector = connectors[i];
					break;
				}
			}
		}
		_connectors = connectors;
	}

	public void setResolvers(Resolver[] resolvers)
	{
		updateBeans(_resolvers, resolvers);
		if (resolvers != null) 
		{
			for (int i = 0; i < resolvers.length; i++)
				resolvers[i].setDnsClient(this);
		}
		_resolvers = resolvers;
		
	}
	
	protected void addEtcHosts()
	{
		try
		{
			String os = System.getProperty("os.name");
			File file;
			if (os.startsWith("Windows"))
				file = new File("C:/WINDOWS/system32/drivers/etc/hosts");
			else
				file = new File("/etc/hosts");
			
			if (!file.exists() || !file.isFile() || !file.canRead())
			{
				LOG.warn("Unable to read " + file.getAbsolutePath());
				return;
			}
			LOG.debug("Read hosts file: " + file.getAbsolutePath());
			
			addEtcHosts(new FileInputStream(file));
		}
		catch (Exception e)
		{
			LOG.warn("Parse error in hosts file", e);
		}
	}

	protected void addEtcHosts(InputStream is)
	{
		
		BufferedReader br = null;
		try
		{	
			
			br = new BufferedReader(new InputStreamReader(is)); 
			String line;
			while ((line = br.readLine()) != null)
			{
				try
				{
					if (line.indexOf('#') != -1)
						line = line.substring(0, line.indexOf('#'));
					StringScanner scanner = new StringScanner(line);
					scanner.skipWhitespace();
			
					if (scanner.eof())
						continue;
					
					String addr = scanner.readToSpace();
					InetAddress ip = InetAddress.getByName(addr);
					scanner.skipWhitespace();
					
					boolean first = true;
					while (!scanner.eof())
					{
						String host = scanner.readToSpace();
						scanner.skipWhitespace();
						LOG.debug("Assign host " + host + " to IP address " + ip + " from hosts file");
						_staticHostsByName.put(new Name(host), new InetAddress[] {ip});
						if (first)
						{
							_staticHostsByAddr.put(PtrRecord.getReverseName(ip), host);
							first = false;
						}
					}
					
					
				}
				catch (Exception e)
				{
					LOG.warn("Error in parsing of etc host file", e);
				}

			}
		}
		catch (IOException e)
		{
			LOG.warn("Parse error in hosts file", e);
		}
		finally
		{
			if (br != null)
			{
				try
				{
					br.close();
				}
				catch (IOException e)
				{
					LOG.debug("", e);
				}
			}
		}
	}

	@ManagedAttribute("Prefer IPv6")
	public boolean isPreferIpv6()
	{
		return _preferIpv6;
	}

	public void setPreferIpv6(boolean preferIpv6)
	{
		_preferIpv6 = preferIpv6;
	}

	public Executor getExecutor()
	{
		return _executor;
	}

	public void setExecutor(Executor executor)
	{
		updateBean(_executor, executor);
		_executor = executor;
	}

}
