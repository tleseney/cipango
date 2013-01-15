// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
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
package org.cipango.server.dns;

public interface BlackList
{
	/**
	 * For SIP requests, failure occurs if the transaction layer reports a 503 error response or a
	 * transport failure of some sort (generally, due to fatal ICMP errors in UDP or connection
	 * failures in TCP). Failure also occurs if the transaction layer times out without ever having
	 * received any response, provisional or final (i.e., timer B or timer F in RFC 3261 [1] fires)
	 * 
	 * @see RFC 3263 §4.3
	 */
	public enum Reason { CONNECT_FAILED, ICMP_ERROR, TIMEOUT, RESPONSE_CODE_503 }
	
	public boolean isBlacklisted(Hop hop);
	
	public void hopFailed(Hop hop, Reason reason);
	
	
}
