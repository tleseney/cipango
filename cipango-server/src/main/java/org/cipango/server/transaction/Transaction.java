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

import org.cipango.server.SipConnection;
import org.cipango.server.SipRequest;

public interface Transaction 
{
		
	public enum State 
	{
		UNDEFINED,
		CALLING,
		TRYING,
		PROCEEDING,
		COMPLETED,
		CONFIRMED,
		ACCEPTED,
		TERMINATED;
	}
		
	public static final int DEFAULT_T1 = 500;
	public static final int DEFAULT_T2 = 4000;
	public static final int DEFAULT_T4 = 5000;
	public static final int DEFAULT_TD = 32000;
				
	public boolean isServer();
	
	public SipConnection getConnection();
	
	public State getState();
	
	public boolean isInvite();
	
	public boolean isAck();
	
	public boolean isCancel();
	
	public boolean isCompleted();
	
	public String getBranch();
	
	public void setTransactionManager(TransactionManager manager);
		
	public SipRequest getRequest();
	
}
