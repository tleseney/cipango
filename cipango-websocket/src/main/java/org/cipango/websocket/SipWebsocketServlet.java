// ========================================================================
// Copyright 2011 NEXCOM Systems
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
package org.cipango.websocket;

import org.cipango.websocket.WebSocketConnector.WebSocketConnection;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class SipWebsocketServlet extends WebSocketServlet
{

//	private WebSocketConnector _connector;
//	
//	public SipWebsocketServlet(WebSocketConnector connector)
//	{
//		_connector = connector;
//	}
	
//	public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol)
//	{
//		return _connector.addConnection(request);
//	}

	@Override
	public void configure(WebSocketServletFactory factory)
	{
		factory.register(WebSocketConnection.class);
		// TODO Auto-generated method stub
		
	}

	
	
}
