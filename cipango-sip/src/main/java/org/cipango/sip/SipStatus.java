package org.cipango.sip;

import static javax.servlet.sip.SipServletResponse.*;


public enum SipStatus 
{
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
	UNAUTHORIZED(SC_UNAUTHORIZED,"Unauthorized"),
	PAYMENT_REQUIRED(SC_PAYMENT_REQUIRED,"Payment Required"),
	FORBIDDEN(SC_FORBIDDEN,"Forbidden"),
	NOT_FOUND(SC_NOT_FOUND,"Not Found"),
	METHOD_NOT_ALLOWED(SC_METHOD_NOT_ALLOWED,"Method Not Allowed"),
	NOT_ACCEPTABLE(SC_NOT_ACCEPTABLE,"Not Acceptable"),
	PROXY_AUTHENTICATION_REQUIRED(SC_PROXY_AUTHENTICATION_REQUIRED,"Proxy Authentication Required"),
	REQUEST_TIMEOUT(SC_REQUEST_TIMEOUT,"Request Timeout"),
	GONE(SC_GONE,"Gone"),
	CONDITIONAL_REQUEST_FAILED(SC_CONDITIONAL_REQUEST_FAILED,"Conditional Request Failed"),
	REQUEST_ENTITY_TOO_LARGE(SC_REQUEST_ENTITY_TOO_LARGE,"Request Entity Too Large"),
	REQUEST_URI_TOO_LONG(SC_REQUEST_URI_TOO_LONG,"Request-URI Too Long"),
	UNSUPPORTED_MEDIA_TYPE(SC_UNSUPPORTED_MEDIA_TYPE,"Unsupported Media Type"),
	UNSUPPORTED_URI_SCHEME(SC_UNSUPPORTED_URI_SCHEME,"Unsupported URI Scheme"),
	UNKNOWN_RESOURCE_PRIORITY(417,"Unknown Resource-Priority"),
	BAD_EXTENSION(SC_BAD_EXTENSION,"Bad Extension"),
	EXTENSION_REQUIRED(SC_EXTENSION_REQUIRED,"Extension Required"),
	SESSION_INTERVAL_TOO_SMALL(SC_SESSION_INTERVAL_TOO_SMALL,"Session Interval Too Small"),
	INTERVAL_TOO_BRIEF(SC_INTERVAL_TOO_BRIEF,"Interval Too Brief"),
	USE_IDENTITY_HEADER(SC_USE_IDENTITY_HEADER,"Use Identity Header"),
	PROVIDE_REFERRER_IDENTITY(SC_PROVIDE_REFERER_IDENTITY,"Provide Referrer Identity"),
	BAD_IDENTITY_INFO(SC_BAD_IDENTITY_INFO,"Bad Identity-Info"),
	UNSUPPORTED_CERTIFICATE(SC_UNSUPPORTED_CERTIFICATE,"Unsupported Certificate"),
	INVALID_IDENTITY_HEADER(SC_INVALID_IDENTITY_HEADER,"Invalid Identity Header"),
	TEMPORARILY_UNAVAILABLE(SC_TEMPORARLY_UNAVAILABLE,"Temporarily Unavailable"),
	CALL_TRANSACTION_DOES_NOT_EXIST(SC_CALL_LEG_DONE,"Call/Transaction Does Not Exist"),
	LOOP_DETECTED(SC_LOOP_DETECTED,"Loop Detected"),
	TOO_MANY_HOPS(SC_TOO_MANY_HOPS,"Too Many Hops"),
	ADDRESS_IMCOMPLETE(SC_ADDRESS_INCOMPLETE,"Address Incomplete"),
	AMBIGUOUS(SC_AMBIGUOUS,"Ambiguous"),
	BUSY_HERE(SC_BUSY_HERE,"Busy Here"),
	REQUEST_TERMINATED(SC_REQUEST_TERMINATED,"Request Terminated"),
	NOT_ACCEPTABLE_HERE(SC_NOT_ACCEPTABLE_HERE,"Not Acceptable Here"),
	BAD_EVENT(SC_BAD_EVENT,"Bad Event"),
	REQUEST_PENDING(SC_REQUEST_PENDING,"Request Pending"),
	UNDECIPHERABLE(SC_UNDECIPHERABLE,"Undecipherable"),
	SECURITY_AGREEMENT_REQUIRED(SC_SECURITY_AGREEMENT_REQUIRED,"Security Agreement Required"),
			
	SERVER_INTERNAL_ERROR(SC_SERVER_INTERNAL_ERROR,"Server Internal Error"),
	NOT_IMPLEMENTED(SC_NOT_IMPLEMENTED,"Not Implemented"),
	BAD_GATEWAY(SC_BAD_GATEWAY,"Bad Gateway"),
	SERVICE_UNAVAILABLE(SC_SERVICE_UNAVAILABLE,"Service Unavailable"),
	SERVER_TIMEOUT(SC_SERVER_TIMEOUT,"Server Time-out"),
	VERSION_NOT_SUPPORTED(SC_VERSION_NOT_SUPPORTED,"Version Not Supported"),
	MESSAGE_TOO_LARGE(SC_MESSAGE_TOO_LARGE,"Message Too Large"),
	PRECONDITION_FAILURE(SC_PRECONDITION_FAILURE,"Precondition Failure"),
	
	BUSY_EVERYWHERE(SC_BUSY_EVERYWHERE,"Busy Everywhere"),
	DECLINE(SC_DECLINE,"Decline"),
	DOES_NOT_EXIST_ANYWHERE(SC_DOES_NOT_EXIT_ANYWHERE,"Does Not Exist Anywhere"),
	NOT_ACCEPTABLE_ANYWHERE(SC_NOT_ACCEPTABLE_ANYWHERE,"Not Acceptable");

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
	
	public static boolean isSuccess(int code)
	{
		return ((200 <= code) && (code < 300));
	}
}
