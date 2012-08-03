<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"">
<head>
		<title>Cipango console signin page</title>
		<link rel="stylesheet" href="css/style.css" type="text/css" media="screen" />
</head>
<body>
	<div id="header">
		<h1><a href="#" title="cipango console / back to home">cipango</a></h1>
	</div>
	<div id="menu">
		<ul>
		</ul>
	</div>
		<div id="container-main">
	
		<div id="container-main-top"></div>
	
		<!-- ************************************************* -->
		<!-- *** SUB-MENU ************************************ -->
		
		<div id="main">
			<h1>Log In to Cipango console</h1>
			<div class="data">
				<form method="POST" action="j_security_check">
					<table cellpadding="0" cellspacing="0" class="table_login">
						 <%
								if ("true".equals(request.getParameter("logout")))
								{
									if (session != null)
										session.invalidate();
									%><tr><td colspan="2"><div class="info">Sucessful logout</div></td></tr><%
								}
								if ("true".equals(request.getParameter("authFail")))
									%><tr><td colspan="2"><div class="warn">Invalid username or password</div></td></tr><%
						%>
						<tr>
							<td>Username:</td>
							<td><input type="text" name="j_username" id="login" /></td>
						</tr>
						<tr>
							<td>Password:</td>
							<td><input type="password" name="j_password" id="password" /></td>
						</tr>
						<tr>
							<td></td>
							<td><input type="submit" value="Log In" /></td>
						</tr>
					</table>
				</form>
			</div>
				
		</div>
			
		</div>
		
		<div id="container-main-bottom"></div>
		
	</div>
	
	
	<!-- ************************************************* -->
	<!-- *** FOOTER ************************************** -->
	<div id="footer">
			Powered by <a href="http://www.cipango.org">Cipango</a>
	</div>

</div>

</body>
</html>

