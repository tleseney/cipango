package org.cipango.console;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ReflectionException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.cipango.console.data.ConsoleLogger;
import org.cipango.console.util.ConsoleUtil;
import org.cipango.console.util.Parameters;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.w3c.dom.Node;

public class SvgServlet extends HttpServlet
{
	private Logger _logger = Log.getLogger(SvgServlet.class);
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		try
		{
			MBeanServerConnection mbsc = (MBeanServerConnection) request.getSession().getAttribute(MBeanServerConnection.class.getName());
			if (mbsc == null)
			{
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
			
			response.setContentType("image/svg+xml");
			int maxMessages = 
				ConsoleUtil.getParamValueAsInt(Parameters.MAX_MESSAGES, request, ConsoleLogger.DEFAULT_MAX_MESSAGES);
			
			String msgFilter =request.getParameter(Parameters.MESSAGE_FILTER);
			if (mbsc.isRegistered(SipManager.CONSOLE_LOGGER))
			{
				String userAgent = request.getHeader("User-Agent");
				
				// Firefox does not support animation and IE does not support foreignObject
				boolean supportAnimation = (userAgent.indexOf("Firefox") == -1 
					&& userAgent.indexOf("MSIE") == -1 )
					|| "Chrome".equalsIgnoreCase(request.getParameter("ua"));
				
				Object[] params = {new Integer(maxMessages), msgFilter, "dataToSvg.xsl", supportAnimation};
				byte[] image = (byte[]) mbsc.invoke(
						SipManager.CONSOLE_LOGGER, 
						"generateGraph", 
						params,
						new String[] {Integer.class.getName(), String.class.getName(), String.class.getName(), Boolean.class.getName()});
				
				// Only Internet explorer does NOT applies XSL on a XML document.
				if (userAgent == null || userAgent.indexOf("MSIE") != -1)
					image = doXsl(image);
				response.getOutputStream().write(image);
			}
		}
		catch (Exception e)
		{
			_logger.warn("Failed to generated SVG", e);
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		}
	}
	
	private byte[] doXsl(byte[] source)
	{
		try
		{
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			StreamResult result = new StreamResult(os);
			TransformerFactory factory = TransformerFactory.newInstance();
			DocumentBuilderFactory documentBuilderFactory = 
				DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			//documentBuilder.setEntityResolver(new EasipEntityResolver());
			
			Node doc = documentBuilder.parse(new ByteArrayInputStream(source));
			
			Transformer transformer = factory.newTransformer(
					new StreamSource(getClass().getResourceAsStream("dataToSvg.xsl")));
			transformer.transform(new DOMSource(doc), result);
			return os.toByteArray();
		}
		catch (Throwable e)
		{
			_logger.warn("Unable to do XSL transformation", e);
			return source;
		}
	}
}
