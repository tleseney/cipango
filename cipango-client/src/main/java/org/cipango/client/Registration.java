package org.cipango.client;

import java.util.List;

import javax.servlet.sip.Address;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

public class Registration 
{
	public Registration(SipURI uri)
	{
		
	}
	
	public SipServletRequest createRegister(URI contact, int expires)
	{
		return null;
	}
	
	public interface Listener
	{
		void onRegistered(Address contact, int expires, List<Address> contacts);
		void onRegistrationFailed(int status);
	}
}
