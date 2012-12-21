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
package org.cipango.tests;

import org.cipango.client.Credentials;

public class CredentialsImpl implements Credentials
{
	private String _realm;
	private String _user;
	private String _password;
	
	public CredentialsImpl(String realm, String user, String password)
	{
		_realm = realm;
		_user = user;
		_password = password;
	}
	public String getUser()
	{
		return _user;
	}

	public String getPassword()
	{
		return _password;
	}

	public String getRealm()
	{
		return _realm;
	}

}
