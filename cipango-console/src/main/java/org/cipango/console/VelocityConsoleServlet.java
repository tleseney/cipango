package org.cipango.console;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.VelocityLayoutServlet;
import org.cipango.console.JmxConnection.LocalConnection;
import org.cipango.console.menu.Menu;
import org.cipango.console.menu.MenuFactory;
import org.cipango.console.menu.MenuFactoryImpl;
import org.cipango.console.menu.Page;
import org.cipango.console.util.Attributes;
import org.cipango.console.util.Parameters;
import org.cipango.console.util.ReplaceTool;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class VelocityConsoleServlet extends VelocityLayoutServlet
{

	private Logger _logger = Log.getLogger("console");
	
	private Map<String, List<Action>> _actions = new HashMap<String, List<Action>>();
	
	private Map<String, JmxConnection> _jmxMap = new HashMap<String, JmxConnection>();
	
	// Use a separate map for statistic graph to ensure only one instance is created by connection
	private Map<String, StatisticGraph> _statisticGraphs = new HashMap<String, StatisticGraph>();
	private LocalConnection _localConnection;
	
	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		_localConnection = new JmxConnection.LocalConnection();
		
//	FIXME tmp
//		if (_localConnection.isConnectionValid())
//		{
//			try
//			{
//				StatisticGraph statisticGraph = new StatisticGraph(_localConnection);
//				statisticGraph.start();
//				_statisticGraphs.put(_localConnection.getId(), statisticGraph);
//			}
//			catch (Exception e)
//			{
//				_logger.warn("Failed to start statistic graph", e);
//			}
//		}

		_jmxMap.put(_localConnection.getId(), _localConnection);
		
		List<JmxConnection.RmiConnection> rmiConnections = JmxConnection.RmiConnection.getRmiConnections();
		for (JmxConnection.RmiConnection connection : rmiConnections)
		{
			try
			{
				_jmxMap.put(connection.getId(), connection);
				StatisticGraph statisticGraph = new StatisticGraph(connection);
				statisticGraph.start();
				_statisticGraphs.put(connection.getId(), statisticGraph);
			}
			catch (Exception e)
			{
				_logger.warn("Unable to add RMI connection: " + connection, e);
			}
			
		}
		
		if (getServletContext().getAttribute(MenuFactory.class.getName()) == null)
			getServletContext().setAttribute(MenuFactory.class.getName(), new MenuFactoryImpl());
	}
	
	public Map<String, Object> newJmxMap(JmxConnection connection)
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("replace", new ReplaceTool());
		map.put("connections", _jmxMap.values());
		map.put("jmxConnection", connection);
		
		MBeanServerConnection mbsc = connection.getMbsc();
		map.put("envManager", new EnvManager(mbsc));
		map.put("jettyManager", new JettyManager(mbsc));
		map.put("applicationManager", new ApplicationManager(mbsc));
		map.put("sipManager", new SipManager(mbsc));
		map.put("diameterManager", new DiameterManager(mbsc));
		map.put("snmpManager", new SnmpManager(mbsc));
		map.put("statisticGraph", _statisticGraphs.get(connection.getId()));
		return map;
	}
	
	@Override
	protected void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		JmxConnection connection = getConnection(request);
		if (!connection.isConnectionValid() && !_localConnection.isConnectionValid())
		{
			response.sendError(503 ,"JMX is not enabled, unable to use cipango console. Please start Cipango with:\n" +
			"\tjava -jar start.jar --ini=start-cipango.ini --pre=etc/cipango-jmx.xml");
			return;
		}
		
		HttpSession session = request.getSession();
		
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

		Menu menu = getMenuFactory().getMenu(command, connection.getMbsc());
		request.setAttribute(Attributes.MENU, menu);
		
		Page currentPage = menu.getCurrentPage();
		if (currentPage != null && request.getAttribute(Attributes.FATAL) == null)
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
	
	public JmxConnection getConnection(HttpServletRequest request)
	{
		JmxConnection connection = null;
		HttpSession session = request.getSession();
		
		if (request.getAttribute(Attributes.FATAL) != null)
			return _localConnection;
		
		String connectionId = request.getParameter(Parameters.JMX_CONNECTION);
		if (connectionId != null && !"".equals(connectionId))
		{
			connection = _jmxMap.get(connectionId);
			if (connection == null)
				fatal(request, "Coud not found RMI connection with ID " + connectionId);
			else
			{
				if (connection.isLocal())
				{
					if (session.getAttribute(JmxConnection.class.getName()) != null)
						session.setAttribute(Attributes.INFO, "Connected to local JVM");
				}
				else
					session.setAttribute(Attributes.INFO, "Connected to remote server: " + connection);

				session.setAttribute(JmxConnection.class.getName(), connection);
			}
		}
		if (connection == null)
			connection = (JmxConnection) session.getAttribute(JmxConnection.class.getName());
		if (connection == null)
		{
			if (_jmxMap.size() > 1)
				session.setAttribute(Attributes.INFO, "Connected to local JVM");
			connection = _localConnection;
			session.setAttribute(JmxConnection.class.getName(), connection);
		}
		
		try
		{
			session.setAttribute(MBeanServerConnection.class.getName(), connection.getMbsc());
		}
		catch (Exception e)
		{
			fatal(request, "Unable to connect to remote server: " + connection);
			connection = _localConnection;
		}

		return connection;
	}
	
	public void fatal(HttpServletRequest request, String message)
	{
		_logger.warn(message);
		request.getSession().removeAttribute(Attributes.INFO);
		request.setAttribute(Attributes.FATAL, message);
	}
		
	@Override
	protected void fillContext(Context context, HttpServletRequest request)
	{
		super.fillContext(context, request);
		
		JmxConnection connection = getConnection(request);
		
		HttpSession session = request.getSession();
		session.setAttribute(StatisticGraph.class.getName(), _statisticGraphs.get(connection.getId()));
		
		if (connection.getContextMap() == null)
			connection.setContextMap(newJmxMap(connection));
		
		for (Map.Entry<String, Object> entry: connection.getContextMap().entrySet())
			context.put(entry.getKey(), entry.getValue());

	}
	
	public MenuFactory getMenuFactory()
	{
		return (MenuFactory) getServletContext().getAttribute(MenuFactory.class.getName());
	}

	@Override
	public void destroy()
	{	
		for (StatisticGraph statisticGraph : _statisticGraphs.values())
			statisticGraph.stop();
	}

	@Override
	protected Template getTemplate(HttpServletRequest request, HttpServletResponse response)
	{
		if (request.getAttribute(Attributes.FATAL) != null)
			return getTemplate("components/Fatal.vm");
		return super.getTemplate(request, response);
	}

	
}
