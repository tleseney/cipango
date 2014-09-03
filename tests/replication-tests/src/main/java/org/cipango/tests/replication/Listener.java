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
package org.cipango.tests.replication;

import java.io.Serializable;

import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.TimerListener;
import javax.servlet.sip.annotation.SipListener;

@SipListener
public class Listener implements TimerListener
{

	public void timeout(ServletTimer timer)
	{
		Serializable info = timer.getInfo();
		try
		{
			if (info instanceof TimerTimeout)
			{
				((TimerTimeout) info).run(timer.getApplicationSession());
			} 
			else
				System.err.println("Unknow timeout: " + info);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
