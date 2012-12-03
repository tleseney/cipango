// ========================================================================
// Copyright 2010-2012 NEXCOM Systems
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
package org.cipango.console;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class Deployer
{

	private static final String JAR_PREFIX = "jar:";
	private static final String JAR_POSTFIX = "!/";
	private MBeanServerConnection _connection;
	private Logger _logger = Log.getLogger(Deployer.class);
	
	public Deployer(MBeanServerConnection connection)
	{
		_connection = connection;
	}
	
	public boolean undeploy(ObjectName contextHandler)
	{
		try
		{
			File warFile = getWarFile(contextHandler);
			
			File contextFile = getContextFile(contextHandler);

			if (contextFile != null && contextFile.exists())
				contextFile.delete();

			boolean deleteOk = deleteAll(warFile);
			if (!deleteOk)
			{
				_logger.info("Unable to delete application file: {}", warFile, null);
				return false;
			}
			return true;
		}
		catch (Exception e)
		{
			_logger.warn("Unable to undeploy context " + contextHandler, e);
			return false;
		}
	}
		
	private File getContextFile(ObjectName contextHandler) throws Exception
	{
		String contextPath = (String) _connection.getAttribute(contextHandler, "contextPath");
		return getContextFile(contextPath);
	}
	
	private File getContextFile(String contextPath) throws Exception
	{
		String filename = null;
		if (_connection.isRegistered(ApplicationManager.CONTEXT_DEPLOYER))
			filename = (String) _connection.invoke(ApplicationManager.CONTEXT_DEPLOYER,
				"getContextFilename", new Object[] {contextPath}, new String[] {String.class.getName()});
		if (filename != null)
			return new File(filename);	
		return null;
	}
	
	
	private File getWarFile(ObjectName contextHandler) throws Exception
	{
		String warPath = (String) _connection.getAttribute(contextHandler, "war");
		if (warPath.startsWith(JAR_PREFIX))
		{
			warPath = warPath.substring(JAR_PREFIX.length());
			int index = warPath.length() - JAR_POSTFIX.length();
			warPath = warPath.substring(0, index);
			return new File(new URI(warPath));
		}
		else if (warPath.startsWith("file:"))
		{
			return new File(new URI(warPath));
		}
		else
		{
			// When it has been deployed with context file, protocol is not
			// present
			return new File(warPath);
		}
	}
	
	private boolean deleteAll(File root)
	{
		boolean success = true;
		if (root.exists())
		{
			if (!root.isDirectory())
			{
				success = root.delete();
			}
			else
			{
				File[] files = root.listFiles(new FilenameFilter()
				{
					public boolean accept(File dir, String name)
					{
						if (name.equals(".") || name.equals(".."))
						{
							return false;
						}
						return true;
					}
				});
				for (int i = 0; i < files.length; i++)
				{
					success = deleteAll(files[i]) && success;
				}
				success = root.delete() && success;
			}
		}
		return success;
	}
	
	/**
	 * 
	 * @param name
	 *            the deployement name.
	 * @param sarFile
	 *            the content of sar to deploy.
	 * @return <code>true</code> if the deploy is successful.
	 */
	public void deploy(String name, byte[] sarContent) throws Exception
	{
		int index = Math.max(name.lastIndexOf("/"), name.lastIndexOf("\\"));
		if (index != -1)
			name = name.substring(index + 1);
		
		if (!name.endsWith(".war") && !name.endsWith(".sar") && !name.endsWith(".jar"))
			throw new IllegalArgumentException("Bad extension in name: " + name  + ". Allowed is '.war' and '.sar'");

		assertValidArchive(sarContent);


		File deployDir = getDeployDir();
		File warFile = new File(deployDir, name);
		
		if (!deployDir.equals(warFile.getParentFile()))
			throw new IllegalArgumentException("Invalid deployment: " + name);

		if (warFile.exists())
		{
			if (warFile.canWrite() && warFile.isFile())
			{
				_logger.info("Deployment {} already exist, overwrite it", name, null);
			}
			else
			{
				throw new IllegalArgumentException("Deployment "
						+ " already exist and could not overwrite it");
			}
		}
		else
			_logger.info("Create new deployment {}", name, null);
		
		FileOutputStream os = new FileOutputStream(warFile);
		os.write(sarContent);
		os.close();

		_logger.debug("Copy file {} successful", name, null);

	}
		
	private File getDeployDir() throws JMException, IOException
	{
		if (_connection.isRegistered(ApplicationManager.SIP_APP_DEPLOYER))
		{
			String directory = (String) _connection.getAttribute(ApplicationManager.SIP_APP_DEPLOYER, "webAppDir");
			return new File(directory);
		}
		else
		{
			return new File(System.getProperty("jetty.home", "."), "sipapps");
		}
	}
	
	private void assertValidArchive(byte[] sarContent)
	{
		try
		{
			JarInputStream is = new JarInputStream(new ByteArrayInputStream(sarContent));
			ZipEntry entry;
			if ((entry = is.getNextEntry()) == null)
				throw new IllegalArgumentException("Not a JAR archive format");

			do
			{
				if (!entry.isDirectory()
						&& (entry.getName().equals("WEB-INF/sip.xml") || entry.getName().equals(
								"WEB-INF/web.xml")))
					return;
			}
			while ((entry = is.getNextEntry()) != null);
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException("Not a JAR archive format: " + e.getMessage());
		}
		throw new IllegalArgumentException("Missing WEB-INF/sip.xml or WEB-INF/web.xml in archive");

	}


}
