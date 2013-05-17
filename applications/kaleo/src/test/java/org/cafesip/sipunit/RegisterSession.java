// ========================================================================
// Copyright 2008-2009 NEXCOM Systems
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.TimeoutEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipTransaction;

public class RegisterSession extends AbstractSession
{
	protected CallIdHeader _callId;
	private int _cseq = 0;
    private Request lastRegistrationRequest;
	private static int __timeout = 5000;
	
	public RegisterSession(SipPhone phone)
	{
		super(phone);
		_callId = _sipPhone.getParent().getSipProvider().getNewCallId();
	}
	
	
    /**
     * This method is used to register with the SIP proxy server that was
     * specified when this SipPhone was created. If none was specified, the
     * REGISTER message is sent using information from this SipPhone's URI
     * (address of record). In either case, if the host is not a numeric IP
     * address (w.x.y.z), DNS will be used to resolve the host name to an
     * address.
     * <p>
     * Initially, a REGISTER message is sent without any user name and password.
     * If the server returns an OK, this method returns a true value.
     * <p>
     * If any challenge is received in response to sending the REGISTER message
     * (response code UNAUTHORIZED or PROXY_AUTHENTICATION_REQUIRED), the
     * SipPhone's credentials list is checked first for the corresponding realm
     * entry. If found, the credentials list entry username and password are
     * used to form the required authorization header for resending the REGISTER
     * message to the server, and the authorization header is saved for later
     * re-use. You can clear out saved authorization headers by calling the
     * unregister() method.
     * <p>
     * If the challenging realm is not found in the SipPhone credentials list,
     * the user parameter passed to this method is examined. If it is null, this
     * method returns false. If it is not null, the user and password values
     * passed in to this method are used to respond to the challenge. The
     * credentials list is not modified by this scenario (no entry is
     * automatically added with this user, password). Also, the authorization
     * created for this registration is not saved for re-use on a later
     * registration. IE, the user/password parameters are for a one-time,
     * single-shot use only.
     * <p>
     * After responding to the challenge(s) by resending the REGISTER message,
     * this method returns a true or false value depending on the outcome as
     * indicated by the server.
     * <p>
     * If the contact parameter is null, user@hostname is used where hostname is
     * the SipStack's IP address property which defaults to
     * InetAddress.getLocalHost().getHostAddress(), and other SipStack
     * properties also apply. Otherwise, the contact parameter given is used in
     * the Registration message sent to the server.
     * <p>
     * If the expiry parameter is 0, the registration request never expires.
     * Otherwise, the duration, given in seconds, is sent to server.
     * <p>
     * This method can be called repeatedly to update the expiry or to add new
     * contacts.
     * <p>
     * This method determines the contact information for this user agent,
     * whether the registration was successful or not. If successful, the
     * contact information may have been updated by the server (such as the
     * expiry time, if not specified to this method by the caller). Once this
     * method has been called, the test program can get information about the
     * contact for this agent by calling the *MyContact*() getter methods.
     * 
     * @param user
     *            Optional - user name for authenticating with the server.
     *            Required if the server issues an authentication challenge.
     * @param password
     *            Optional - used only if the server issues an authentication
     *            challenge.
     * @param contact
     *            An URI string (ex: sip:bob@192.0.2.4), or null to use the
     *            default contact for this user agent.
     * @param expiry
     *            Expiry time in seconds, or 0 if no registration expiry.
     * @throws InvalidArgumentException 
     * @throws ParseException 
     */
    public Response register(String user, String password, String contact,
            int expiry)
    {
        	Request request = createRegister(contact, expiry);
            return sendRegistrationMessage(request, user, password);
    }
    
