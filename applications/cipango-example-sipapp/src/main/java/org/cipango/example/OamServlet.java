// ========================================================================
// Copyright 2007-2009 NEXCOM Systems
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
package org.cipango.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.SortedSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



public class OamServlet extends HttpServlet
{
	
	
	@SuppressWarnings("unchecked")
	public Map<String, SortedSet<Binding>> getBindings()
	{
		return (Map<String, SortedSet<Binding>>) getServletContext().getAttribute(Binding.class.getName());
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException
	{
		PrintWriter out = response.getWriter();
		out.println("<html><head><title>OAM</title></head><body>");
		out.println("<h2>Registered Users</h2>");

		Map<String, SortedSet<Binding>> bindings = getBindings();
		synchronized (bindings)
		{
			String aor = request.getParameter("aor");
			if (aor != null)
			{
				bindings.remove(aor);
			}
			
		    out.println("<table border=\"1\" cellspacing=\"0\">" +
		    		"<th>AOR</th><th>Contact</th><th>Expires</th><th>Quality</th><th>Actions</th>");

		    for (SortedSet<Binding> l : bindings.values()) {
		    	boolean first = true;		
				synchronized (l)
				{
					for (Binding binding : l)
					{
						out.println("<tr>");
						if (first)
						{
							out.println("<td ROWSPAN=\"" + l.size() + "\">" + binding.getAor() + "</td>");
						}
						out.println("<td>" + binding.getContact() + "</td>");
						out.println("<td>" + binding.getExpires() + "</td>");
						out.println("<td>" + binding.getQ() + "</td>");
						if (first)
						{
							out.println("<td ROWSPAN=\"" + l.size() + "\"><A href=\"" + request.getRequestURI() 
									+ "?aor=" + binding.getAor() + "\">Deregister</A></td>");
							first = false;
						}
						
						out.println("</tr>");
					}
					
				}
		    }
		    out.println("</table>");
		    out.println("</body></html>");
		}
	
	}

}
