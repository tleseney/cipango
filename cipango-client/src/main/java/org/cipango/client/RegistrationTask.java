package org.cipango.client;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;

import org.cipango.client.Registration.Listener;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Manages background upholding of a registration.
 */
public class RegistrationTask implements Registration.Listener, Runnable
{
	private Logger _log = Log.getLogger(RegistrationTask.class);
	private Registration _registration;
	private Registration.Listener _listener;
	private Address _contact;
	int _expires;
	
	public RegistrationTask(Registration registration, Address contact)
	{
		_contact = contact;
		_registration = registration;
		_registration.setListener(this);
	}
	
	public void setListener(Listener listener)
	{
		_listener = listener;
	}
	
	public Registration getRegistration()
	{
		return _registration;
	}

	@Override
	public void onRegistered(Address contact, int expires,
			List<Address> contacts)
	{
		_expires = expires;
		if (_listener != null)
			_listener.onRegistered(contact, expires, contacts);
	}

	@Override
	public void onUnregistered(Address contact)
	{
		if (_listener != null)
			_listener.onUnregistered(contact);
		
		// TODO: what should be the behaviour of this task in this case?
	}

	@Override
	public void onRegistrationFailed(int status)
	{
		if (_listener != null)
			_listener.onRegistrationFailed(status);
		
	}
	
	@Override
	public void run()
	{
		long delay = _expires * 1000 / 2;
		
		while (true)
		{
    		try
    		{
    			Thread.sleep(delay);
    			_registration.register(_contact.getURI(), _expires);
    			delay = _expires * 1000 / 2;
    		}
    		catch (InterruptedException e)
    		{
    			break;
    		}
			catch (IOException e)
			{
				_log.warn(e);
				delay = 60000;
			}
			catch (ServletException e)
			{
				_log.warn(e);
				delay = 60000;
			}
		}
	}
}
