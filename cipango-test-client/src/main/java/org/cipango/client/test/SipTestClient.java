package org.cipango.client.test;

import java.io.File;

import org.cipango.client.SipClient;
import org.cipango.server.SipServer;
import org.cipango.server.log.FileMessageLog;

public class SipTestClient extends SipClient
{
	
	public SipTestClient() 
	{
		super();
	}

	public SipTestClient(int port) 
	{
		super(port);
	}

	public SipTestClient(String host, int port) 
	{
		super(host, port);
	}

	public SipServer getServer()
	{
		return super.getServer();
	}
	
	public void setMessageLogger(String baseDir, Class<?> testedClass, String method, String username)
	{
		FileMessageLog logger = new FileMessageLog();
		logger.setAppend(false);
		File file = new File(baseDir, testedClass.getSimpleName());
		file.mkdirs();
		StringBuilder sb = new StringBuilder();
		if (method != null)
			sb.append(method).append('-');
		int port =  getServer().getConnectors()[0].getPort();
		if (username != null)
			sb.append(username).append("-(").append(port).append(")");
		else
			sb.append("-on-port-" + port);
		sb.append(".log");
		file = new File(file, sb.toString());
		logger.setFilename(file.getPath());
		logger.setRetainDays(-1);
		getServer().setAccessLog(logger);
	}
}
