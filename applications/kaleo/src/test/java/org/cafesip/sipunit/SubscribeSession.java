// ========================================================================
// Copyright 2009 NEXCOM Systems
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
package org.cafesip.sipunit;

import static junit.framework.Assert.fail;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.sip.Dialog;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.TransactionState;
import javax.sip.header.HeaderFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

public class SubscribeSession extends AbstractSession implements RequestListener
{

	private String _event;
	private List<RequestEvent> _receivedRequests = new ArrayList<RequestEvent>();
	private int _indexLastReadRequest = 0;
	private Dialog _dialog;
	
	private static int __requestTimeout = 5000;
	
	public SubscribeSession(SipPhone phone, String event)
	{
		super(phone);
		_event = event;
		_sipPhone.removeRequestListener(Request.NOTIFY, _sipPhone);
		_sipPhone.addRequestListener(Request.NOTIFY, this);
	}

	
	public Request newInitialSubscribe(int expires, String target)
	{
		if (_dialog != null)
			fail("Dialog already established. Should use newSubsequentSubscribe()");
		try
		{
			Request subscribe = newRequest(Request.SUBSCRIBE, 1, target);
			HeaderFactory hf = getHeaderFactory();
			subscribe.addHeader(hf.createEventHeader(_event));
			subscribe.addHeader(hf.createExpiresHeader(expires));
			subscribe.setHeader(_sipPhone.getContactInfo().getContactHeader());
			return subscribe;
		}
		catch (Exception e)
		{
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			throw new RuntimeException(e);
		}
	}
	
	public Request newSubsequentSubscribe(int expires)
	{
		if (_dialog == null)
			fail("Could not create subsequent request, no dialog established");
		try
		{
			Request subscribe = _dialog.createRequest(Request.SUBSCRIBE);
			HeaderFactory hf = getHeaderFactory();
			subscribe.addHeader(hf.createEventHeader(_event));
			subscribe.addHeader(hf.createExpiresHeader(expires));
			subscribe.setHeader(_sipPhone.getContactInfo().getContactHeader());
			return subscribe;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		
	}

	public synchronized void processEvent(EventObject event)
	{
		if (event instanceof RequestEvent)
        {
			RequestEvent requestEvent = (RequestEvent) event;
			if (requestEvent.getDialog() == _dialog || _dialog == null)
			{
	            _receivedRequests.add(requestEvent);
	            notifyAll();
			}
        }
	}
	
	public ServerTransaction waitForNotify()
	{
		return waitForRequest(Request.NOTIFY, true, __requestTimeout);
	}
	
    /**
     * Wait for a request.
     * @param method the method expected.
     * @param strict if <code>true</code>,  fails if received a request with the wrong method
     * else, ignore request with wrong method.
     * @param timeout
     * @return
     */
    public synchronized ServerTransaction waitForRequest(String method, boolean strict, long timeout)
    {
    	 	
    	if (!isLastRequestUncommited())
    	{
    		try { wait(timeout); } catch (Exception e) {}
    	}
    	RequestEvent event = getLastRequest();
    	if (event == null)
    		fail("No event received");
    	Request request = event.getRequest();

    	if (!isLastRequestUncommited())
    		fail("Response has been already sent for last request:\n" + event);
    	
        if (strict && !request.getMethod().equals(method))
        	fail("Received " + request.getMethod() + " when expected " + method);
                	
        while (!request.getMethod().equals(method))
        {
        	try { wait(timeout); } catch (Exception e) {}
        	event = getLastRequest();
        	request = event.getRequest();
        }
        
        ServerTransaction tx = event.getServerTransaction();

        if (tx == null)
        {
        	if (Request.ACK.equals(method))
        		return null;
        	
            try
            {
                tx = _sipPhone.getParent().getSipProvider().getNewServerTransaction(request);
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
        }
        
        return tx;    
    }

    public boolean isLastRequestUncommited()
    {
       	if (_indexLastReadRequest >= _receivedRequests.size())
       		return false;
       	
       	ServerTransaction tx = _receivedRequests.get(_indexLastReadRequest).getServerTransaction();
       	return  tx.getState() != TransactionState.COMPLETED
       			&& tx.getState() != TransactionState.TERMINATED;
    }

    public Response createResponse(int status, Request request)
    {
    	try
		{
			Response response = _sipPhone.getMessageFactory().createResponse(
			        status, request);
			return response;
		}
		catch (ParseException e)
		{
			throw new RuntimeException(e);
		}
    }
    
    @Override
    public Response waitResponse(SipTransaction trans)
    {
    	Response response = super.waitResponse(trans);
    	_dialog = trans.getClientTransaction().getDialog();
    	return response;
    }
    
    public void sendResponse(int status, ServerTransaction tx)
    {
    	try
		{
			Response response = _sipPhone.getMessageFactory().createResponse(
			        status, tx.getRequest());
			tx.sendResponse(response);
		}
		catch (Exception e)
		{
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			throw new RuntimeException(e);
		}
    }
    
    public RequestEvent getLastRequest()
    {
    	if (_receivedRequests.size() == 0)
    		return null;
    	_indexLastReadRequest = _receivedRequests.size() - 1;
    	return _receivedRequests.get(_indexLastReadRequest);
    }
    
}
