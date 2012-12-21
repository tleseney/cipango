// ========================================================================
// Copyright 2008-2012 NEXCOM Systems
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
package org.cipango.server.security;

import javax.servlet.sip.AuthInfo;

import org.eclipse.jetty.util.LazyList;

public class AuthInfoImpl implements AuthInfo
{
	private Object _authInfos;

	public AuthElement getAuthElement(int statusCode, String realm)
	{
		for (int i = LazyList.size(_authInfos); i-- > 0;)
		{
			AuthElement element = (AuthElement) LazyList.get(_authInfos, i);
			if (element.getStatusCode() == statusCode
					&& element.getRealm().equals(realm))
				return element;
		}
		return null;
	}

	public void addAuthInfo(int statusCode, String realm, String username,
			String password)
	{
		AuthElement authElement = new AuthElement();
		authElement.setStatusCode(statusCode);
		authElement.setRealm(realm);
		authElement.setUsername(username);
		authElement.setPassword(password);
		_authInfos = LazyList.add(_authInfos, authElement);
	}

	public static class AuthElement
	{
		private int _statusCode;
		private String _realm;
		private String _username;
		private String _password;

		public int getStatusCode()
		{
			return _statusCode;
		}

		public void setStatusCode(int statusCode)
		{
			_statusCode = statusCode;
		}

		public String getRealm()
		{
			return _realm;
		}

		public void setRealm(String realm)
		{
			_realm = realm;
		}

		public String getUsername()
		{
			return _username;
		}

		public void setUsername(String username)
		{
			_username = username;
		}

		public String getPassword()
		{
			return _password;
		}

		public void setPassword(String password)
		{
			_password = password;
		}
	}
}
