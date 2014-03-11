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
package org.cipango.dns;



public class DnsClass
{

	/**
	 * the Internet
	 */
	public static int IN = 1;
	/**
	 * the CSNET class (Obsolete - used only for examples in some obsolete RFCs)
	 * @deprecated
	 */
	public static int CS = 2;
	/**
	 * the CHAOS class
	 */
	public static int CH = 3;
	/**
	 *  Hesiod [Dyer 87]
	 */
	public static int HS = 4;
	/** 
	 * Special value used in dynamic update messages 
	 */
	public static int NONE = 254;
	/** 
	 * Matches any class 
	 */
	public static int ANY = 255;

	private DnsClass()
	{
	}
	
}
