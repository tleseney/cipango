package org.cipango.kaleo.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cipango.kaleo.Resource;
import org.cipango.kaleo.AbstractResourceManager.ResourceHolder;
import org.cipango.kaleo.event.EventResource;
import org.cipango.kaleo.event.Subscription;
import org.cipango.kaleo.location.Binding;
import org.cipango.kaleo.location.LocationService;
import org.cipango.kaleo.location.Registration;
import org.cipango.kaleo.presence.PresenceEventPackage;
import org.cipango.kaleo.presence.policy.XcapPolicy;
import org.cipango.kaleo.presence.policy.PolicyManager;
import org.cipango.kaleo.presence.policy.XcapPolicyManager;
import org.cipango.kaleo.presence.watcherinfo.WatcherInfoEventPackage;
import org.eclipse.jetty.util.ajax.JSON;
import org.eclipse.jetty.util.ajax.JSON.Convertor;
import org.eclipse.jetty.util.ajax.JSON.Output;


public class APIServlet extends HttpServlet
{
	private PresenceEventPackage _presence;
	private LocationService _locationService;
	private XcapPolicyManager _xcapPolicyManager;
	private WatcherInfoEventPackage _watcherInfo;
	
	public void init()
	{
		_presence = (PresenceEventPackage) getServletContext().getAttribute(PresenceEventPackage.class.getName());
		_locationService = (LocationService) getServletContext().getAttribute(LocationService.class.getName());
		_xcapPolicyManager = (XcapPolicyManager) getServletContext().getAttribute(PolicyManager.class.getName());
		_watcherInfo = (WatcherInfoEventPackage) getServletContext().getAttribute(WatcherInfoEventPackage.class.getName());
		
		JSON.getDefault().addConvertor(Resource.class, new Convertor()
		{
			public void toJSON(Object obj, Output out) 
			{
				Resource resource = (Resource) obj;
				out.add("uri", resource.getUri());
			}
			public Object fromJSON(Map object)  { return null; }
		});
		JSON.getDefault().addConvertor(EventResource.class, new Convertor()
		{
			public void toJSON(Object obj, Output out) 
			{
				EventResource resource = (EventResource) obj;
				out.add("uri", resource.getUri());
				out.add("subscriptions", resource.getSubscriptions());
			}
			public Object fromJSON(Map object)  { return null; }
		});
		JSON.getDefault().addConvertor(Subscription.class, new Convertor()
		{
			public void toJSON(Object obj, Output out) 
			{
				Subscription subscription = (Subscription) obj;
				out.add("uri", subscription.getUri());
				out.add("state", subscription.getState().toString());
				out.add("authorized", subscription.isAuthorized());
			}
			public Object fromJSON(Map object)  { return null; }
		});
		
		JSON.getDefault().addConvertor(Registration.class, new Convertor()
		{
			public void toJSON(Object obj, Output out) 
			{
				Registration record = (Registration) obj;
				out.add("aor", record.getUri());
				out.add("bindings", record.getBindings());
			}
			public Object fromJSON(Map object)  { return null; }
		});
		JSON.getDefault().addConvertor(Binding.class, new Convertor()
		{
			public void toJSON(Object obj, Output out) 
			{
				Binding binding = (Binding) obj;
				out.add("contact", binding.getContact());
				out.add("expiration", new Date(binding.getExpirationTime()));
			}
			public Object fromJSON(Map object)  { return null; }
		});
		
		JSON.getDefault().addConvertor(XcapPolicy.class, new Convertor()
		{
			public void toJSON(Object obj, Output out) 
			{
				XcapPolicy policy = (XcapPolicy) obj;
				out.add("Resource", policy.getResourceUri());
				out.add("XcapResources", policy.getXcapResources());
			}
			public Object fromJSON(Map object)  { return null; }
		});
		
		JSON.getDefault().addConvertor(ResourceHolder.class, new Convertor()
		{
			public void toJSON(Object obj, Output out) 
			{
				ResourceHolder holder = (ResourceHolder) obj;
				out.add("lock", holder.getHoldCount());
				out.add("Owner", holder.getOwner());
				out.add("Resource", holder.getResource());
			}
			public Object fromJSON(Map object)  { return null; }
		});
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String pathInfo = request.getPathInfo();
		
		if (pathInfo == null)
			pathInfo = "";
		
		if (pathInfo.startsWith("/"))
			pathInfo = pathInfo.substring(1);
		
		String[] path = pathInfo.split("/");
		
		if ("registrations".equals(path[0]))
			response.getOutputStream().println(JSON.getDefault().toJSON( _locationService.getResources()));
		else if ("presentities".equals(path[0]))
			response.getOutputStream().println(JSON.getDefault().toJSON(_presence.getResources()));
		else if ("policies".equals(path[0]))
			response.getOutputStream().println(JSON.getDefault().toJSON(_xcapPolicyManager.getPolicies()));
		else if ("locks".equals(path[0]))
		{
			List<ResourceHolder> holders = new ArrayList<ResourceHolder>();
			holders.addAll(_locationService.getHolders());
			holders.addAll(_presence.getHolders());
			holders.addAll(_watcherInfo.getHolders());
			response.getOutputStream().println(JSON.getDefault().toJSON(holders));
		}
	}
}
