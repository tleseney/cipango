// ========================================================================
// Copyright 2012 NEXCOM Systems
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
package org.cipango.server.session.scoped;

import org.cipango.server.SipConnection;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.Session;
import org.cipango.server.session.SessionManager.ApplicationSessionScope;
import org.cipango.server.transaction.ClientTransactionListener;
import org.cipango.server.transaction.Transaction;

public class ScopedClientTransactionListener extends ScopedObject implements ClientTransactionListener
{

	private ClientTransactionListener _listener;
	private Session _session;
	
	public ScopedClientTransactionListener(Session session, ClientTransactionListener listener)
	{
		_listener = listener;
		_session = session;
	}
	
	@Override
	public void transactionTerminated(Transaction transaction)
	{
		ApplicationSessionScope scope = openScope();
		try
		{
			_listener.transactionTerminated(transaction);
		}
		finally
		{
			scope.close();
		}
	}

	@Override
	protected ApplicationSession getAppSession()
	{
		return _session.appSession();
	}

	@Override
	public void handleResponse(SipResponse response)
	{
		ApplicationSessionScope scope = openScope();
		try
		{
			_listener.handleResponse(response);
		}
		finally
		{
			scope.close();
		}
	}

	@Override
	public void customizeRequest(SipRequest request, SipConnection connection)
	{
		_listener.customizeRequest(request, connection);
	}

}
