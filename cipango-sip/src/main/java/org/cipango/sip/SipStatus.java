package org.cipango.sip;

import static javax.servlet.sip.SipServletResponse.*;


public enum SipStatus 
{
	 //private static final SipStatus[] a;
/*
    static
    {
        for (Code code : Code.values())
        {
            codeMap[code._code] = code;
        }
    }
*/
	    
	TRYING(SC_TRYING, "Trying"), 
	RINGING(SC_RINGING, "Ringing"),
	CALL_IS_BEING_FORWARDED(SC_CALL_BEING_FORWARDED, "Call Is Being Forwarded"),
	QUEUED(182, "Queued"),
	SESSION_PROGRESS(SC_SESSION_PROGRESS, "Session Progress"),
	
	OK(SC_OK, "OK"),
	ACCEPTED(SC_ACCEPTED, "Accepted"),
			
	MULTIPLE_CHOICES(SC_MULTIPLE_CHOICES,"Multiple Choices"),
	MOVED_PERMANENTLY(SC_MOVED_PERMANENTLY,"Moved Permanently"),
	MOVED_TEMPORARILY(SC_MOVED_TEMPORARILY,"Moved Temporarily"),
	USE_PROXY(SC_USE_PROXY,"Use Proxy"),
	ALTERNATE_SERVICE(SC_ALTERNATIVE_SERVICE,"Alternative Service"),
			
	BAD_REQUEST(SC_BAD_REQUEST,"Bad Request"),
	UNAUTHORIZED(0,"Unauthorized"),
	PAYEMENT_REQUIRED(0,"Payment Required"),
	FORBIDDEN(0,"Forbidden"),
	NOT_FOUND(0,"Not Found"),
	METHOD_NOT_ALLOWED(0,"Method Not Allowed"),
	NOT_ACCEPTABLE(0,"Not Acceptable"),
	PROXY_AUTHENTICATION_REQUIRED(0,"Proxy Authentication Required"),
	REQUEST_TIMEOUT(0,"Request Timeout"),
	GONE(0,"Gone"),
	CONDITIONAL_REQUEST_FAILED(0,"Conditional Request Failed"),
	REQUEST_ENTITY_TOO_LARGE(0,"Request Entity Too Large"),
	REQUEST_URI_TOO_LONG(0,"Request-URI Too Long"),
	UNSUPPORTED_MEDIA_TYPE(0,"Unsupported Media Type"),
	UNSUPPORTED_URI_SCHEME(0,"Unsupported URI Scheme"),
	UNKNOWN_RESOURCE_PRIORITY(0,"Unknown Resource-Priority"),
	BAD_EXTENSION(0,"Bad Extension"),
	EXTENSION_REQUIRED(0,"Extension Required"),
	SESSION_INTERVAL_TOO_SMALL(0,"Session Interval Too Small"),
	INTERVAL_TOO_BRIEF(0,"Interval Too Brief"),
	USE_IDENTITY_HEADER(0,"Use Identity Header"),
	PROVIDE_REFERRER_IDENTITY(0,"Provide Referrer Identity"),
	BAD_IDENTITY_INFO(0,"Bad Identity-Info"),
	UNSUPPORTED_CERTIFICATE(0,"Unsupported Certificate"),
	INVALID_IDENTITY_HEADER(0,"Invalid Identity Header"),
	TEMPORARILY_UNAVAILABLE(0,"Temporarily Unavailable"),
	CALL_TRANSACTION_DOES_NOT_EXIST(0,"Call/Transaction Does Not Exist"),
	LOOP_DETECTED(0,"Loop Detected"),
	TOO_MANY_HOPS(0,"Too Many Hops"),
	ADDRESS_IMCOMPLETE(0,"Address Incomplete"),
	AMBIGUOUS(0,"Ambiguous"),
	BUSY_HERE(0,"Busy Here"),
	REQUEST_TERMINATED(0,"Request Terminated"),
	NOT_ACCEPTABLE_HERE(0,"Not Acceptable Here"),
	BAD_EVENT(0,"Bad Event"),
	REQUEST_PENDING(0,"Request Pending"),
	UNDECIPHERABLE(0,"Undecipherable"),
	SECURITY_AGREEMENT_REQUIRED(0,"Security Agreement Required"),
			
	SERVER_INTERNAL_ERROR(SC_SERVER_INTERNAL_ERROR,"Server Internal Error"),
	NOT_IMPLEMENTED(0,"Not Implemented"),
	BAD_GATEWAY(0,"Bad Gateway"),
	SERVICE_UNAVAILABLE(0,"Service Unavailable"),
	SERVER_TIMEOUT(0,"Server Time-out"),
	VERSION_NOT_SUPPORTED(0,"Version Not Supported"),
	MESSAGE_TOO_LARGE(0,"Message Too Large"),
	PRECONDITION_FAILURE(0,"Precondition Failure"),
	
	BUSY_EVERYWHERE(0,"Busy Everywhere"),
	DECLINE(0,"Decline"),
	DOES_NOT_EXIST_ANYWHERE(0,"Does Not Exist Anywhere"),
	NOT_ACCEPTABLE_ANYWHERE(0,"Not Acceptable");

	public static final int MAX_CODE = SC_NOT_ACCEPTABLE_ANYWHERE;
	
	private static final SipStatus[] __map = new SipStatus[SC_NOT_ACCEPTABLE_ANYWHERE+1];
	
	static 
	{
		for (SipStatus status : SipStatus.values())
		{
			__map[status._code] = status;
		}
	}
	
	public static SipStatus get(int code)
	{
		if (code <= MAX_CODE)
			return __map[code];
		return null;
	}
	
	private int _code;
	private String _reason;
	
	SipStatus(int code, String reason)
	{
		_code = code;
		_reason = reason;
	}
	
	public int getCode()
	{
		return _code;
	}
	
	public String getReason()
	{
		return _reason;
	}
}
