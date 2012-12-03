package org.cipango.console.util;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class LogAdapter implements LogChute
{
	private Logger _logger = Log.getLogger("org.cipango.console.util.LogAdapter.velocity");

	@Override
	public void init(RuntimeServices rs) throws Exception
	{
	}

	@Override
	public void log(int level, String message)
	{
		switch (level)
		{
		case DEBUG_ID:
			_logger.debug(message, (Object) null);
			break;
		case INFO_ID:
			_logger.info(message, (Object) null);
			break;
		case WARN_ID:
		case ERROR_ID:
			_logger.warn(message, (Object) null);
			break;
		default:
			break;
		}
	}

	@Override
	public void log(int level, String message, Throwable t)
	{
		switch (level)
		{
		case DEBUG_ID:
			_logger.debug(message, t);
			break;
		case INFO_ID:
			_logger.info(message, t);
			break;
		case WARN_ID:
		case ERROR_ID:
			_logger.warn(message, t);
			break;
		default:
			break;
		}
	}

	@Override
	public boolean isLevelEnabled(int level)
	{
		switch (level)
		{
		case TRACE_ID:
			return false;
		case DEBUG_ID:
			return _logger.isDebugEnabled();
		case INFO_ID:
		case WARN_ID:
		case ERROR_ID:
			return true;
		default:
			return false;
		}
	}

}
