package org.cipango.sipunit;

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
