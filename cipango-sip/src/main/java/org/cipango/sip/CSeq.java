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

package org.cipango.sip;

import javax.servlet.sip.ServletParseException;

import org.cipango.util.StringUtil;

public class CSeq  implements Cloneable
{
	private long _number;
	private String _method;
	private String _cseq;
	
	public CSeq(String cseq) throws ServletParseException 
    {
		_cseq = cseq;
		parse();
	}
	
	private void parse() throws ServletParseException 
	{	
		int index = _cseq.indexOf(' ');
    	if (index < 0) 
    		throw new ServletParseException("Invalid CSeq header: " + _cseq);
    	
    	String sNumber = _cseq.substring(0, index).trim();
    	try 
    	{
    		_number = Long.parseLong(sNumber);
    	} 
    	catch (NumberFormatException _) 
    	{
    		throw new ServletParseException("Invalid CSeq number: " + sNumber);
    	}
    	
    	_method = _cseq.substring(index + 1).trim();
    	
    	if (!StringUtil.isToken(_method)) 
    		throw new ServletParseException("Invalid CSeq method: " + _method);
	}

	public long getNumber() 
	{
		return _number;
	}
	
	public String getMethod() 
	{
		return _method;
	}
	
	@Override
	public String toString() 
	{
		return _cseq;
	}

	@Override
	public Object clone() throws CloneNotSupportedException
	{
		return super.clone();
	}
}
