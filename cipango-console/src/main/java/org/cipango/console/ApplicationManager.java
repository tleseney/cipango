package org.cipango.console;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.cipango.console.data.Row;
import org.cipango.console.data.Row.Header;
import org.cipango.console.data.Row.Value;
import org.cipango.console.data.Table;
import org.cipango.console.menu.MenuImpl;
import org.cipango.console.util.Attributes;
import org.cipango.console.util.ObjectNameFactory;
import org.cipango.console.util.Parameters;
import org.cipango.console.util.PrinterUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class ApplicationManager
{
	private static Logger __logger = Log.getLogger("console");
	
	public static final ObjectName 
		DAR = ObjectNameFactory.create("org.cipango.dar:type=defaultapplicationrouter,id=0"),
		CONTEXT_DEPLOYER = ObjectNameFactory.create("org.cipango.deployer:type=contextdeployer,id=0"),
		SIP_APP_DEPLOYER = ObjectNameFactory.create("org.cipango.deployer:type=sipappdeployer,id=0");
	
	private static boolean isContext(ObjectName objectName, MBeanServerConnection mbsc) throws Exception
	{
		ObjectName[] contexts = PrinterUtil.getContexts(mbsc);
		for (ObjectName name : contexts)
			if (name.equals(objectName))
				return true;
		return false;
	}
	
	public static final Action START_APP = Action.add(new Action(MenuImpl.MAPPINGS, "start")
	{
		@Override
		public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
		{
			String name = request.getParameter(Parameters.OBJECT_NAME);
			ObjectName objectName = new ObjectName(name);
			if (isContext(objectName, mbsc))
			{
				mbsc.invoke(objectName, "start", null, null);
				String path = (String) mbsc.getAttribute(objectName, "contextPath");
				request.getSession().setAttribute(Attributes.INFO, "Application with context path " + path
						+ " sucessfully started");
			}
			else
				request.getSession().setAttribute(Attributes.WARN, "Could not found application");
		}	
		
	});
	
	public static final Action STOP_APP = Action.add(new Action(MenuImpl.MAPPINGS, "stop")
	{
		@Override
		public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
		{
			String name = request.getParameter(Parameters.OBJECT_NAME);
			ObjectName objectName = new ObjectName(name);
			if (isContext(objectName, mbsc))
			{
				mbsc.invoke(objectName, "stop", null, null);
				String path = (String) mbsc.getAttribute(objectName, "contextPath");
				request.getSession().setAttribute(Attributes.INFO, "Application with context path " + path
						+ " sucessfully stopped");
			}
			else
				request.getSession().setAttribute(Attributes.WARN, "Could not found application");
		}	
		
	});
	
	public static final Action DEPLOY_APP = Action.add(new Action(MenuImpl.MAPPINGS, "deploy")
	{
		@Override
		public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
		{
			if (ServletFileUpload.isMultipartContent(request))
			{
				FileItem item = null;
				try
				{
					ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
					@SuppressWarnings("unchecked")
					List<FileItem>  items = upload.parseRequest(request);
		
					Iterator<FileItem> it = items.iterator();
					while (it.hasNext())
					{
						item = it.next();
						if (!item.isFormField())
						{
							Deployer deployer = new Deployer(mbsc);
							deployer.deploy(item.getName(), item.get());
							request.getSession().setAttribute(Attributes.INFO,
									"Successful request to deploy " + item.getName());
							__logger.info("User " + request.getUserPrincipal() 
									+ " requested to deploy application: " + item.getName());
						}
					}
				}
				catch (Throwable e)
				{
					__logger.warn("Unable to deploy " + item.getName(), e);
					request.getSession().setAttribute(Attributes.WARN, "Unable to deploy "
							+ item.getName() + ": " + e.getMessage());
				}
			}
		}	
				
	});
	
	public static final Action UNDEPLOY_APP = Action.add(new Action(MenuImpl.MAPPINGS, "undeploy")
	{
		@Override
		public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
		{
			String name = request.getParameter(Parameters.OBJECT_NAME);
			ObjectName objectName = new ObjectName(name);
			if (isContext(objectName, mbsc))
			{
				Deployer deployer = new Deployer(mbsc);
				deployer.undeploy(objectName);	
				String path = (String) mbsc.getAttribute(objectName, "contextPath");
				request.getSession().setAttribute(Attributes.INFO, "Successfull request to undeploy application " + path);
				__logger.info("User " + request.getUserPrincipal() 
						+ " requested to undeploy application " + path);
			}
			else
				request.getSession().setAttribute(Attributes.WARN, "Could not found application");
		}	
		
	});
	
	private MBeanServerConnection _mbsc;
	
	public ApplicationManager(MBeanServerConnection mbsc)
	{
		_mbsc = mbsc;
	}
	
	public Table getSipContexts() throws Exception
	{
		ObjectName[] sipContexts = PrinterUtil.getSipAppContexts(_mbsc);
		
		Table contextsTable = new Table(_mbsc, sipContexts, "appContexts");
		
		ObjectName[] otherContexts = PrinterUtil.getNonSipAppContexts(_mbsc);
		if (otherContexts != null && otherContexts.length > 0)
		{
			Table otherContextsTable = new Table(_mbsc, otherContexts, "otherContexts");
			for (Row row : otherContextsTable)
			{
				int index = 0;
				for (Header header : contextsTable.getHeaders())
				{
					Value value = row.get(header);
					if (value == null)
						row.getValues().add(index, new Value("N/A", header));
					index++;
				}
				contextsTable.add(row);
			}
		}
		
		for (Row row : contextsTable)
		{
			boolean running = (Boolean) _mbsc.getAttribute(row.getObjectName(), "running");
			if (running)
				row.addOperation(new Operation(STOP_APP, row.getObjectName(), _mbsc));
			else
				row.addOperation(new Operation(START_APP, row.getObjectName(), _mbsc));
			
			row.addOperation(new Operation(UNDEPLOY_APP, row.getObjectName(), _mbsc));
		}
		return contextsTable;
	}
	
	public List<Context> getSipMappings() throws Exception
	{
		List<Context> l = new ArrayList<ApplicationManager.Context>();
		ObjectName[] sipContexts = PrinterUtil.getSipAppContexts(_mbsc);
		
		for (ObjectName objectName : sipContexts)
		{
			Context context = new Context((String) _mbsc.getAttribute(objectName, "name"));

			ObjectName servletHandler = (ObjectName) _mbsc.getAttribute(objectName, "servletHandler");
			ObjectName[] sipServletMappings = (ObjectName[]) _mbsc.getAttribute(servletHandler, "sipServletMappings");
	
			if (sipServletMappings != null && sipServletMappings.length != 0)
				context.setMappings(new Table(_mbsc, sipServletMappings, "mappings"));
			else
			{
				ObjectName mainServlet = (ObjectName) _mbsc.getAttribute(servletHandler, "mainServlet");
				if (mainServlet != null)
					context.setMainServlet((String) _mbsc.getAttribute(mainServlet, "name"));
			}
			l.add(context);
		}
		return l;
	}
	
	public boolean isDarRegistered() throws IOException
	{
		return _mbsc.isRegistered(DAR);
	}
	
	public String getDarUrl() throws Exception
	{
		return (String) _mbsc.getAttribute(DAR, "configuration");
	}
	
	public String getDarDefaultApplication() throws Exception
	{
		return (String) _mbsc.getAttribute(DAR, "defaultApplication");
	}
	
	public Table getDarConfig() throws Exception
	{
		Table table = new Table();
		table.setTitle("Configuration");
		List<Header> headers = new ArrayList<Row.Header>();
		headers.add(new Header("Method"));
		headers.add(new Header("Application name"));
		headers.add(new Header("Identity"));
		headers.add(new Header("Routing region"));
		headers.add(new Header("URI"));
		headers.add(new Header("Route modifier"));
		headers.add(new Header("State info"));
		table.setHeaders(headers);
		
		String config = (String) _mbsc.getAttribute(DAR, "config");
		InputStream is = new ByteArrayInputStream(config.getBytes());
		Properties properties = new Properties();
		properties.load(is);
		Enumeration<Object> e = properties.keys();
		while (e.hasMoreElements())
		{
			String method = e.nextElement().toString();
			String infos = properties.get(method).toString().trim();
			int li = infos.indexOf('(');
			while (li >= 0)
			{
				Row row = new Row();
				List<Value> values = row.getValues();
				Iterator<Header> headerIt = headers.iterator();
				int ri = infos.indexOf(')', li);
				if (ri < 0)
					throw new ParseException(infos, li);

				values.add(new Value(method, headerIt.next()));
				
				String info = infos.substring(li + 1, ri);
	
				li = infos.indexOf('(', ri);
				InfoIterator it = new InfoIterator(info);
				while (it.hasNext())
					values.add(new Value(it.next(), headerIt.next()));
				table.add(row);
			}
		}
		return table;
	}
		
	
	class InfoIterator implements Iterator<String>
	{
		private String _info;
		private int i;
		private String token;

		public InfoIterator(String info)
		{
			_info = info;
		}

		public boolean hasNext()
		{
			if (token == null)
			{
				int li = _info.indexOf('"', i);
				if (li != -1)
				{
					int ri = _info.indexOf('"', li + 1);
					if (ri != -1)
					{
						token = _info.substring(li + 1, ri);
						i = ri + 1;
					}
				}
			}
			return token != null;
		}

		public String next()
		{
			if (hasNext())
			{
				String s = token;
				token = null;
				return s;
			} else
			{
				return null;
			}
		}

		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}
	
	public static class Context
	{
		private String _name;
		private Table _mappings;
		private String _mainServlet;
		
		public Context(String name)
		{
			_name = name;
		}
		
		public String getName()
		{
			return _name;
		}

		public Table getMappings()
		{
			return _mappings;
		}

		public String getMainServlet()
		{
			return _mainServlet;
		}

		public void setMappings(Table mappings)
		{
			_mappings = mappings;
		}

		public void setMainServlet(String mainServlet)
		{
			_mainServlet = mainServlet;
		}		
	}

	public static class Operation
	{
		private Action _action;
		private ObjectName _objectName;
		private String _applicationName;
		
		public Operation(Action action, ObjectName objectName, MBeanServerConnection mbsc) throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, IOException
		{
			_action = action;
			_objectName = objectName;
			_applicationName = (String) mbsc.getAttribute(_objectName, "contextPath");
		}

		public Action getAction()
		{
			return _action;
		}

		public ObjectName getObjectName()
		{
			return _objectName;
		}

		public String getApplicationName()
		{
			return _applicationName;
		}
		
	}
}
