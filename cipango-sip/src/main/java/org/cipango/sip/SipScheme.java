package org.cipango.sip;

public enum SipScheme 
{
	SIP("sip"), SIPS("sips");
	
	private String _string;
	
	SipScheme(String s)
	{
		_string = s;
	}
	
	@Override
	public String toString()
	{
		return _string;
	}
}
