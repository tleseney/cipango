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
package org.cipango.server.transaction;

import java.io.IOException;

public class ClientTxException extends IOException
{
	private static final long serialVersionUID = 1L;
	
	private ClientTransaction _tx;
	
	public ClientTxException(Exception source, ClientTransaction tx)
	{
		super(source);
		_tx = tx;
	}
	
	public ClientTransaction getClientTransaction()
	{
		return _tx;
	}
}
