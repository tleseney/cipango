package org.cipango.sip;

import org.eclipse.jetty.util.StringMap;

public enum SipMethod 
{
	ACK, 
	BYE, 
	CANCEL, 
	INFO, 
	INVITE, 
	MESSAGE, 
	NOTIFY, 
	OPTIONS, 
	PRACK, 
	PUBLISH, 
	REFER, 
	REGISTER, 
	SUBSCRIBE, 
	UPDATE;
	
	public static final StringMap<SipMethod> CACHE = new StringMap<SipMethod>(true);
	
	static 
	{
		for (SipMethod method : SipMethod.values())
			CACHE.put(method.toString(), method);
	}
}
