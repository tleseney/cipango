package org.cipango.console;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public abstract class JmxConnection
{
	
	private String _displayName;
	private String _id;
	protected MBeanServerConnection _mbsc;
	private Logger _logger = Log.getLogger("console");
	private Map<String, Object> _contextMap;

	public synchronized MBeanServerConnection getMbsc()
	{
		if (!isConnectionValid())
		{
			try
			{
				initJmxConnection();
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		
		return _mbsc;
	}
	
	public synchronized boolean isConnectionValid()
	{
		if (_mbsc == null)
			return false;
		try 
		{
			// This call throw an exception if the remote server has been restarted.
			_mbsc.isRegistered(JettyManager.SERVER);
			return true;
		}
		catch (Exception e) 
		{
			_logger.info("Need to reinitialize RMI connection");
			_mbsc = null;
			_contextMap = null;
			return false;
		}
	}
	
	
	protected abstract void initJmxConnection() throws Exception;

	public String toString()
	{
		return getDisplayName();
	}


	public String getDisplayName()
	{
		if (_displayName != null)
			return _displayName;
		return _id;
	}

	public void setDisplayName(String displayName)
	{
		_displayName = displayName;
	}

	public String getId()
	{
		return _id;
	}

	protected void setId(String id)
	{
		_id = id;
	}
	
	public boolean equals(Object obj)
	{
		return obj instanceof JmxConnection && this.hashCode() == obj.hashCode();
	}

	public int hashCode()
	{
		return _id.hashCode();
	}
	

	public Map<String, Object> getContextMap()
	{
		return _contextMap;
	}

	public void setContextMap(Map<String, Object> contextMap)
	{
		_contextMap = contextMap;
	}
	
	public abstract boolean isLocal();

	public static class LocalConnection extends JmxConnection
	{
		private Logger _logger = Log.getLogger("console");
		
		public LocalConnection() throws IllegalStateException
		{
			setId("local");
			setDisplayName("Local JVM");
			initJmxConnection();
		}
		
		@Override
		protected void initJmxConnection()
		{
			List<MBeanServer> l = MBeanServerFactory.findMBeanServer(null);
			Iterator<MBeanServer> it = l.iterator();
			while (it.hasNext())
			{
				MBeanServer server = it.next();
				for (int j = 0; j < server.getDomains().length; j++)
				{
					if (server.isRegistered(JettyManager.SERVER))
					{
						_mbsc = server;
						break;
					}
				}
			}
			_logger.debug("Use MBeanServerConnection {}", _mbsc, null);
		}

		@Override
		public boolean isLocal()
		{
			return true;
		}
		
	}
	
	public static class RmiConnection extends JmxConnection
	{

		private static final String RMI_HOST = "cipango.console.rmi.host";
		private static final String RMI_PORT = "cipango.console.rmi.port";
		private static final String RMI_NAME = "cipango.console.rmi.name";
		
		private int _port;
		private String _host;
		private Map<String, Object> _environment;
		private Logger _logger = Log.getLogger("console");
		
		public static List<RmiConnection> getRmiConnections()
		{
			List<RmiConnection> l = new ArrayList<RmiConnection>();
			int i = 1;
			
			while (true)
			{
				String host = System.getProperty(RMI_HOST + "." + i);
				String port = System.getProperty(RMI_PORT + "." + i);
				String name = System.getProperty(RMI_NAME + "." + i);
				if (host != null && port != null && !port.trim().equals("0"))
				{
					RmiConnection connection = new RmiConnection(host, port, name, null, null);
					l.add(connection);
				}
				else
					break;
				i++;
			}

			return l;
		}
		
		public RmiConnection(String host, String sPort, String displayName, String usr, String pwd)
		{

			_host = host;
			try
			{
				_port = Integer.parseInt(sPort);
			}
			catch (NumberFormatException e)
			{
				throw new IllegalArgumentException("Invalid port value: " + sPort
						+ " is not a number");
			}
			if (_host == null || _host.equals(""))
			{
				throw new IllegalArgumentException("The host can not be null");
			}
			if (_port > 65536 || _port <= 0)
			{
				throw new IllegalArgumentException("Invalid port value: should be between 0 and 65536");
			}
			setId(_host + ":" + _port);
			setDisplayName(displayName);

			if (usr != null && pwd != null)
			{
				String[] credentials = new String[2];
				credentials[0] = usr;
				credentials[1] = pwd;
				_environment = new HashMap<String, Object>();
				_environment.put(JMXConnector.CREDENTIALS, credentials);
			}
		}
		
		protected void initJmxConnection() throws SecurityException, MalformedURLException, IOException
		{
			_logger.debug("Try to get MBeanServerConnection for connection " + toString());
			try
			{
				if (_mbsc == null)
				{
					JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + _host
							+ ":" + _port + "/jmxrmi");
					JMXConnector jmxc = JMXConnectorFactory.connect(url, _environment);
					_mbsc = jmxc.getMBeanServerConnection();
				}

				_logger.debug("Got MBeanServerConnection " + _mbsc + " for connection " + toString());
			}
			catch (SecurityException e)
			{
				_logger.warn("Unable to connect to " + getId() + " due to invalid login/password: "
						+ e.getMessage());
				throw e;
			}
		}


		public String getHost()
		{
			return _host;
		}

		public int getPort()
		{
			return _port;
		}

		@Override
		public boolean isLocal()
		{
			return false;
		}
		
	}
}
