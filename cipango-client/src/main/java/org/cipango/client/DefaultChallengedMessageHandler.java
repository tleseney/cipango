package org.cipango.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

/**
 * This class is the lightest possible implementation of
 * {@link ChallengedMessageHandler}. It simply handles messages as they come and
 * notifies all the threads waiting for this. It stores neither handled requests
 * nor responses.
 * <p>
 * If credentials are provided, then authentication is automatically handled
 * with the help of a {@link AuthenticationHelper}. Responses with a challenge
 * which can be successfully resolved by the helper are handled silently and are
 * not notified. Once a helper was used, it is attached to the response's
 * session, so users of this object should retrieve it and call its
 * {@link AuthenticationHelper#addAuthentication(SipServletRequest)} on any new
 * request belonging to the same session request before sending it.
 */
public class DefaultChallengedMessageHandler implements
		ChallengedMessageHandler
{
	public static final String HANDLED_ATTRIBUTE = "Challenge-Handled";
	
	private List<Credentials> _credentials;
	private long _timeout;
	
	public List<Credentials> getCredentials()
	{
		return _credentials;
	}

	public void setCredentials(List<Credentials> credentials)
	{
		_credentials = credentials;
	}
	
	public void addToCredentials(Credentials credentials)
	{
		if (_credentials == null)
			_credentials = new ArrayList<Credentials>();
		if (!_credentials.contains(credentials))
			_credentials.add(credentials);
	}
	
	public long getTimeout()
	{
		return _timeout;
	}

	public void setTimeout(long timeout)
	{
		_timeout = timeout;
	}
	
	@Override
	public void handleRequest(SipServletRequest request) throws IOException,
			ServletException
	{
		synchronized (this)
		{
			notifyAll();
		}
	}

	@Override
	public void handleResponse(SipServletResponse response) throws IOException,
			ServletException
	{
		synchronized (this)
		{
			notifyAll();
		}
	}

	@Override
	public boolean handleAuthentication(SipServletResponse response)
			throws IOException, ServletException
	{
		if (_credentials != null && !_credentials.isEmpty())
		{
			AuthenticationHelper helper = getOrCreateAuthenticationHelper(response);
			if (helper.handleChallenge(response))
			{
				response.setAttribute(HANDLED_ATTRIBUTE, true);
				return false;
			}
		}
		return true;
	}

	protected AuthenticationHelper getAuthenticationHelper(
			SipServletMessage message)
	{
		return (AuthenticationHelper) message.getSession().getAttribute(
				AuthenticationHelper.AUTH_HELPER);
	}
	
	protected AuthenticationHelper getOrCreateAuthenticationHelper(SipServletMessage message)
	{
		AuthenticationHelper helper = getAuthenticationHelper(message);
		if (helper == null)
		{
			helper = new AuthenticationHelper(_credentials);
			message.getSession().setAttribute(AuthenticationHelper.AUTH_HELPER, helper);
		}
		return helper;
	}

	protected void doWait(Object o)
	{
		synchronized (o)
		{
			try { o.wait(_timeout); } catch (InterruptedException e) {}
		}
	}
	
	protected void doWait(Object o, long timeout)
	{
		synchronized (o)
		{
			try { o.wait(timeout); } catch (InterruptedException e) {}
		}
	}
}
