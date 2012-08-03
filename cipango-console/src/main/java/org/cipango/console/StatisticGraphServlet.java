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
package org.cipango.console;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;


public class StatisticGraphServlet extends HttpServlet
{
	private static final Long ONE_HOUR = new Long(3600);
	private Logger _logger = Log.getLogger("console");
	

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		try
		{
			StatisticGraph statisticGraph = (StatisticGraph) request.getSession().getAttribute(StatisticGraph.class.getName());
			
			if (statisticGraph == null)
			{
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
			
			response.setContentType("image/png");
			String sTime = request.getParameter("time");
			String type = request.getServletPath().toUpperCase();
			type = type.substring(0, type.length() - 4); // Remove ".png"
			int index = type.lastIndexOf('/');
			if (index != -1)
				type = type.substring(index + 1);
						
			Long time;
			if (sTime == null)
				time = ONE_HOUR;
			else
			{
				try
				{
					time = Long.valueOf(sTime);
				}
				catch (NumberFormatException e)
				{
					time = ONE_HOUR;
				}
			}
			byte[] image = statisticGraph.createGraphAsPng(time, type);

			response.getOutputStream().write(image);
		}
		catch (Exception e)
		{
			_logger.warn("Unable to create graph", e);
		}
	}
}
