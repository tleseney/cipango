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
package org.cipango.diameter.io;

import org.cipango.diameter.AVP;
import org.cipango.diameter.node.DiameterMessage;

/**
 * Diameter codec instances.
 */
public abstract class Codecs 
{
	private Codecs() {}
	
	public static final DiameterCodec<AVP<?>> __avp = new AVPCodec();
	public static final DiameterCodec<DiameterMessage> __message = new MessageCodec();
}
