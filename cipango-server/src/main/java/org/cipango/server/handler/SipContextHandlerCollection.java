package org.cipango.server.handler;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.ar.SipApplicationRouter;
import javax.servlet.sip.ar.SipApplicationRouterInfo;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipRouteModifier;

import org.cipango.server.RequestCustomizer;
import org.cipango.server.SipConnector;
import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.ar.ApplicationRouterLoader;
import org.cipango.server.ar.RouterInfoUtil;
import org.cipango.server.session.SessionHandler;
import org.cipango.server.session.SessionManager;
import org.cipango.server.sipapp.SipAppContext;
import org.cipango.server.transaction.ServerTransaction;
import org.cipango.server.util.ExceptionUtil;
import org.cipango.sip.AddressImpl;
import org.cipango.sip.SipURIImpl;
import org.cipango.sip.URIFactory;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.ArrayUtil;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.component.Container;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.webapp.WebAppContext;

@ManagedObject("Sip context handler collection")
public class SipContextHandlerCollection extends AbstractSipHandler implements RequestCustomizer
{
	private static final Logger LOG = Log.getLogger(SipContextHandlerCollection.class);
	
	private SipAppContext[] _sipContexts;
	private HandlerCollection _webHandlerCollection;
	private SipApplicationRouter _applicationRouter;
	private BeanListener _beanListener = new BeanListener();
	private SipAppLifecycleListener _lifecycleListener = new SipAppLifecycleListener();
	 
	public SipContextHandlerCollection(@Name("contexts") HandlerCollection contexts)
	{		
		if (contexts != null)
			contexts.addBean(_beanListener, false);

		_webHandlerCollection = contexts;
	}
	

    @Override
	protected void doStart() throws Exception
	{
		super.doStart();
		
	    if (_applicationRouter == null)
			setApplicationRouter(ApplicationRouterLoader.loadApplicationRouter());
	    
	    _applicationRouter.init();
	    
	    List<String> appNames = new ArrayList<String>();
		SipAppContext[] contexts = getSipContexts();
		if (contexts != null)
		{
			for (SipAppContext context : contexts)
				if (isSipDeployed(context))
					appNames.add(context.getName());
		}
		_applicationRouter.applicationDeployed(appNames);
	}
    
    
	@Override
	protected void doStop() throws Exception
	{
		if (_applicationRouter != null)
			_applicationRouter.destroy();
		
		if (_webHandlerCollection != null)
			_webHandlerCollection.removeBean(this);
		super.doStop();
	}
	
	
	@Override
	public void handle(SipMessage message) throws IOException, ServletException
    {
		if (message.isRequest())
		{
			SipRequest request = (SipRequest) message;

			SipAppContext appContext = null;
						
			if (request.isInitial())
	        {
				SipApplicationRouterInfo routerInfo = null;
				Address route = request.getPoppedRoute();
				try
				{
					if (route != null)
					{
//	FIXME					SipURI uri = (SipURI) route.getURI();
//						if (RouterInfoUtil.ROUTER_INFO.equals(uri.getUser()))
//						{
//							routerInfo = RouterInfoUtil.decode(uri);
//							route = popLocalRoute(request);
//							if (route != null)
//								request.setPoppedRoute(route);
//						}
					}
					
					if (routerInfo == null)
					{
						routerInfo = getApplicationRouter().getNextApplication(
							request, null, SipApplicationRoutingDirective.NEW, null, null);
					}
				}
				catch (Throwable t) 
				{
					if (!request.isAck())
					{
						SipResponse response = new SipResponse(
								request,
								SipServletResponse.SC_SERVER_INTERNAL_ERROR,
								"Application router error: " + t.getMessage());
						ExceptionUtil.fillStackTrace(response, t);
						sendResponse(response);
					}
					return;
				}
				
				if (routerInfo != null && routerInfo.getNextApplicationName() != null)
				{
					boolean handle = handlingRoute(request, routerInfo);
					if (handle)
						return;
					
					request.setStateInfo(routerInfo.getStateInfo());
					request.setRegion(routerInfo.getRoutingRegion());
					
					String s = routerInfo.getSubscriberURI();
					if (s != null)
					{
						try
						{
							request.setSubscriberURI(URIFactory.parseURI(s));
						}
						catch (ParseException e)
						{
							LOG.debug(e);
						}
					}
					
					String applicationName = routerInfo.getNextApplicationName();
					appContext = (SipAppContext) getContext(applicationName);
															
					if (LOG.isDebugEnabled())
						LOG.debug("application router returned application {} for initial request {}", applicationName, request.getMethod());
					if (appContext == null && applicationName != null)
						LOG.debug("No application with name {} returned by application router could be found", applicationName, null);
				}
				
			}
			else if (_sipContexts != null)
			{			
				String contextId = getContextId(request);
				
				if (contextId != null)
					for(SipAppContext context : _sipContexts)
					{
						if (contextId.equals(context.getContextId()))
						{
							appContext = context;
							break;
						}
					}
			}

			if (appContext == null)
			{
				
				if (!request.isAck())
				{
					sendResponse(new SipResponse(request, SipServletResponse.SC_NOT_FOUND, null));
				}
				return;
			}
			appContext.handle(message);
		}		
    }
	
