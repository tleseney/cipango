// ========================================================================
// Copyright 2008-2012 NEXCOM Systems
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

package org.cipango.dar;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.ar.SipApplicationRouter;
import javax.servlet.sip.ar.SipApplicationRouterInfo;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;
import javax.servlet.sip.ar.SipRouteModifier;
import javax.servlet.sip.ar.SipTargetedRequestInfo;

import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.Dumpable;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Default Application Router. 
 * Looks for its configuration from the property javax.servlet.sip.ar.dar.configuration
 * or etc/dar.properties if not defined. 
 */
@ManagedObject("Default application router")
public class DefaultApplicationRouter implements SipApplicationRouter, Dumpable
{
	private static final Logger LOG = Log.getLogger(DefaultApplicationRouter.class);
	
	public static final String __J_S_DAR_CONFIGURATION = "javax.servlet.sip.ar.dar.configuration";
	
	public static final String ROUTE_OUTGOING_REQUESTS = "org.cipango.dar.routeOutgoingRequests";
	public static final String DEFAULT_CONFIGURATION = "etc/dar.properties";

	private Map<String, RouterInfo[]> _routerInfoMap;
	private String _configuration;
	private SortedSet<String> _applicationNames = new TreeSet<String>();
	
	private boolean _routeOutgoingRequests = true;
	
	public void setConfiguration(String configuration)
	{
		_configuration = configuration;
	}
	
	@ManagedAttribute("Configuration URI")
	public String getConfiguration()
	{
		return _configuration;
	}
	
	public void setRouteOutgoingRequests(boolean b)
	{
		_routeOutgoingRequests = b;
	}

	@ManagedAttribute(value="Route new outgoing requests", readonly=true)
	public boolean getRouteOutgoingRequests()
	{
		return _routeOutgoingRequests;
	}
	
	@ManagedAttribute("Application names")
	public String[] getApplicationNames()
	{
		return _applicationNames.toArray(new String[] {});
	}
	
	public void applicationDeployed(List<String> newlyDeployedApplicationNames)
	{
		_applicationNames.addAll(newlyDeployedApplicationNames);
		init();
	}

	public void applicationUndeployed(List<String> undeployedApplicationNames)
	{
		_applicationNames.removeAll(undeployedApplicationNames);
		init();
	}

	public void destroy()
	{
	}

	public SipApplicationRouterInfo getNextApplication(SipServletRequest initialRequest,
			SipApplicationRoutingRegion region, SipApplicationRoutingDirective directive, SipTargetedRequestInfo toto, Serializable stateInfo)
	{
		if (!_routeOutgoingRequests && initialRequest.getRemoteAddr() == null)
			return null;
		
		if (_routerInfoMap == null || _routerInfoMap.isEmpty())
		{
			if (stateInfo != null || _applicationNames.isEmpty() || directive != SipApplicationRoutingDirective.NEW)
				return null;
			
			return new SipApplicationRouterInfo(_applicationNames.first(), 
					SipApplicationRoutingRegion.NEUTRAL_REGION, 
					initialRequest.getFrom().getURI().toString(), 
					null,
					SipRouteModifier.NO_ROUTE, 
					1);
		}
		
		String method = initialRequest.getMethod();
		RouterInfo[] infos = _routerInfoMap.get(method.toUpperCase());

		if (infos == null)
			return null;
		
		int index = 0;
		if (stateInfo != null)
			index = (Integer) stateInfo;

		if (index >= 0 && index < infos.length)
		{
			RouterInfo info = infos[index];
			
			String identity = info.getIdentity();
			if (identity.startsWith("DAR:"))
			{
				try
				{
					identity = initialRequest.getAddressHeader(identity.substring("DAR:".length())).getURI().toString();
				}
				catch (Exception e)
				{
					LOG.debug("Failed to parse router info identity: " + info.getIdentity(), e);
				}
			}
			
			return new SipApplicationRouterInfo(info.getName(), info.getRegion(), identity, null,
					SipRouteModifier.NO_ROUTE, index + 1);
		}

		return null;
	}
	
