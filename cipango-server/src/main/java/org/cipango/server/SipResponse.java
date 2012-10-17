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
package org.cipango.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.ProxyBranch;
import javax.servlet.sip.Rel100Exception;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.server.transaction.Transaction;
import org.cipango.sip.AddressImpl;
import org.cipango.sip.SipFields;
import org.cipango.sip.SipGenerator;
import org.cipango.sip.SipHeader;
import org.cipango.sip.SipMethod;
import org.cipango.sip.SipStatus;
import org.eclipse.jetty.util.StringUtil;

public class SipResponse extends SipMessage implements SipServletResponse
{
	private SipRequest _request;
	private int _status;
	private String _reason;
	
	public SipResponse()
	{
		
	}
	
	public SipResponse(SipRequest request, int status, String reason)
	{
		_request = request;
		if (status >= 200)
			_request.setCommitted(true);
		
		_session = request._session;
		setStatus(status, reason);
		
		SipFields requestFields = request.getFields();
		_fields.copy(requestFields, SipHeader.VIA);
		_fields.copy(requestFields, SipHeader.FROM);
		_fields.copy(requestFields, SipHeader.TO);
		_fields.copy(requestFields, SipHeader.CALL_ID);
		_fields.copy(requestFields, SipHeader.CSEQ);
		
		if (status < 300)
			_fields.copy(requestFields, SipHeader.RECORD_ROUTE);
		
		if (_session != null)
		{
			if (_request.isInitial())
			{
				AddressImpl to = (AddressImpl) _fields.get(SipHeader.TO);
				if (to.getTag() == null)
				{
					_session.setUAS();
					to.setParameter(AddressImpl.TAG, _session.getLocalTag());
				}
			}
			if (needsContact())
			{
				_fields.set(SipHeader.CONTACT, _session.getContact(request.getConnection()));
			}
		}
	}
	
	protected boolean needsContact()
	{
		return (_request.isInvite() && _status < 300); // TODO
	}
	
	public Transaction getTransaction()
	{
		return _request.getTransaction();
	}
	
	/**
	 * @see SipServletResponse#send()
	 */
	public void send() throws IOException
	{
		send(false);
	}
	
	protected void send(boolean reliable) throws IOException
	{
		if (isCommitted())
			throw new IllegalStateException("response is committed");
		
		_session.sendResponse(this);
		//((ServerTransaction) getTransaction()).send(this);
		
		setCommitted(true);
		// TODO scope
		
	}
	
	public boolean isSuccess()
	{
		return SipStatus.isSuccess(_status);
	}
	
	public boolean isRequest()
	{
		return false;
	}
	
	@Override
	public void setCharacterEncoding(String encoding)
	{
		/*
		 * Because of a change in Servlet spec 2.4 the setCharacterEncoding() 
		 * does NOT throw the java.io.UnsupportedEncodingException as derived 
		 * from SipServletMessage.setCharacterEncoding(String) but inherits 
		 * a more generic setCharacterEncoding() method from the 
		 * javax.servlet.ServletResponse. 
		 */
		_characterEncoding = encoding;
	}
	
	@Override
	public void setBufferSize(int size) {}

	@Override
	public int getBufferSize() { return 0; }

	@Override
	public void flushBuffer() throws IOException {}

	@Override
	public void resetBuffer() {}

	@Override
	public void reset() {}

	@Override
	public void setLocale(Locale locale) 
	{
		setContentLanguage(locale);
	}

	@Override
	public SipServletRequest createAck() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SipServletRequest createPrack() throws Rel100Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<String> getChallengeRealms() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException { return null; }

	@Override
	public Proxy getProxy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ProxyBranch getProxyBranch() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see SipServletResponse#getReasonPhrase()
	 */
	public String getReasonPhrase()
	{
		return _reason;
	}

	/**
	 * @see SipServletResponse#getRequest()
	 */
	public SipServletRequest getRequest()
	{
		return _request;
	}

	/**
	 * @see SipServletResponse#getStatus()
	 */
	public int getStatus()
	{
		return _status;
	}
	
	public Locale getLocale() 
	{
		return getContentLanguage();
	}

	/**
	 * @see SipServletMessage#getMethod
	 */
	public String getMethod()
	{
		return getRequest().getMethod();
	}
	
	@Override
	public PrintWriter getWriter() throws IOException 
	{
		return null;
	}

	@Override
	public boolean isBranchResponse() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void sendReliably() throws Rel100Exception {
		// TODO Auto-generated method stub
		
	}

	
	public void setStatus(int status) 
	{
		setStatus(status, null);		
	}

	/**
	 * @see SipServletResponse#setStatus(int, String)
	 */
	public void setStatus(int status, String reason) 
	{
		if (status < 100 || status >= 700) 
    		throw new IllegalArgumentException("Invalid status-code: " + status);
    	    	
		_status = status;
		_reason = reason;
	}

	@Override
	protected boolean canSetContact() 
	{
		 return _request.isRegister() 
        		||(getStatus() >= 300 && getStatus() < 400) 
        		|| getStatus() == 485
        		|| (getStatus() == 200 && _request.isMethod(SipMethod.OPTIONS));
	}
	
	@Override
	public String toString()
	{
		ByteBuffer buffer = ByteBuffer.allocate(4096); // FIXME size
		new SipGenerator().generateResponse(buffer, _status, _reason, _fields, getRawContent());
		return new String(buffer.array(), StringUtil.__UTF8_CHARSET);
	}

}
