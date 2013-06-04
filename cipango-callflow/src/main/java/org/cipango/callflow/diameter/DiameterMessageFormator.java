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
package org.cipango.callflow.diameter;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.cipango.diameter.AVP;
import org.cipango.diameter.node.DiameterAnswer;
import org.cipango.diameter.node.DiameterMessage;


public class DiameterMessageFormator
{
	private static DiameterMessageFormator __default = new DiameterMessageFormator(false);
	private static DiameterMessageFormator __pretty = new DiameterMessageFormator(true);
	
	private boolean _prettyPrint;
	
	public static DiameterMessageFormator getDefault()
    {
        return __default;
    }
	
	public static DiameterMessageFormator getPretty()
    {
        return __pretty;
    }
	
	public DiameterMessageFormator(boolean prettyPrint)
	{
		_prettyPrint = prettyPrint;
	}
		
	public Output newOutput()
	{
		return new OutputImpl();
	}


    public interface Output
    {
    	public void add(Object object);
        public void add(String name, Object value);
    }
        
    public class OutputImpl implements Output
    {
    	private StringBuilder _sb = new StringBuilder();
    	private int _indexTab;
    		
		public void add(String name, Object value)
		{
			if (value == null)
			{
				_sb.append(name).append('\n');
			} 
			else 
			{
				if (name == null && value instanceof Collection<?>)
				{
					Iterator<?> it = ((Collection<?>) value).iterator();
					while (it.hasNext())
					{
						Object val = (Object) it.next();
						add(val);
					}
				}
				else
				{
					if (_prettyPrint)
		    		{
		    			for (int i = 0; i < _indexTab; i++)
		    				_sb.append('\t');
		    			_indexTab++;
		    		}
					
					_sb.append(name);
					
					_sb.append(": ");
					
					if (value instanceof Boolean)
						_sb.append(((Boolean) value) ? 1 : 0);
					else if (value instanceof String || value.getClass().isPrimitive())
						_sb.append(value);
					else if (value instanceof Date)
						_sb.append(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'").format((Date) value));
					else if (value instanceof Collection<?>)
					{
						_sb.append('\n');
						Iterator<?> it = ((Collection<?>) value).iterator();
						while (it.hasNext())
						{
							Object val = (Object) it.next();
							add(val);
						}
						// Remove extra '\n'
						_sb.setLength(_sb.length() - 1); 
					}
					else if (value instanceof byte[])
					{
						byte[] tab = (byte[]) value;
						if (tab[0] == '<' && tab[1] == '?' && tab[2] == 'x' && tab[3] == 'm' && tab[4] == 'l')
							_sb.append(new String(tab));
						else
							_sb.append(value);
					}
					else
					{
						_sb.append(value);
					}
					
		    		if (_prettyPrint)
		    		{
		    			_indexTab--;
		    			_sb.append('\n');
		    		}
				}
			}
		}
		

		public void add(Object value)
		{
			if (value instanceof Collection<?>)
			{
				Iterator<?> it = ((Collection<?>) value).iterator();
				_indexTab++;
				while (it.hasNext())
				{
					Object val = (Object) it.next();
					add(val);
				}
				_indexTab--;
			}
			else if (value instanceof DiameterMessage)
			{
				DiameterMessage message = (DiameterMessage) value;
				StringBuilder sb = new StringBuilder();
				sb.append("[" + message.getApplicationId() + ",");
				sb.append(message.getEndToEndId() + "," + message.getHopByHopId() + "] ");
				sb.append(message.getCommand());
				if (value instanceof DiameterAnswer)
					sb.append(" / " + ((DiameterAnswer) message).getResultCode());
				add(sb.toString());				
				add(message.getAVPs());
			}
			else if (value instanceof AVP)
			{
				AVP avp = (AVP) value;
				add(avp.getType().toString(), avp.getValue());
			}
			else
			{				
				if (_prettyPrint)
	    		{
	    			for (int i = 0; i < _indexTab; i++)
	    				_sb.append('\t');
	    			_indexTab++;
	    		}
				
						
				if (value instanceof Boolean)
					_sb.append(((Boolean) value) ? 1 : 0);
				else if (value instanceof String || value.getClass().isPrimitive())
					_sb.append(value);
				else if (value instanceof Date)
					_sb.append(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'").format((Date) value));
				else
		        {
		            _sb.append(value);
		        }
	    		if (_prettyPrint)
	    		{
	    			_indexTab--;
	    			_sb.append('\n');
	    		}
			}
		}
		
		public String toString()
		{
			return _sb.toString();
		}


    }

	public boolean isPrettyPrint()
	{
		return _prettyPrint;
	}

	public void setPrettyPrint(boolean prettyPrint)
	{
		_prettyPrint = prettyPrint;
	}
}
