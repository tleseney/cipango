package org.cipango.server;

import java.io.IOException;

public interface RequestCustomizer
{
	void customizeRequest(SipRequest request) throws IOException;
}
