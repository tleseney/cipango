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
	private Address _contact;
	int _expires;
	
	public RegistrationTask(Registration registration, Address contact)
	{
		_contact = contact;
		_registration = registration;
		_registration.addListener(this);
	}
	
	public void setListener(Listener listener)
	{
		_registration.addListener(listener);
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
	}

	@Override
	public void onUnregistered(Address contact)
	{
		// TODO: what should be the behaviour of this task in this case?
	}

	@Override
	public void onRegistrationFailed(int status)
	{
		// TODO: what should be the behaviour of this task in this case?
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
    			_registration.register(_contact, _expires);
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
