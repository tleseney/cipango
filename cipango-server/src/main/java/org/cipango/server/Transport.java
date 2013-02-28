package org.cipango.server;

public enum Transport 
{
	UDP("UDP", 5060, false, false, "SIP+D2U", "_sip._udp."),
	TCP("TCP", 5060, true, false, "SIP+D2T", "_sip._tcp."),
	TLS("TLS", 5061, true, true, "SIPS+D2T", "_sips._tcp."),
	WS("WS", 80, true, false, "SIP+D2W", "_sip._tcp."), // FIXME srv prefix
	WSS("WSS", 443, true, true, "SIPS+D2W ", "_sips._tcp."); // FIXME srv prefix
	
	private String _name;
	private int _defaultPort;
	private boolean _reliable;
	private boolean _secure;
	private String _service;
	private String _srvPrefix;
	
	Transport(String name, int defaultPort, boolean reliable, boolean secure, String service, String srvPrefix)
	{
		_name = name;
		_defaultPort = defaultPort;
		_service = service;
		_reliable = reliable;
		_secure = secure;
		_srvPrefix = srvPrefix;
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
	
	public String getSrvPrefix()
	{
		return _srvPrefix;
	}
	
	public String toString()
	{
		return _name;
	}

}
