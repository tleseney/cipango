// ========================================================================
// Copyright 2010 NEXCOM Systems
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
package org.cipango.test;

import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.TimerListener;
import javax.servlet.sip.annotation.SipListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SipListener
public class Listener implements TimerListener
{

	private static final Logger __logger = LoggerFactory.getLogger(Listener.class);
	
	public void timeout(ServletTimer timer)
	{
		try
		{
			Runnable r = (Runnable) timer.getInfo();
			r.run();
		}
		catch (Throwable e)
		{
			__logger.warn("Failed to handle timer " + timer.getInfo(), e);
		}
	}

}
