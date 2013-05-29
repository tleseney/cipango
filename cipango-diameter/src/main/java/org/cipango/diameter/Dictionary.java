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
package org.cipango.diameter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Contains all loaded diameter types (commands, AVP types ...)
 *
 */
public class Dictionary 
{
	private static final Logger LOG = Log.getLogger(Dictionary.class);
	
	private static final Dictionary __dictionary = new Dictionary();
	
	public static Dictionary getInstance()
	{
		return __dictionary;
	}
	
	private Map<Long, Type<?>> _types = new HashMap<Long, Type<?>>();
	private Map<Integer, DiameterCommand> _requests = new HashMap<Integer, DiameterCommand>();
	private Map<Integer, DiameterCommand> _answers = new HashMap<Integer, DiameterCommand>();	
	private Map<Long, ResultCode> _resultCodes = new HashMap<Long, ResultCode>();
	
	public DiameterCommand getRequest(int code)
	{
		return _requests.get(code);
	}
	
	public DiameterCommand getAnswer(int code)
	{
		return _answers.get(code);
	}
	
	public Type<?> getType(int vendorId, int code)
	{
		return _types.get(id(vendorId, code));
	}
	
	public ResultCode getResultCode(int vendorId, int code)
	{
		return _resultCodes.get(id(vendorId, code));
	}
	
	public void load(Class<?> clazz) 
	{
		Field [] fields = clazz.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) 
		{
			Field field = fields[i];
			if (((field.getModifiers() & Modifier.STATIC) == Modifier.STATIC)) 
			{
				try 
				{
					if (Type.class.isAssignableFrom(field.getType())) 
					{
			
						Type<?> type = (Type<?>) field.get(null);
						LOG.debug("Loaded type: " + type);
						_types.put(id(type.getVendorId(), type.getCode()), type);
					} 
					else if (DiameterCommand.class.isAssignableFrom(field.getType())) 
					{
						DiameterCommand command = (DiameterCommand) field.get(null);
						LOG.debug("Loaded command: " + command);
						if (command.isRequest()) 
							_requests.put(command.getCode(), command);
						else 
							_answers.put(command.getCode(), command);
					}
					else if (ResultCode.class.isAssignableFrom(field.getType()))
					{
						ResultCode rc = (ResultCode) field.get(null);
						LOG.debug("Loaded result code: " + rc);
						_resultCodes.put(id(rc.getVendorId(), rc.getCode()), rc);
					}
				} 
				catch (Exception e) 
				{
					LOG.warn(e);
				}
			}
		}
	}
	
	public long id(int vendorId, int code)
	{
		return (long) vendorId << 32 | code;
	}
}
