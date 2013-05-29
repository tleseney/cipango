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
package org.cipango.diameter.util;

import org.cipango.diameter.AVP;
import org.cipango.diameter.AVPList;
import org.cipango.diameter.api.DiameterServletAnswer;
import org.cipango.diameter.base.Accounting;
import org.cipango.diameter.ims.Zh;
import org.cipango.diameter.node.DiameterMessage;
import org.cipango.diameter.node.DiameterRequest;
import org.cipango.diameter.node.Node;
import org.eclipse.jetty.util.StringUtil;

public class PrettyPrinter implements DiameterVisitor
{
	private int _index;
	private StringBuilder _buffer;
	
	public void visit(DiameterMessage message) 
	{
		_index = 0;
		_buffer = new StringBuilder();
		_buffer.append("[appId=").append(message.getApplicationId());
		_buffer.append(",e2eId=").append(message.getEndToEndId());
		_buffer.append(",hopId=").append(message.getHopByHopId()).append("] ");
		_buffer.append(message.getCommand());
		
		if (message instanceof DiameterServletAnswer)
			_buffer.append(" / ").append(((DiameterServletAnswer) message).getResultCode());
 
		_buffer.append(StringUtil.__LINE_SEPARATOR);
	}

	public void visit(AVP<?> avp)
	{
		if (!(avp.getValue() instanceof AVPList))
		{
			for (int i = 0; i < _index; i++)
				_buffer.append("    ");
			_buffer.append(avp.getType()).append(" = ");
			
			if (avp.getValue() instanceof byte[])
			{
				byte[] tab = (byte[]) avp.getValue();
				if (tab != null && tab.length > 5 
						&& tab[0] == '<' && tab[1] == '?' && tab[2] == 'x' && tab[3] == 'm' && tab[4] == 'l')
					_buffer.append(new String(tab));
				else
					_buffer.append(tab);
			}
			else
				_buffer.append(avp.getValue());
			_buffer.append(StringUtil.__LINE_SEPARATOR);
		}
	}
	
	public void visitEnter(AVP<AVPList> avp)
	{
		_buffer.append(avp.getType() + " = ");
		_buffer.append(StringUtil.__LINE_SEPARATOR);
		_index++;
	}
	
	public void visitLeave(AVP<AVPList> avp)
	{
		_index--;
	}
	
	public String toString()
	{
		return _buffer.toString();
	}
	
	public static void main(String[] args) 
	{
		DiameterMessage message = new DiameterRequest(new Node(), Accounting.ACR, Accounting.ACCOUNTING_ORDINAL, "foo");
		message.getAVPs().add(Zh.ZH_APPLICATION_ID.getAVP());

		PrettyPrinter pp = new PrettyPrinter();
		message.accept(pp);
		System.out.println(pp);
	}
}
