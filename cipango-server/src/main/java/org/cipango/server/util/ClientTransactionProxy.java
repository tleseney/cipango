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
package org.cipango.server.util;

import org.cipango.server.SipConnection;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.transaction.ClientTransaction;
import org.cipango.server.transaction.TransactionManager;

public abstract class ClientTransactionProxy implements ClientTransaction
{

	protected abstract ClientTransaction getTransaction();
	
	@Override
	public boolean isServer()
	{
		return false;
	}

	@Override
	public SipConnection getConnection()
	{
		return getTransaction().getConnection();
	}

	@Override
	public State getState()
	{
		return getTransaction().getState();
	}

	@Override
	public boolean isInvite()
	{
		return getTransaction().isInvite();
	}

	@Override
	public boolean isAck()
	{
		return getTransaction().isAck();
	}

	@Override
	public boolean isCancel()
	{
		return getTransaction().isCancel();
	}

	@Override
	public boolean isCompleted()
	{
		return getTransaction().isCompleted();
	}

	@Override
	public String getBranch()
	{
		return getTransaction().getBranch();
	}

	@Override
	public void setTransactionManager(TransactionManager manager)
	{
		getTransaction().setTransactionManager(manager);
	}

	@Override
	public SipRequest getRequest()
	{
		return getTransaction().getRequest();
	}
	
	@Override
	public void cancel(SipRequest cancel)
	{
		getTransaction().cancel(cancel);
	}


	@Override
	public boolean isCanceled()
	{
		return getTransaction().isCanceled();
	}

	@Override
	public synchronized void handleResponse(SipResponse response)
	{
		getTransaction().handleResponse(response);
	}

	@Override
	public void terminate()
	{
		getTransaction().terminate();
	}

	@Override
	public SipResponse create408()
	{
		return getTransaction().create408();
	}
	
}