	private String getContextId(SipRequest request)
	{
		String appId = request.getParameter(SessionHandler.APP_ID);
		if (appId == null)
			appId = request.getToTag();
		
		if (appId != null)
		{
			int index = appId.indexOf(SessionManager.CONTEXT_ID_SEPARATOR);
			if (index != -1)
				return appId.substring(0, index);
		}
		return null;
	}
	
	// FIXME check following code. 
	private boolean handlingRoute(SipRequest request, SipApplicationRouterInfo routerInfo)
	{
		if (routerInfo.getRouteModifier() == null || SipRouteModifier.NO_ROUTE == routerInfo.getRouteModifier())
			return false;
		
		String[] routes = routerInfo.getRoutes();
		try
		{
			if (SipRouteModifier.ROUTE == routerInfo.getRouteModifier() && routes != null)
			{
				Address topRoute = new AddressImpl(routes[0]);
				if (getServer().isLocalURI(topRoute.getURI()))
					request.setPoppedRoute(topRoute);
				else
				{
					for (int i = routes.length; i >= 0; --i)
						request.pushRoute(new AddressImpl(routes[i]));
					request.send();
					return true;
				}
			}
			else if (SipRouteModifier.ROUTE_BACK == routerInfo.getRouteModifier() && routes != null)
			{
				SipConnector defaultConnector = getServer().getConnectors()[0];
    			SipURI ownRoute = new SipURIImpl(null, defaultConnector.getHost(), defaultConnector.getPort());
    			RouterInfoUtil.encode(ownRoute, routerInfo);

    			ownRoute.setLrParam(true);
				request.pushRoute(ownRoute);
				for (int i = routes.length; i >= 0; --i)
					request.pushRoute(new AddressImpl(routes[i]));
				request.send();
				return true;
			} 
			else if (routes == null 
					&& (SipRouteModifier.ROUTE_BACK == routerInfo.getRouteModifier() || SipRouteModifier.ROUTE == routerInfo.getRouteModifier()))
			{
				LOG.debug("Router info set route modifier to {} but no route provided, assume NO_ROUTE", routerInfo.getRouteModifier());
			}
			return false;
		}
		catch (Exception e)
		{
			if (!request.isAck())
			{
				// Could have ServletParseException or IllegalArgumentException on pushRoute
				SipResponse response = (SipResponse) request.createResponse(
	        			SipServletResponse.SC_SERVER_INTERNAL_ERROR,
	        			"Error in handler: " + e.getMessage());
				ExceptionUtil.fillStackTrace(response, e);
				try { sendResponse(response); } catch (Exception e1) {LOG.ignore(e1); }
			}
        	return true;
		}
	}
	
	private void sendResponse(SipResponse response)
	{
		if (response.to().getTag() == null)
			response.to().setParameter(AddressImpl.TAG, "123"); // FIXME tag
		ServerTransaction tx = (ServerTransaction) response.getTransaction();
		tx.send(response);
	}
	
	public SipAppContext getContext(String name)
	{
		if (_sipContexts != null)
		{
			for (int i = 0; i < _sipContexts.length; i++)
			{
				if (_sipContexts[i].getName().equals(name))	
					return _sipContexts[i];
			}
		}
		return null;
	}

