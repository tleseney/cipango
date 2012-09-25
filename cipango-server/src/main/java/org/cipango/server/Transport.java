package org.cipango.server;

public enum Transport 
{
	UDP("UDP", 5060, false, false, "SIP+D2U"),
	TCP("TCP", 5060, true, false, "SIP+D2T"),
	TLS("TLS", 5061, true, true, "SIPS+D2T");
	
	private String _name;
	private int _defaultPort;
	private boolean _reliable;
	private boolean _secure;
	private String _service;
	
	Transport(String name, int defaultPort, boolean reliable, boolean secure, String service)
	{
		_name = name;
		_defaultPort = defaultPort;
		_service = service;
		_reliable = reliable;
		_secure = secure;
	}
	
	public boolean isReliable()
	{
		return _reliable;
	}
	
	public boolean isSecure()
	{
		return _secure;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public String getService()
	{
		return _service;
	}
	
	public int getDefaultPort()
	{
		return _defaultPort;
	}
	
	public String toString()
	{
		return _name;
	}
}
