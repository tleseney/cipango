package org.cipango.client;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.URI;

/**
 * A SIP Dialog abstraction.
 */
public abstract class AbstractDialog
{
	private SipFactory _factory;
	private List<Credentials> _credentials; 
	private long _timeout;

	protected SipSession _session;
	private SessionHandler _sessionHandler;
	
	public SipFactory getFactory()
	{
		return _factory;
	}
	
	public void setFactory(SipFactory factory)
	{
		_factory = factory;
	}

	public List<Credentials> getCredentials()
	{
		return _credentials;
	}

	public void setCredentials(List<Credentials> credentials)
	{
		_credentials = credentials;
	}
	
	public long getTimeout()
	{
		return _timeout;
	}

	public void setTimeout(long timeout)
	{
		_timeout = timeout;
	}
	
	public SipSession getSession()
	{
		return _session;
	}	

	/**
	 * Starts this dialog.
	 * 
	 * The dialog is configured and the given request is sent. The method
	 * returns immediately after the request is sent. The caller then should
	 * Call <code>waitForResponse</code> or <code>waitForFinalResponse</code> on
	 * this dialog and react accordingly so as to finish the dialog
	 * establishment.
	 * 
	 * @param request
	 *            The request that will be sent to open the dialog. Ideally,
	 *            this request was created with
	 *            <code>createInitialRequest</code> and eventually tweaked as
	 *            desired afterwards.
	 * @throws IOException
	 * @throws ServletException
	 */
	public void start(SipServletRequest request) throws IOException, ServletException
	{
		if (_session != null)
			throw new ServletException("Dialog already started");

		_sessionHandler = new SessionHandler();

		_session = request.getSession();
		_session.setAttribute(MessageHandler.class.getName(), _sessionHandler);

		_sessionHandler.setTimeout(_timeout);
		// handler.setCredentials(_credentials);
		// handler.send();
		request.send();
	}
	
	/**
	 * Synchronously ends this dialog.
	 * 
	 * @param request
	 * @throws IOException
	 */
	public void terminate(SipServletRequest request) throws IOException
	{
		if (_session == null)
			return;

		request.send();
		_sessionHandler.waitForFinalResponse();
	}

	/**
	 * Creates the request that should initiate the dialog.
	 * 
	 * The initial request being dependent on the kind of dialog, this method is
	 * to be implemented by the dialog specialization classes. The caller is
	 * autorized to complete it before providing it to <code>start</code>.
	 * 
	 * @param local
	 *            the URI of the local user agent, the one which is creating
	 *            this dialog.
	 * @param remote
	 *            the URI of the remote agent in the dialog.
	 * @return The brand new request, associated to this dialog.
	 */
	public abstract SipServletRequest createInitialRequest(URI local, URI remote);

	public SipServletRequest createRequest(String method)
	{
		if (_session == null)
			return null;

		return _session.createRequest(method);
	}

	public SipServletRequest waitForRequest()
	{
		if (_session == null)
			return null;

		return _sessionHandler.waitForRequest();
	}
	
	public SipServletResponse waitForResponse()
	{
		if (_session == null)
			return null;

		return _sessionHandler.waitForResponse();
	}
	
	public SipServletResponse waitForFinalResponse()
	{
		if (_session == null)
			return null;
		return _sessionHandler.waitForFinalResponse();
	}

	public SessionHandler getSessionHandler()
	{
		return _sessionHandler;
	}

}
