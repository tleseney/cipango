// ========================================================================
// Copyright 2008-2012 NEXCOM Systems
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

public class RAck implements Cloneable
{
	private long _rseq;
	private long _number;
	private String _method;
	private String _rack;
	
	public RAck(String rack) throws ServletParseException 
    {
		_rack = rack;
		parse();
	}
	
	private void parse() throws ServletParseException 
	{	
		int index = skipSpaces(0);
		
		int index2 = _rack.indexOf(' ', index);
		
    	if (index2 < 0) 
    		throw new ServletParseException("Invalid RAck header: " + _rack);
    	
    	String s = _rack.substring(index, index2);
    	try 
    	{
    		_rseq = Long.parseLong(s);
    	} 
    	catch (NumberFormatException _) 
    	{
    		throw new ServletParseException("Invalid RSeq number: " + s);
    	}
    	
    	index = skipSpaces(index2);
    	
    	index2 = _rack.indexOf(' ', index);
    	if (index2 < 0) 
    		throw new ServletParseException("Invalid RAck header: " + _rack);
    	
    	s = _rack.substring(index, index2);
    	try 
    	{
    		_number = Long.parseLong(s);
    	} 
    	catch (NumberFormatException _) 
    	{
    		throw new ServletParseException("Invalid CSeq number: " + s);
    	}
    	
    	index = skipSpaces(index2);
    	
    	_method = _rack.substring(index).trim();

// FIXME
//		if (!SipGrammar.isToken(_method))
//			throw new ServletParseException("Invalid RAck method: " + _method);
	}

	private int skipSpaces(int start)
	{
		int i = start;
		while (_rack.charAt(i) == ' ')
			i++;
		return i;
	}

	public long getCSeq() 
	{
		return _number;
	}
	
	public String getMethod() 
	{
		return _method;
	}
	
	public long getRSeq()
	{
		return _rseq;
	}
	
	@Override
	public String toString() 
	{
		return _rack;
	}

	@Override
	public Object clone() throws CloneNotSupportedException
	{
		return super.clone();
	}
}
