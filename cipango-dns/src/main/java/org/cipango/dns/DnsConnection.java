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
package org.cipango.dns;

import java.io.IOException;

public interface DnsConnection
{
	void send(DnsMessage message) throws IOException;
	
	/**
	 * Returns the answser of <code>request</code> or <code>null</code> if no 
	 * answser has been received before <code>timeout</code> milliseconds. 
	 * @param timeout the timeout in milliseconds before return <code>null</code>.
	 * @return the answser of <code>request</code> or <code>null</code> if no 
	 * answser has been received before <code>timeout</code> milliseconds. 
	 */
	DnsMessage waitAnswer(DnsMessage request, int timeout);
	
	
}