	@ManagedAttribute("default application when no DAR configuration has been found")
	public String getDefaultApplication()
	{
		if ((_routerInfoMap == null || _routerInfoMap.isEmpty()) && !_applicationNames.isEmpty())
			return _applicationNames.first();
		return null;
	}

	public void setRouterInfos(Map<String, RouterInfo[]> infoMap)
	{
		_routerInfoMap = infoMap;
	}
	
	public Map<String, RouterInfo[]> getRouterInfos()
	{
		return _routerInfoMap;
	}
	
	@ManagedAttribute("Configuration")
	public String getConfig()
	{
		if (_routerInfoMap == null)
			return null;
		
		StringBuilder sb = new StringBuilder();
		Iterator<String> it = _routerInfoMap.keySet().iterator();
		while (it.hasNext())
		{
			
			String method = (String) it.next();
			RouterInfo[] routerInfos = _routerInfoMap.get(method);
			sb.append(method).append(": ");
			for (int i = 0; routerInfos != null && i < routerInfos.length; i++)
			{
				RouterInfo routerInfo = routerInfos[i];
				sb.append('(');
				sb.append('"').append(routerInfo.getName()).append("\", ");
				sb.append('"').append(routerInfo.getIdentity()).append("\", ");
				sb.append('"').append(routerInfo.getRegion().getType()).append("\", ");
				sb.append('"').append(routerInfo.getUri()).append("\", ");
				sb.append('"').append(routerInfo.getRouteModifier()).append("\", ");
				sb.append('"').append(i).append('"');
				sb.append(')');
				if (i + 1 < routerInfos.length)
					sb.append(", ");
			}
			sb.append('\n');
		}
		return sb.toString();
	}
	
	public RouterInfo[] getRouterInfo(String key)
	{
		return _routerInfoMap.get(key);
	}

	@ManagedOperation(value="Init", impact="ACTION")
	public void init() 
	{
		if (!System.getProperty(ROUTE_OUTGOING_REQUESTS, "true").equalsIgnoreCase("true"))
			_routeOutgoingRequests = false;
		
		if (_configuration == null)
		{
			String configuration = System.getProperty(__J_S_DAR_CONFIGURATION);
		
			if (configuration != null)
			{
				_configuration = configuration;
			}
			else if (System.getProperty("jetty.home") != null)
			{
				File home = new File(System.getProperty("jetty.home"));
				_configuration = new File(home, DEFAULT_CONFIGURATION).toURI().toString();
			}
			
			if (_configuration == null)
				_configuration = DEFAULT_CONFIGURATION;
		}
		
		try
		{
			DARConfiguration config = new DARConfiguration(new URI(_configuration));
			config.configure(this);
		}
		catch (Exception e)
		{
			LOG.debug("DAR configuration error: " + e);
		}
		
		
		if ((_routerInfoMap == null || _routerInfoMap.isEmpty()) && !_applicationNames.isEmpty())
			LOG.info("No DAR configuration. Using application: " + _applicationNames.first());
	}
	
	public void init(Properties properties)
	{
		init();
	}

	public String dump()
	{
		return ContainerLifeCycle.dump(this);
	}

	public void dump(Appendable out, String indent) throws IOException
	{
		out.append("DefaultApplicationRouter ");
		if (_routerInfoMap == null || _routerInfoMap.isEmpty())
		{
			if (!_applicationNames.isEmpty())
				out.append("default application: ").append(getDefaultApplication());
			else
				out.append("No applications");
		}
		else
		{
			out.append("\n");
			List<Dumpable> l = new ArrayList<Dumpable>();
			Iterator<String> it = _routerInfoMap.keySet().iterator();
			while (it.hasNext())
				l.add(new DumpableMethod(it.next()));
			ContainerLifeCycle.dump(out,indent, l);
		}
			
	}
		
	public class DumpableMethod implements Dumpable
	{
		private String _method;
		
		public DumpableMethod(String method)
		{
			_method = method;
		}
		
		public String dump()
		{
			return ContainerLifeCycle.dump(this);
		}
	
		public void dump(Appendable out, String indent) throws IOException
		{
			out.append(_method).append("\n");
			ContainerLifeCycle.dump(out,indent, Arrays.asList(_routerInfoMap.get(_method)));
		}
		
	}
}

