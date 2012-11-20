package org.cipango.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

public class AsyncMessageHandler implements MessageHandler
{
	private static final Set<String> DIALOG_REQUESTS = new HashSet<String>(Arrays.asList(
		     "INVITE", "REFER", "SUBSCRIBE"));
		     
	private List<Listener> _listeners = new ArrayList<Listener>();
	private List<Credentials> _credentials = new ArrayList<Credentials>();
	private long _timeout;

	public interface Listener
	{
		void onRequest(SipServletRequest request);
		void onDialog(SipServletRequest request, Dialog dialog);
	}

	public long getTimeout()
	{
		return _timeout;
	}

	public void setTimeout(long timeout)
	{
		_timeout = timeout;
	}

	public List<Credentials> getCredentials()
	{
		return _credentials;
	}

	public void addCredentials(Credentials credentials)
	{
		_credentials.add(credentials);
	}
	
	public List<Listener> getListeners()
	{
		return _listeners;
	}

	public void addListener(Listener listener)
	{
		if (!_listeners.contains(listener))
			_listeners.add(listener);
	}

	public void removeListener(Listener listener)
	{
		_listeners.remove(listener);
	}
	
	@Override
	public void handleRequest(SipServletRequest request) throws IOException, ServletException
	{
		if (DIALOG_REQUESTS.contains(request.getMethod()))
		{
			Dialog dialog = (request.getMethod().equals(SipMethods.INVITE)) ?
					new Call() : new Dialog();
			
			// The dialog MUST be customized *before* accept is called.
			customize(dialog);
			dialog.accept(request);

			for (Listener l : _listeners)
				l.onDialog(request, dialog);
		}
		else
		{
			for (Listener l : _listeners)
				l.onRequest(request);
		}
	}

	@Override
	public void handleResponse(SipServletResponse response) throws IOException, ServletException
	{
	}
	
	protected void customize(Dialog dialog)
	{
		// Normally, setting the factory or the outbound proxy should not be
		// necessary in this context.
		dialog.setCredentials(_credentials);
		dialog.setTimeout(_timeout);
	}
}
