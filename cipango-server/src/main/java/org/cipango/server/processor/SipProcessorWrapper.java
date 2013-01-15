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
