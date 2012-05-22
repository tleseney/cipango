package org.cipango.server.session;

import org.cipango.server.SipRequest;
import org.cipango.server.SipServer;
import org.cipango.server.transaction.Transaction;
import org.cipango.util.TimerTask;

public interface CallSession
{
	TimerTask schedule(Runnable runnable, long delay);
	void cancel(TimerTask task);
	
	Transaction createTransaction(SipRequest request);
	Transaction getTransaction(String branch, boolean cancel, boolean server);
	void removeTransaction(Transaction transaction);
	
	ApplicationSession createApplicationSession();
	void removeApplicationSession(ApplicationSession session);
	
	boolean isDone();
	SipServer getServer();
}
