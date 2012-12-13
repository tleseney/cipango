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
import java.nio.BufferOverflowException;
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

import org.cipango.server.session.SessionManager.ApplicationSessionScope;
import org.cipango.server.transaction.ClientTransaction;
import org.cipango.server.transaction.Transaction;
import org.cipango.sip.AddressImpl;
import org.cipango.sip.SipFields;
import org.cipango.sip.SipFields.Field;
import org.cipango.sip.SipGenerator;
import org.cipango.sip.SipGrammar;
import org.cipango.sip.SipHeader;
import org.cipango.sip.SipMethod;
import org.cipango.sip.SipStatus;
import org.cipango.sip.Via;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class SipResponse extends SipMessage implements SipServletResponse
{
	private static final Logger LOG = Log.getLogger(SipResponse.class);
	private SipRequest _request;
	private int _status;
	private String _reason;
    private ProxyBranch _proxyBranch;
    private boolean _branchResponse = false;
	
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
				if (status > 100 && !_session.isProxy() && to.getTag() == null) // FIXME better handling proxy case (Virtual branch)
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
	
	public boolean is2xx()
	{
		return (200 <= _status && _status < 300);
	}
	
	public boolean needsContact()
	{
		return (isInvite() || isSubscribe() || isMethod(SipMethod.NOTIFY)
				|| isMethod(SipMethod.REFER) || isMethod(SipMethod.UPDATE))
				&& (getStatus() < 300);
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
		
		// Need a scope here as this method can be called outside of a managed thread
		ApplicationSessionScope scope = appSession().getSessionManager().openScope(appSession());
    	try
    	{
    		_session.sendResponse(this, reliable);
    		setCommitted(true);
    	}
    	finally
    	{
    		scope.close();
    	}		
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
	public SipServletRequest createAck() 
	{
		if (!isInvite()) 
			throw new IllegalStateException("Not INVITE method");
        
        if (_status > 100 && _status < 200)
        {   // For Sip servlet 1.0 compliance
            try
			{
				return createPrack();
			}
			catch (Rel100Exception e)
			{
				throw new IllegalStateException(e.getMessage(), e);
			}
        }
        else if (is2xx())
        {
        	setCommitted(true);
            return _session.getUa().createRequest(SipMethod.ACK, getCSeq().getNumber());
        }
        else 
            throw new IllegalStateException("non 2xx or 1xx response");
	}

	@Override
	public SipServletRequest createPrack() throws Rel100Exception 
	{
		if (isCommitted())
			throw new IllegalStateException("Already committed");
		
		if (!isInvite()) 
			throw new Rel100Exception(Rel100Exception.NOT_INVITE);
		
		if (_status > 100 && _status < 200)
        {
            long rseq = getRSeq();
            if (!isReliable1xx()) 
        		throw new Rel100Exception(Rel100Exception.NOT_100rel);
        	
            SipRequest request = (SipRequest) _session.createRequest(SipMethod.PRACK.asString()); // TODO do not use API method
            request.getFields().set(SipHeader.RACK, rseq + " " + getCSeq());
            setCommitted(true);
            return request;
        }
		else 
            throw new Rel100Exception(Rel100Exception.NOT_1XX);
	}
	
	public boolean isReliable1xx()
	{
		if (_status > 100 && _status < 200)
		{
			if (getRSeq() == -1)
				return false;
			Iterator<String> it = _fields.getValues(SipHeader.REQUIRE.asString());
			while (it.hasNext()) {
				String val = it.next();
				if (SipGrammar.REL_100.equalsIgnoreCase(val)) {
					return true;
				}
			}
            return false;
		}
		return false;
	}
	
    public void setRSeq(long rseq)
    {
        getFields().set(SipHeader.RSEQ, Long.toString(rseq));
    }
    
    public long getRSeq()
    {
        return getFields().getLong(SipHeader.RSEQ);
    }

	@Override
	public Iterator<String> getChallengeRealms() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException { return null; }

	@Override
	public Proxy getProxy()
	{
		if (_proxyBranch != null)
			return _proxyBranch.getProxy();
		return null;
	}

	@Override
	public ProxyBranch getProxyBranch()
	{
		return _proxyBranch;
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

	
	@Override
	public SipMethod getSipMethod()
	{
		if (_sipMethod == null)
		{
			String method = getMethod();
			_sipMethod = SipMethod.get(method);
		}
		return _sipMethod;
	}
	
	/**
	 * @see SipServletMessage#getMethod
	 */
	public String getMethod()
	{
		if (_method == null)
		{
			try
			{
				_method = getCSeq().getMethod();
			}
			catch (Exception e)
			{
				LOG.debug(e);
			}
		}
		return _method;
	}
	
	@Override
	public PrintWriter getWriter() throws IOException 
	{
		return null;
	}

	@Override
	public boolean isBranchResponse() 
	{
		return _branchResponse;
	}

	@Override
	public void sendReliably() throws Rel100Exception 
	{
		SipRequest request = (SipRequest) getRequest();
        if (!request.isInvite())
            throw new Rel100Exception(Rel100Exception.NOT_INVITE);
        if (_status < 101 || _status > 199)
            throw new Rel100Exception(Rel100Exception.NOT_1XX);
    
        Iterator<String> it = _request.getHeaders(SipHeader.SUPPORTED.asString());
        boolean supports100rel = false;
        
        while (it.hasNext() && !supports100rel)
        {
            String s = it.next();
            if (s.equals(SipGrammar.REL_100))
                supports100rel = true;
        }
        
        if (!supports100rel)
        {
            it = _request.getHeaders(SipHeader.REQUIRE.asString());
            while (it.hasNext() && !supports100rel)
            {
                String s = (String) it.next();
                if (s.equals(SipGrammar.REL_100))
                    supports100rel = true;
            }
        }
        
        if (!supports100rel)
            throw new Rel100Exception(Rel100Exception.NO_REQ_SUPPORT);
		try
		{
			send(true);
		}
		catch (IOException e)
		{
			LOG.warn(e);
		}
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
		 return isRegister() 
        		||(getStatus() >= 300 && getStatus() < 400) 
        		|| getStatus() == 485
        		|| (getStatus() == 200 && isMethod(SipMethod.OPTIONS));
	}
	
	@Override
	public String toString()
	{
		ByteBuffer buffer = null;
		int bufferSize = 4096 + getContentLength();
		
		while (true)
		{
			buffer = ByteBuffer.allocate(bufferSize);

			try {
				new SipGenerator().generateResponse(buffer, _status, _reason, _fields, getRawContent(), getHeaderForm());
				return new String(buffer.array(), 0, buffer.position(), StringUtil.__UTF8_CHARSET);

			}
			catch (BufferOverflowException e)
			{
				bufferSize += 4096 + getContentLength();
			}
		}
	}
	
	@Override
	public String toStringCompact()
	{
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		new SipGenerator().generateResponseLine(buffer, _status, _reason);
		Field field = getFields().getField(SipHeader.CALL_ID);
		if (field != null)
			field.putTo(buffer, HeaderForm.DEFAULT);
		return new String(buffer.array(), 0, buffer.position(), StringUtil.__UTF8_CHARSET);
	}

	public void setRequest(SipRequest request)
	{
		_request = request;
	}

	public void setBranchResponse(boolean branchResponse)
	{
		_branchResponse = branchResponse;
	}

	public void setProxyBranch(ProxyBranch proxyBranch)
	{
		_proxyBranch = proxyBranch;
	}
	
	public Via removeTopVia() 
	{
		Via via = (Via) _fields.removeFirst(SipHeader.VIA);
		return via;
	}

}
