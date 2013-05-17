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
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ClickToDialHttpServlet extends HttpServlet
{


	public ClickToDialService getClickToDialService()
	{
		return (ClickToDialService) getServletContext().getAttribute(ClickToDialService.class.getName());
	}
	
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
		out.println("<html><head><title>Click to dial service</title></head><body>");
		String action = request.getParameter("action");
		if ("call".equals(action))
		{
			String from = request.getParameter("from");
			String to = request.getParameter("to");
			if (from != null && to != null)
			{
				out.println(from + " is calling " + to);
				getClickToDialService().call(from, to);
			}
			else
				out.write("Cannot make call, missing \"from\" or \"to\" parameter");
		}

		List<Call> calls = getClickToDialService().getCalls();
		
		if ("hangup".equals(action))
		{
			String id = request.getParameter("id");
			if (id != null)
			{
				 for (Call call : calls) {
					 if (call.getId().equals(id))
						 call.hangup();
				 }
			} else
			{
				out.write("Cannot hang up call, missing \"id\" parameter");
			}
		}

		
		
		out.println("<h2>Click to dial service</h2>");

		Map<String, SortedSet<Binding>> bindings = getBindings();
		synchronized (bindings)
		{			
			out.println("<form method=\"post\" action=\"" + request.getRequestURI() + "\">");
			out.println("From:&nbsp;<SELECT name=\"from\">");	
		    for (String aor : bindings.keySet()) {
		    	out.println("<OPTION>" + aor + "</OPTION>");
		    }
		    out.println("</SELECT>");	
		    out.println("To:&nbsp;<SELECT name=\"to\">");
		    int i = 0;
		    for (String aor : bindings.keySet()) {
		    	out.println("<OPTION " + ((i++ == 1) ? "SELECTED" : "") +  ">" + aor + "</OPTION>");
		    }
		    out.println("</SELECT>");
		    out.println("<input name=\"action\" type=\"submit\" value=\"call\"/>");
		    out.println("<input type=\"submit\" value=\"refresh\"/>");
		    out.println("</form>");	
		}
		
		if (!calls.isEmpty())
		{
			out.println("<h2>Running calls</h2>");
		    out.println("<table border=\"1\" cellspacing=\"0\">" +
    		"<th>From</th><th>To</th><th>State</th><th>Actions</th>");

		    for (Call call : calls) {
				out.println("<tr>");
				out.println("<td>" + call.getFrom() + "</td>");
				out.println("<td>" + call.getTo() + "</td>");
				out.println("<td>" + call.getState() + "</td>");
				out.println("<td><form method=\"post\" action=\"" + request.getRequestURI() + "\">");
				out.println("<input type=\"hidden\" name=\"id\" value=\"" + call.getId() + "\"/>");
				out.println("<input name=\"action\" type=\"submit\" value=\"hangup\"/>");
				out.println("</form></td>");	
				out.println("</tr>");
		    }
		    out.println("</table>");
		}
		
		out.println("</body></html>");
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
	IOException
	{
		doGet(request, response);
	}
	
	
}
