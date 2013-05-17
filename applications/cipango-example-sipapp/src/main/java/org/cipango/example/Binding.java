// ========================================================================
// Copyright 2007-2009 NEXCOM Systems
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
package org.cipango.example;

import javax.servlet.sip.URI;


public class Binding implements Cloneable, Comparable<Binding>
{
	private String _aor;
	private URI _contact;
	private String _callId;
	private int _cseq;
	private long _absoluteExpires;
	private float _q;
	
	public Binding(String aor, URI contact)
	{
		_aor = aor;
		_contact = contact;
	}
	
	public String getAor()
	{
		return _aor;
	}
	
	public String getCallId()
	{
		return _callId;
	}
	
	public int getCseq()
	{
		return _cseq;
	}
	
	public URI getContact()
	{
		return _contact;
	}
	
	public void setCallId(String callId)
	{
		_callId = callId;
	}
	
	public void setCseq(int cseq)
	{
		_cseq = cseq;
	}
	
	public void setExpires(int expires)
	{
		_absoluteExpires = expires * 1000 + System.currentTimeMillis();
	}
	
	public int getExpires()
    {
        return (int) (_absoluteExpires - System.currentTimeMillis()) / 1000;
    }
	
	public long getAbsoluteExpires()
    {
        return _absoluteExpires;
    }

    
    public boolean isExpired()
    {
    	return (_absoluteExpires - System.currentTimeMillis()) <= 0;
    }
    
	public int compareTo(Binding b) {
		float f = b.getQ() - _q;
		if (f == 0)
			return b.hashCode() - hashCode();
		else
			return (int) (f * 1000);
	}

	public float getQ()
	{
		return _q;
	}

	public void setQ(float q)
	{
		_q = q;
	}
}
