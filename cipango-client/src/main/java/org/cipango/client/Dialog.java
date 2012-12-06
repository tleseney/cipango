package org.cipango.client;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.URI;

/**
 * A SIP Dialog abstraction.
 */
public class Dialog
{
	public static final String INITIAL_REQUEST_ATTRIBUTE = "INITIAL_REQUEST";
	
	private SipFactory _factory;
	private List<Credentials> _credentials; 
	private SessionHandler _sessionHandler;
	private Address _outboundProxy;
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
	 * Opens a dialog with an incoming request.
	 * <p>
	 * The dialog is configured with the given request. The method returns
	 * immediately and the caller should then prepare and send its response. so
	 * as to finish the dialog establishment.
	 * 
	 * @param request
	 *            The request that was received and which opens this dialog.
	 * @throws ServletException
	 */
	public void accept(SipServletRequest request) throws ServletException
	{
		initialize(request);
	}

	/**
	 * Starts this dialog.
	 * <p>
	 * The dialog is configured and the given request is sent. The method
	 * returns immediately after the request is sent. The caller then should
	 * Call <code>waitForResponse</code> or <code>waitForFinalResponse</code> on
	 * this dialog and react accordingly so as to finish the dialog
	 * establishment.
	 * 
	 * @param request
	 *            The request that will be sent to open this dialog. Ideally,
	 *            this request was created with
	 *            <code>createInitialRequest</code>, or with a specialized
	 *            method in child classes and eventually tweaked as desired
	 *            afterwards.
	 * @throws IOException
	 * @throws ServletException
	 */
	public void start(SipServletRequest request) throws IOException, ServletException
	{
		initialize(request);
		_sessionHandler.send(request);
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
	 * <p>
	 * The initial request being dependent on the kind of dialog, this method is
	 * to be implemented by the dialog specialization classes. The caller is
	 * authorized to complete it before providing it to <code>start</code>.
	 * 
	 * @param local
	 *            the URI of the local user agent, the one which is creating
	 *            this dialog.
	 * @param remote
	 *            the URI of the remote agent in the dialog.
	 * @return The brand new request, associated to this dialog.
	 */
	public SipServletRequest createInitialRequest(String method, URI local, URI remote)
	{
		if (_session != null)
			throw new IllegalStateException("Session already created");
		
		SipApplicationSession appSession = getFactory().createApplicationSession();
		SipServletRequest request = getFactory().createRequest(appSession, method, local, remote);
		if (_outboundProxy != null)
			request.pushRoute(_outboundProxy);
		return request;
		
	}

	public SipServletRequest createRequest(String method)
	{
		if (_session == null)
			throw new IllegalStateException("Session not created");

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

	public Address getOutboundProxy()
	{
		return _outboundProxy;
	}

	public void setOutboundProxy(Address outboundProxy)
	{
		_outboundProxy = outboundProxy;
	}
	
	/**
	 * Update the dialog with the response.
	 * Could be needed if there has been multiple dialogs.
	 */
	public void updateDialog(SipServletResponse response)
	{
		SipSession session = response.getSession();
		if (session.equals(_session))
			return;
		
		if (!session.getCallId().equals(_session.getCallId()))
			throw new IllegalArgumentException("Session " + session + " is not a derived session from " + _session);
		
		String localTag = response.getFrom().getParameter("tag");
		if (!localTag.equals(_session.getLocalParty().getParameter("tag"))
				&& !localTag.equals(_session.getRemoteParty().getParameter("tag")))
			throw new IllegalArgumentException("Session " + session + " is not a derived session from " + _session);
		
		_session = session;
	}
	
	private void initialize(SipServletRequest request) throws ServletException
	{
		if (_session != null)
			throw new ServletException("Dialog already started");

		_sessionHandler = new SessionHandler();
		_session = request.getSession();
		_session.setAttribute(MessageHandler.class.getName(), _sessionHandler);
		_session.setAttribute(INITIAL_REQUEST_ATTRIBUTE, request);
		
		_sessionHandler.setTimeout(_timeout);
		_sessionHandler.setCredentials(_credentials);
	}

}
