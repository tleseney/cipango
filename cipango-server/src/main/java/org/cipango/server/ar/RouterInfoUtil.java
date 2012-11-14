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

package org.cipango.server.ar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.servlet.sip.SipURI;
import javax.servlet.sip.ar.SipApplicationRouterInfo;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;
import javax.servlet.sip.ar.SipRouteModifier;

import org.eclipse.jetty.util.TypeUtil;

public class RouterInfoUtil 
{
	public static final String ROUTER_INFO = "X-Cipango-Router-Info";
	private static final String NEXT_APPLICATION_NAME = "appName";
	private static final String STATE_INFO = "stateInfo";
	private static final String SUBSCRIBER_URI = "subUri";
	private static final String ROUTING_REGION = "region";
	private static final String ROUTES = "routes";
	private static final String ROUTE_MODIFIER = "route-modifier";
	
	public static void encode(SipURI uri, SipApplicationRouterInfo routerInfo) throws IOException
	{
		uri.setUser(ROUTER_INFO);
		
		uri.setParameter(NEXT_APPLICATION_NAME, routerInfo.getNextApplicationName());
		setParameter(uri, ROUTING_REGION, routerInfo.getRoutingRegion());
		setParameter(uri, SUBSCRIBER_URI, routerInfo.getSubscriberURI());
		setParameter(uri, ROUTES, routerInfo.getRoutes());
		if (routerInfo.getRouteModifier() != null)
			uri.setParameter(ROUTE_MODIFIER, routerInfo.getRouteModifier().toString());
		setParameter(uri, STATE_INFO, routerInfo.getStateInfo());
		
	}
	
	private static void setParameter(SipURI uri, String name, String value)
	{
		if (value != null)
			uri.setParameter(name, value);
	}
	
	private static void setParameter(SipURI uri, String name, Serializable value) throws IOException
	{
		if (value != null)
		{
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bout);
			out.writeObject(value);
			uri.setParameter(name, TypeUtil.toHexString(bout.toByteArray()));
		}
	}
	
	private static Serializable getSerializable(SipURI uri, String name) throws IOException, ClassNotFoundException
	{
		String s = uri.getParameter(name);
		if (s != null)
		{
			byte[] b = TypeUtil.fromHexString(s);
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(b));
			return (Serializable) in.readObject();
		}
		return null;
	}
	
	public static SipApplicationRouterInfo decode(SipURI uri) throws Exception
	{
		String appName = uri.getParameter(NEXT_APPLICATION_NAME);
		String sRouteModifier = uri.getParameter(ROUTE_MODIFIER);
		SipRouteModifier routeModifier = null;
		if (sRouteModifier != null)
			routeModifier = SipRouteModifier.valueOf(sRouteModifier);
		
		return new SipApplicationRouterInfo(
				appName,
				(SipApplicationRoutingRegion) getSerializable(uri, ROUTING_REGION), 
				uri.getParameter(SUBSCRIBER_URI),
				(String[]) getSerializable(uri, ROUTES),
				routeModifier, 
				getSerializable(uri, STATE_INFO));
	}
}
