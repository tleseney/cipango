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

import java.net.InetAddress;

import org.cipango.server.Transport;

public class Hop implements Comparable<Hop>
{
	private String _host;
	private int _port = -1;
	private Transport _transport;
	private boolean _secure;
	private InetAddress _address;
	private int _weight = 1;
	private int _priority = 100;
	private int _preference;
	
	public String getHost()
	{
		return _host;
	}
	public void setHost(String host)
	{
		_host = host;
	}
	public int getPort()
	{
		return _port;
	}
	public void setPort(int port)
	{
		_port = port;
	}
	public Transport getTransport()
	{
		return _transport;
	}
	public void setTransport(Transport transport)
	{
		_transport = transport;
	}
	
	public boolean isPortSet()
	{
		return _port > 0;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(_host);
		if (_port != -1)
			sb.append(":").append(_port);
		if (_transport != null)
			sb.append("/").append(_transport);
		return sb.toString();
	}
	public boolean isSecure()
	{
		return _secure;
	}
	public void setSecure(boolean secure)
	{
		_secure = secure;
	}
	public InetAddress getAddress()
	{
		return _address;
	}
	public void setAddress(InetAddress address)
	{
		_address = address;
	}
	public int getWeight() {
		return _weight;
	}
	public void setWeight(int weight) {
		_weight = weight;
	}
	public int getPriority() {
		return _priority;
	}
	public void setPriority(int priority) {
		_priority = priority;
	}
	public int getPreference() {
		return _preference;
	}
	public void setPreference(int preference) {
		_preference = preference;
	}
	
	@Override
	public int compareTo(Hop o)
	{
		int pref = getPreference() - o.getPreference();
		if (pref != 0)
			return pref;
		int priority = getPriority() - o.getPriority();
		if (priority != 0)
			return priority;
		int weight = getWeight() - o.getWeight();
		if (weight != 0)
			return weight;
		int transport = getTransport().ordinal() - o.getTransport().ordinal();
		if (transport != 0) // Prefer UDP
			return transport;
		return hashCode() - o.hashCode();
	}
}