    public Request createRegister( String contact, int expiry)
    {
    	try
		{
			Request request = newRequest(Request.REGISTER, ++_cseq, _sipPhone.getAddress().getURI().toString());
			String host = ((SipURI) request.getRequestURI()).getHost();
			request.setRequestURI(getAddressFactory().createSipURI(null, host));
			request.setHeader(_callId);

			ContactHeader contactHeader;
			if (contact != null)
			{	
			    Address contactAddr = getAddressFactory().createAddress(contact);
			    if (!contactAddr.getURI().isSipURI()) {
			    	fail("URI " + contact + " is not a Sip URI");
			    }

			    contactHeader = getHeaderFactory().createContactHeader(contactAddr);
				if (contact.equals("*"))
				{
					contactHeader.setWildCard();
					request.addHeader(getHeaderFactory().createExpiresHeader(expiry));
				}
			}
			else
				contactHeader = _sipPhone.getContactInfo().getContactHeader();
  
			contactHeader.setExpires(expiry);
      
			request.addHeader(contactHeader);

			// include any auth information for this User Agent's registration
			// if any exists
			LinkedHashMap<String, AuthorizationHeader> auth_list = 
				_sipPhone.getAuthorizations().get(_callId.getCallId());
			if (auth_list != null)
			{
			    ArrayList<AuthorizationHeader> auth_headers = new ArrayList<AuthorizationHeader>(
			            auth_list.values());
			    Iterator<AuthorizationHeader> i = auth_headers.iterator();
			    while (i.hasNext())
			    {
			        AuthorizationHeader auth = i.next();
			        request.addHeader(auth);
			    }
			}
			else
			{
			    // create the auth list entry for this phone's registrations
			    _sipPhone.enableAuthorization(_callId.getCallId());
			}
			return request;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
    }
    
    /**
     * This method is equivalent to the other register() method with no
     * authorization parameters passed in. Call this method if no authorization
     * will be needed or after setting up the SipPhone's credentials list.
     * 
     * @param contact
     *            An URI string (ex: sip:bob@192.0.2.4)
     * @param expiry
     *            Expiry time in seconds, or 0 if no expiry.
     * @return false if registration fails or an error is encountered, true
     *         otherwise.
     */
    public Response register(String contact, int expiry)
    {
        return register(null, null, contact, expiry);
    }
    
    public Response register(int expiry)
    {
        return register(null, null, null, expiry);
    }

    /**
     * This method performs the SIP unregistration process. It returns true if
     * unregistration was successful or no unregistration was needed, and false
     * otherwise. Any authorization headers required for the last registration
     * are cleared out.
     * <p>
     * If the contact parameter is null, user@hostname is unregistered where
     * hostname is obtained by calling InetAddr.getLocalHost(). Otherwise, the
     * contact parameter value is used in the unregistration message sent to the
     * server.
     * 
     * @param contact
     *            The contact URI (ex: sip:bob@192.0.2.4) to unregister.
     * @param timeout
     *            The maximum amount of time to wait for a response, in
     *            milliseconds. Use a value of 0 to wait indefinitely.
     * 
     * @return true if the unregistration succeeded or no unregistration was
     *         needed, false otherwise.
     */
    public Response unregister(String contact)
    {
    	return register(null, null, contact, 0);

    }
    
    public Response sendRegistrationMessage(Request request, String user,
            String password)
    {
    	return sendRegistrationMessage(request, user, password, Response.OK);
    }
	
    public Response sendRegistrationMessage(Request request, int expectedResponseCode)
    {
    	return sendRegistrationMessage(request, null, null, expectedResponseCode);
    }
    
    public Response sendRegistrationMessage(Request request, String user,
	            String password, int expectedResponseCode)
	    {
        SipTransaction trans = _sipPhone.sendRequestWithTransaction(request, true, null);

        Response response = waitResponse(trans);
        int status_code = response.getStatusCode();

        while (status_code != expectedResponseCode)
        {
            if (status_code == Response.TRYING)
            {
                response = waitResponse(trans);
                status_code = response.getStatusCode();
            }
            else if ((status_code == Response.UNAUTHORIZED)
                    || (status_code == Response.PROXY_AUTHENTICATION_REQUIRED))
            {
                // modify the request to include user authorization info
                request = _sipPhone.processAuthChallenge(response, request, user, password);
                if (request == null)
                    return null;

                _cseq++;
                // clean up last transaction
                _sipPhone.clearTransaction(trans);

                // send the request again
                trans = _sipPhone.sendRequestWithTransaction(request, true, null);

                response = waitResponse(trans);
                status_code = response.getStatusCode();
            }
            else
            {
                fail("The status code " + status_code + " " + response.getReasonPhrase() +
                		" was received from the server when expected " + expectedResponseCode);
            }
        }

        lastRegistrationRequest = request;

        return response;
    }
        
    /**
     * This method returns the request sent at the last successful registration.
     * 
     * @return Returns the lastRegistrationRequest.
     */
    protected Request getLastRegistrationRequest()
    {
        return lastRegistrationRequest;
    }
    
    public boolean isRegistered()
    {
    	if (lastRegistrationRequest == null)
    		return false;
    	ContactHeader contact = (ContactHeader) lastRegistrationRequest.getHeader(ContactHeader.NAME);
    	if (contact != null)
    		return contact.getExpires() != 0;
    	ExpiresHeader expires = lastRegistrationRequest.getExpires();
    	if (expires != null && expires.getExpires() == 0)
    		return false;
    	return true;
    		
    }
    
	public int getTimeout()
	{
		return __timeout;
	}


	public static void setTimeout(int timeout)
	{
		__timeout = timeout;
	}
}
