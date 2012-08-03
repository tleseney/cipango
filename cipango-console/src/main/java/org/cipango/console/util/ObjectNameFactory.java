package org.cipango.console.util;

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * A simple factory for creating safe object names. This factory will <b>not</b>
 * throw MalformedObjectNameException. Any such exceptions will be translated
 * into Errors.
 * 
 * <p>
 * This should only be used where it is not possible to catch a
 * MalformedObjectNameException, such as when defining a static final in an
 * interface.
 * 
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @version $Revision: 1.1 $
 */
public class ObjectNameFactory
{

	public static ObjectName create(String name)
	{
		try
		{
			return new ObjectName(name);
		}
		catch (MalformedObjectNameException e)
		{
			throw new Error("Invalid ObjectName: " + name, e);
		}
	}

	public static ObjectName create(String domain, String key, String value)
	{
		try
		{
			return new ObjectName(domain, key, value);
		}
		catch (MalformedObjectNameException e)
		{
			throw new Error("Invalid ObjectName: " + domain + "," + key + ","
					+ value + "; " + e);
		}
	}

	public static ObjectName create(String domain, Hashtable<String, String> table)
	{
		try
		{
			return new ObjectName(domain, table);
		}
		catch (MalformedObjectNameException e)
		{
			throw new Error("Invalid ObjectName: " + domain + "," + table
					+ "; " + e);
		}
	}

}