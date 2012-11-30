package org.cipango.console;

import java.io.IOException;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.eclipse.jetty.util.component.AbstractLifeCycle;

public class Manager
{
	protected MBeanServerConnection _mbsc;
	
	public Manager(MBeanServerConnection mbsc)
	{
		_mbsc = mbsc;
	}
	
	public boolean isRunning(ObjectName objectName) throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, IOException
	{
		return isRunning(_mbsc, objectName);
	}
	
	public static boolean isRunning(MBeanServerConnection mbsc, ObjectName objectName) throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, IOException
	{
		String state = (String) mbsc.getAttribute(objectName, "state");
		return state.equals(AbstractLifeCycle.STARTED) || state.equals(AbstractLifeCycle.STARTING);
	}
}
