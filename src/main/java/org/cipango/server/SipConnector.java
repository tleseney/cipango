package org.cipango.server;

import org.eclipse.jetty.util.component.LifeCycle;

public interface SipConnector extends LifeCycle {

    Transport getTransport();
    String getHost();
    int getPort();

    void setPort(int port);
    void setHost(String host);
}
