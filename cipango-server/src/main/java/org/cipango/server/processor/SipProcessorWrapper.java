package org.cipango.server.processor;

import org.cipango.server.SipMessage;
import org.cipango.server.SipProcessor;
import org.cipango.server.SipServer;

public class SipProcessorWrapper extends AbstractSipProcessor 
{
	private SipProcessor _processor;

	public SipProcessorWrapper(SipProcessor processor)
	{
		_processor = processor;
	}
	
	public SipProcessorWrapper()
	{
		
	}
	
	public SipProcessor getProcessor()
	{
		return _processor;
	}
	
	public void setProcessor(SipProcessor processor)
	{
		_processor = processor;
	}
	
	@Override
	protected void doStart() throws Exception
	{
		if (_processor != null)
			_processor.start();
		super.doStart();
	}
	
	
	@Override
	public void setServer(SipServer server)
	{
		super.setServer(server);
		if (_processor != null)
			_processor.setServer(server);
	}
	
	public void doProcess(SipMessage message) throws Exception
	{
		if (_processor != null)
			_processor.doProcess(message);
		else 
			getServer().handle(message);
	}
}
