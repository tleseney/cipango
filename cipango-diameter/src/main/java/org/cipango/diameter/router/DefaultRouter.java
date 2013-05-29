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
package org.cipango.diameter.router;

import java.util.Hashtable;
import java.util.Map;

import org.cipango.diameter.node.DiameterRequest;
import org.cipango.diameter.node.Peer;


public class DefaultRouter implements DiameterRouter
{

	private Map<String, Peer> _peers = new Hashtable<String, Peer>();
	
	public Peer getRoute(DiameterRequest request)
	{
		if (request.getDestinationHost() == null)
			return null;
		return _peers.get(request.getDestinationHost());
	}


	public void peerAdded(Peer peer)
	{
		_peers.put(peer.getHost(), peer);
	}

	public void peerRemoved(Peer peer)
	{
		_peers.remove(peer.getHost());
	}

}
