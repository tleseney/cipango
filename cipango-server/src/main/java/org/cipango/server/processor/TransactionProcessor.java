package org.cipango.server.processor;

import org.cipango.server.SipMessage;
import org.cipango.server.SipProcessor;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.transaction.ServerTransaction;
import org.cipango.sip.Via;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class TransactionProcessor extends SipProcessorWrapper
{
	private final Logger LOG = Log.getLogger(TransactionProcessor.class);

	public TransactionProcessor(SipProcessor processor)
	{
		super(processor);
	}
	
	public void doProcess(SipMessage message)  throws Exception
	{
		if (message.isRequest())
			doProcessRequest((SipRequest) message);
		else
			doProcessResponse((SipResponse) message);
	}
	
	protected void doProcessRequest(SipRequest request) throws Exception
	{
		String branch = request.getTopVia().getBranch();
	
		if (branch == null || !branch.startsWith(Via.MAGIC_COOKIE))
		{
			// OpenSER uses 0 as branch for ACK requests
			if (!"0".equals(branch) && request.isAck())
			{
				LOG.debug("not 3261 branch: {}, dropping request", branch);
				return;
			}
		}
				
		ServerTransaction transaction = (ServerTransaction) request.getCallSession().getTransaction(branch, request.isCancel(), true);
		
		if (transaction != null)
		{
			transaction.handleRequest(request);
		}
		else
		{
			if (!request.isAck())
				request.getCallSession().createTransaction(request);
			
			// TODO add cancel processing
			super.doProcess(request);
		}
	}
	
	protected void doProcessResponse(SipResponse response)
	{
		
	}

}
