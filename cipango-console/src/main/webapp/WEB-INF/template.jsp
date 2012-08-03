<%@ page import="java.util.*,org.cipango.console.*,org.cipango.console.printer.*,org.cipango.console.printer.generic.*, java.io.PrintWriter"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" dir="ltr" lang="en-EN">
<%
	Menu menuPrinter = ((Menu) request.getAttribute(Attributes.MENU));
%>
<head>
  <title><%= menuPrinter.getTitle() %></title>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <meta name="Identifier-URL" content="" />
  <meta name="Description" content="" />
  <meta name="Keywords" content="" />
  <link rel="stylesheet" href="css/style.css" type="text/css" media="screen" />
  <%
 		if(request.getAttribute(Attributes.CSS_SRC) != null)
  		{
	  		String[] css = ((String) request.getAttribute(Attributes.CSS_SRC)).split(",");
			for(int i=0; i<css.length; i++)
			{
				%><link rel="stylesheet" href="<%= css[i] %> " /><%
			}
		}
	 	if(request.getAttribute(Attributes.JAVASCRIPT_SRC) != null)
  		{
	  		String[] js = ((String) request.getAttribute(Attributes.JAVASCRIPT_SRC)).split(",");
			for(int i=0; i<js.length; i++)
			{
				%><script type="text/javascript" src="<%= js[i] %> "></script><%
			}
		}
  %>
</head>
<body>
	<div id="container">
		<div id="header">
			<h1><a href="#" title="cipango console / back to home">cipango</a></h1>
			<div class="login">
				<% 
				if (request.getUserPrincipal() != null) {
				%>
				<span>Logged in as <a href=""><%= request.getUserPrincipal().getName() %></a> | <a href="login.jsp?logout=true" title="Log Out">Log Out</a>&nbsp;&nbsp;</span>
				<% } %>
			</div>
		</div>
			<% 
			menuPrinter.print(out);
			%>
			<div id="container-main">
				<div id="container-main-top"></div>
				<% 
				menuPrinter.getSubMenu().print(out);
				%>
				<div id="main">
					<%
					String info = (String) session.getAttribute(Attributes.INFO);
					if (info != null) {
						%><div class="info"><%= info %></div><%
						session.removeAttribute(Attributes.INFO);
					}
					
					String warn = (String) session.getAttribute(Attributes.WARN);
					if (warn != null) {
						%><div class="warn"><%= warn %></div><%
						session.removeAttribute(Attributes.WARN);
					}
					out.write(menuPrinter.getHtmlTitle());
					HtmlPrinter printer = (HtmlPrinter) request.getAttribute(Attributes.CONTENT);
					if (printer != null) {
						printer.print(out);
					}
				%>
			</div>
			<div id="container-main-bottom"></div>
		</div>
		<div id="footer">
			Powered by <a href="http://www.cipango.org">Cipango</a>
		</div>
	</div>
</body>
</html>
