package org.cipango.client;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
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
	 * Synchronously starts this dialog.
	 * 
	 * This method returns either when success, failure or request timeout is
	 * encountered. Authentication is handled automatically if credentials were
	 * provided to this <code>AbstractDialog</code>.
	 * 
	 * @param request
	 *            The request that will be sent to open the dialog. Ideally,
	 *            this request was created with
	 *            <code>createInitialRequest</code> and eventually updated
	 *            afterwards.
	 * @return <code>true</code> if dialog was successfully created,
	 *         <code>false</code> otherwise.
	 * @throws IOException
	 * @throws ServletException
	 */
	public boolean start(SipServletRequest request) throws IOException, ServletException
	{
		if (_session != null)
			throw new ServletException("Dialog already started");

		_session = request.getSession();

		// TODO: set SessionHandler, set creds and timeout
		// handler.send();
		//
		return false;
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
		
		// TODO: handler.waitForFinalResponse();
	}

	/**
	 * Creates the request that should initiate the dialog.
	 * 
	 * The initial request being dependent on the kind of dialog, this method is
	 * to be implemented by the dialog specialization classes.
	 * 
	 * @param local
	 *            the URI of the local <code>UserAgent</code>.
	 * @param remote
	 *            the URI of the remote agent in the dialog.
	 * @return
	 */
	public abstract SipServletRequest createInitialRequest(URI local, URI remote);

	public SipServletRequest createRequest(String method)
	{
		return _factory.createRequest(_session.getApplicationSession(), method,
				_session.getLocalParty(), _session.getRemoteParty());
	}
	
	public void send(SipServletRequest request)
	{
		
	}
}
