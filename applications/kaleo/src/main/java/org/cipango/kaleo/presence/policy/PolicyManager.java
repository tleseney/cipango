// ========================================================================
// Copyright 2009 NEXCOM Systems
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
package org.cipango.kaleo.presence.policy;

import org.cipango.kaleo.Resource;

public interface PolicyManager
{
	public enum SubHandling
	{
		BLOCK("block", 0),
		CONFIRM("confirm", 10),
		POLITE_BLOCK("polite-block", 20),
		ALLOW("allow", 30);
		
		private String _name;
		private int _value;
		
		private SubHandling(String name, int value)
		{
			_name = name;
			_value = value;
		}

		public String getName()
		{
			return _name;
		}

		public int getValue()
		{
			return _value;
		}
	}
		
	public SubHandling getPolicy(String subscriberUri, Resource resource);
	
	public Policy getPolicy(Resource resource);
}
