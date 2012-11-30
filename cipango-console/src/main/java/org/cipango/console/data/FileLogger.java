// ========================================================================
// Copyright 2012 NEXCOM Systems
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
package org.cipango.console.data;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletRequest;

import org.cipango.console.Action;
import org.cipango.console.Action.StartAction;
import org.cipango.console.Action.StopAction;
import org.cipango.console.Manager;
import org.cipango.console.menu.Page;

public class FileLogger
{
	protected MBeanServerConnection _mbsc;
	private boolean _deleteSupported;
	private ObjectName _objectName;
	private Page _page;
		
	public FileLogger(MBeanServerConnection mbsc, Page page, ObjectName objectName, boolean deleteSupported) throws Exception
	{
		_mbsc = mbsc;
		_page = page;
		_deleteSupported = deleteSupported;
		_objectName = objectName;
	}
	
	public boolean isEnabled() throws Exception
	{
		return Manager.isRunning(_mbsc, _objectName);
	}
	
	public boolean isRegistered() throws IOException
	{
		return _mbsc.isRegistered(_objectName);
	}
	
	public String getFilename() throws Exception
	{
		return (String) _mbsc.getAttribute(_objectName, "filename");
	}
	
	public Integer getRetainDays() throws Exception
	{
		return (Integer) _mbsc.getAttribute(_objectName, "retainDays");
	}

	public boolean isDeletedSupported()
	{
		return _deleteSupported;
	}
	
	public Action getStartAction()
	{
		return new StartFileLoggerAction(_page, _objectName);
	}
	
	public Action getStopAction()
	{
		return new StopFileLoggerAction(_page, _objectName);
	}
	
	public Action getDeleteAction()
	{
		return new DeleteLogsFilesAction(_page);
	}
	
	public static class StartFileLoggerAction extends StartAction
	{
		public StartFileLoggerAction(Page page, ObjectName objectName)
		{
			super(page, "activate-file-message-log", objectName);
		}
	}
	
	public static class StopFileLoggerAction extends StopAction
	{
		public StopFileLoggerAction(Page page, ObjectName objectName)
		{
			super(page, "deactivate-file-message-log", objectName);
		}
	}
	
	public static class DeleteLogsFilesAction extends Action
	{
		public DeleteLogsFilesAction(Page page)
		{
			super(page, "delete-logs-files");
		}

		@Override
		protected void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
		{
			throw new UnsupportedOperationException();
		}
	}
	
}
