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

import java.io.IOException;

import org.cipango.server.SipRequest;
import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.Session;
import org.cipango.server.session.SessionManager.ApplicationSessionScope;
import org.cipango.server.transaction.ServerTransaction;
import org.cipango.server.transaction.ServerTransactionListener;
import org.cipango.server.transaction.Transaction;

public class ScopedServerTransactionListener extends ScopedObject implements ServerTransactionListener
{

	private ServerTransactionListener _listener;
	private Session _session;
	
	public ScopedServerTransactionListener(Session session, ServerTransactionListener listener)
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
	public void handleCancel(ServerTransaction tx, SipRequest cancel) throws IOException
	{
		ApplicationSessionScope scope = openScope();
		try
		{
			_listener.handleCancel(tx, cancel);
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

}
