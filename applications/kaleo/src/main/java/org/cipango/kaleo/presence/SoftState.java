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

package org.cipango.kaleo.presence;

import org.cipango.kaleo.event.State;

/**
 * Soft state.
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc3903.html">RFC 3903</a>
 */
public class SoftState extends State
{
	private String _etag;
	private long _expirationTime;
	
	public SoftState(String contentType, Object content, String etag, long expirationTime)
	{
		super(contentType, content);
		_etag = etag;
		_expirationTime = expirationTime;
	}
	
	public long getExpirationTime()
	{
		return _expirationTime;
	}
	
	public void setExpirationTime(long expirationTime)
	{
		_expirationTime = expirationTime;
	}
	
	public void setETag(String etag)
	{
		_etag = etag;
	}
	
	public String getETag()
	{
		return _etag;
	}
	
	public boolean equals(Object o)
	{
		if (!(o instanceof SoftState)) return false;
		
		return ((SoftState) o).getETag().equals(_etag);
	}
	
	public String toString()
	{
		return  _etag + "= " + getContent();
	}
}
