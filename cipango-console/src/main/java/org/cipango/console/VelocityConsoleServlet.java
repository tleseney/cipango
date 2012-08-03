package org.cipango.console;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.VelocityLayoutServlet;
import org.cipango.console.menu.Menu;
import org.cipango.console.menu.MenuFactory;
import org.cipango.console.menu.MenuFactoryImpl;
import org.cipango.console.menu.Page;
import org.cipango.console.util.ObjectNameFactory;
import org.cipango.console.util.Parameters;
import org.cipango.console.util.ReplaceTool;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class VelocityConsoleServlet extends VelocityLayoutServlet
{

	public static final ObjectName 
	CONNECTOR_MANAGER = ObjectNameFactory.create("org.cipango.server:type=connectormanager,id=0"),
	CONTEXT_DEPLOYER = ObjectNameFactory.create("org.cipango.deployer:type=contextdeployer,id=0"),
	DAR = ObjectNameFactory.create("org.cipango.dar:type=defaultapplicationrouter,id=0"),
	DIAMETER_NODE = ObjectNameFactory.create("org.cipango.diameter.node:type=node,id=0"),
	DIAMETER_PEERS = ObjectNameFactory.create("org.cipango.diameter.node:type=peer,*"),
	DIAMETER_FILE_LOG = ObjectNameFactory.create("org.cipango.diameter.log:type=filemessagelogger,id=0"),
	DIAMETER_CONSOLE_LOG = ObjectNameFactory.create("org.cipango.callflow.diameter:type=jmxmessagelogger,id=0"),
	HTTP_LOG = ObjectNameFactory.create("org.eclipse.jetty:type=ncsarequestlog,id=0"),
	SIP_APP_DEPLOYER = ObjectNameFactory.create("org.cipango.deployer:type=sipappdeployer,id=0"),
	SIP_CONSOLE_MSG_LOG = ObjectNameFactory.create("org.cipango.callflow:type=jmxmessagelog,id=0"),
	SERVER = ObjectNameFactory.create("org.cipango.server:type=server,id=0"), 
	SNMP_AGENT = ObjectNameFactory.create("org.cipango.snmp:type=snmpagent,id=0"), 
	SIP_MESSAGE_LOG = ObjectNameFactory.create("org.cipango.server.log:type=filemessagelog,id=0"),
	TRANSACTION_MANAGER = ObjectNameFactory.create("org.cipango.server.transaction:type=transactionmanager,id=0");
	
	private Logger _logger = Log.getLogger("console");
	private MBeanServerConnection _mbsc;
	private EnvManager _envManager;
	private JettyManager _jettyManager;
	private ApplicationManager _applicationManager;
	private SipManager _sipManager;
	private DiameterManager _diameterManager;
	private SnmpManager _snmpManager;
	private StatisticGraph _statisticGraph;
	private ReplaceTool _replaceTool = new ReplaceTool();
	private Map<String, List<Action>> _actions = new HashMap<String, List<Action>>();
	
	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		initConnection();
		_envManager = new EnvManager(_mbsc);
		_jettyManager = new JettyManager(_mbsc);
		_applicationManager = new ApplicationManager(_mbsc);
		_sipManager = new SipManager(_mbsc);
		_diameterManager = new DiameterManager(_mbsc);
		_snmpManager = new SnmpManager(_mbsc);
		
		if (isJmxEnabled())
		{
			try
			{
				_statisticGraph = new StatisticGraph(_mbsc);
				_statisticGraph.start();
				getServletContext().setAttribute(StatisticGraph.class.getName(), _statisticGraph);
			}
			catch (Exception e)
			{
				_logger.warn("Failed to start statistic graph", e);
			}
		}
		if (getServletContext().getAttribute(MenuFactory.class.getName()) == null)
			getServletContext().setAttribute(MenuFactory.class.getName(), new MenuFactoryImpl(_mbsc));
	}
	
	private void initConnection() throws ServletException
	{
		try
		{
			List<MBeanServer> l = MBeanServerFactory.findMBeanServer(null);
			Iterator<MBeanServer> it = l.iterator();
			while (it.hasNext())
			{
				MBeanServer server = it.next();
				for (int j = 0; j < server.getDomains().length; j++)
				{
					if (server.isRegistered(SERVER))
					{
						_mbsc = server;
						break;
					}
				}
			}
			_logger.debug("Use MBeanServerConnection {}", _mbsc, null);
		}
		catch (Throwable t)
		{
			_logger.warn("Unable to get MBeanServer", t);
			throw new IllegalStateException("Unable to get MBeanServer", t);
		}
	}
	
	@Override
	protected void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		HttpSession session = request.getSession();
		session.setAttribute(MBeanServerConnection.class.getName(), _mbsc);
		session.setAttribute(StatisticGraph.class.getName(), _statisticGraph);
		
		Principal principal = request.getUserPrincipal();
		if (principal != null && !principal.equals(session.getAttribute(Principal.class.getName())))
		{		
			_logger.info("User " + principal.getName() + " has logged in console");
			session.setAttribute(Principal.class.getName(), principal);
		}
		
		String command = request.getServletPath();
		int index = command.lastIndexOf('/');
		if (index != -1)
			command = command.substring(index + 1);

		Menu menu = getMenuFactory().getMenu(command);
		request.setAttribute("menu", menu);
		
		Page currentPage = menu.getCurrentPage();
		if (currentPage != null)
		{
			registerActions();
			Action action = getAction(currentPage, request);
			if (action != null)
			{
				action.process(request);
				if (action.isAjax(request))
				{
					action.setAjaxContent(request, response);
				}
				else
					response.sendRedirect(command);

				return;
			} 
		}
		super.doRequest(request, response);
	}
	
	protected void registerActions()
	{
		synchronized (Action.ACTIONS)
		{
			for (Action action : Action.ACTIONS)
				registerAction(action);
			Action.ACTIONS.clear();
		}
	}
	
	public void registerAction(Action action)
	{
		List<Action> list = _actions.get(action.getPage().getName());
		if (list == null)
		{
			list = new ArrayList<Action>();
			_actions.put(action.getPage().getName(), list);
		}
		list.add(action);
	}
	
	protected Action getAction(Page page, HttpServletRequest request)
	{
		String param = request.getParameter(Parameters.ACTION);
		if (param == null || page == null)
			return null;
		
		List<Action> list = _actions.get(page.getName());
		if (list != null)
		{
			for (Action action : list)
				if (action.getParameter().equalsIgnoreCase(param))
					return action;
		}
		return null;

	}
	
	public boolean isJmxEnabled()
	{
		return _mbsc != null;
	}
		
	@Override
	protected void fillContext(Context context, HttpServletRequest request)
	{
		super.fillContext(context, request);
		
		context.put("envManager", _envManager);
		context.put("jettyManager", _jettyManager);
		context.put("applicationManager", _applicationManager);
		context.put("sipManager", _sipManager);
		context.put("diameterManager", _diameterManager);
		context.put("snmpManager", _snmpManager);
		context.put("statisticGraph", _statisticGraph);
		context.put("replace", _replaceTool);
	}
	
	public MenuFactory getMenuFactory()
	{
		return (MenuFactory) getServletContext().getAttribute(MenuFactory.class.getName());
	}

	@Override
	public void destroy()
	{	
		if (_statisticGraph != null)
			_statisticGraph.stop();
	}

}