	@ManagedAttribute(value="SIP contexts", readonly=true)
	public SipAppContext[] getSipContexts()
	{
		return _sipContexts;
	}

	public void addContext(SipAppContext context)
	{
		LOG.info("Add SIP context {}", context);
		setSipContexts(ArrayUtil.addToArray(getSipContexts(), context, SipAppContext.class));
		
		if (isStarted() && isSipDeployed(context))
			_applicationRouter.applicationDeployed(Arrays.asList(context.getName()));
		context.addLifeCycleListener(_lifecycleListener);
	}
	
	protected boolean isSipDeployed(SipAppContext context)
	{
		return context.isStarted() && context.hasSipServlets() /* FIXME && context.isAvailable() */;
	}
		
	public void removeContext(SipAppContext context)
	{
		LOG.info("Remove SIP context {}", context);
		setSipContexts(ArrayUtil.removeFromArray(getSipContexts(), context));
		if (isStarted() && context.hasSipServlets())
			_applicationRouter.applicationUndeployed(Arrays.asList(context.getName()));
		context.removeLifeCycleListener(_lifecycleListener);
		
		removeBean(context);
	}
	
	private void setSipContexts(SipAppContext[] sipContexts)
	{
		if (sipContexts!=null)
            for (SipAppContext handler:sipContexts)
            {
                if (handler.getServer()!=getServer())
                    handler.setServer(getServer());
            }   
		_sipContexts = sipContexts;
	}

	@ManagedAttribute(value="SIP application router", readonly=true)
	public SipApplicationRouter getApplicationRouter()
	{
		return _applicationRouter;
	}

	public void setApplicationRouter(SipApplicationRouter applicationRouter)
	{
		updateBean(_applicationRouter, applicationRouter);

		_applicationRouter = applicationRouter;
	}
	

	@Override
	public void customizeRequest(SipRequest request) throws IOException
	{
		if (!request.isInitial())
    		return;
    	
    	SipApplicationRouterInfo routerInfo = _applicationRouter.getNextApplication(
    			request,
    			request.getRegion(),
    			request.getRoutingDirective(),
    			null,
    			request.getStateInfo());
    	
    	if (routerInfo != null && routerInfo.getNextApplicationName() != null)
    	{
    		SipConnector connector = getServer().getConnectors()[0];
    		SipURI route = new SipURIImpl(null, connector.getHost(), connector.getPort());
    		RouterInfoUtil.encode(route, routerInfo);
    		route.setLrParam(true);
    		
    		request.pushRoute(route);
    	}
	}
	
	private class SipAppLifecycleListener implements LifeCycle.Listener
	{

		@Override
		public void lifeCycleStarting(LifeCycle event)
		{
		}

		@Override
		public void lifeCycleStarted(LifeCycle event)
		{
			SipAppContext context = ((SipAppContext) event);
			
			if (isSipDeployed(context))
			{
				_applicationRouter.applicationDeployed(Arrays.asList(context.getName()));
			}
			
			// Add bean here, to ensure that as context is already started, the 
			// MBean could have the right name.
			addBean(context);
			manage(context);
		}

		@Override
		public void lifeCycleFailure(LifeCycle event, Throwable cause)
		{
		}

		@Override
		public void lifeCycleStopping(LifeCycle event)
		{
		}

		@Override
		public void lifeCycleStopped(LifeCycle event)
		{
			SipAppContext context = ((SipAppContext) event);
			if (isStarted())
			{
				_applicationRouter.applicationUndeployed(Arrays.asList(context.getName()));
			}
		}
		
	}
	
	private class BeanListener implements Container.Listener
	{
		@Override
		public void beanAdded(Container parent, Object child)
		{
			if (child instanceof WebAppContext)
			{
				WebAppContext context = (WebAppContext) child;
				SipAppContext  appContext = context.getBean(SipAppContext.class);
				if (appContext != null)
					addContext(appContext);
			}	
		}

		@Override
		public void beanRemoved(Container parent, Object child)
		{
			if (child instanceof WebAppContext)
			{
				WebAppContext context = (WebAppContext) child;
				SipAppContext  appContext = context.getBean(SipAppContext.class);
				if (appContext != null)
					removeContext(appContext);
			}
		}
	}

	
}
