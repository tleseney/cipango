// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
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

package org.cipango.server.log.event;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class LogEventDispatcher implements EventDispatcher
{
	 private Logger _logger;
	 
	 public LogEventDispatcher()
	 {
		 _logger = Log.getLogger("cipango.event");
	 }
	
	public void dispatch(int eventType, String message)
	{
		switch (eventType)
		{
		case Events.START:
			_logger.info(message);
			break;
		default:
			_logger.warn(message);
			break;
		}
	}
}
