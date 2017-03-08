package org.cipango.server;

import org.cipango.server.nio.UdpConnector;
import org.eclipse.jetty.util.component.ContainerLifeCycle;

public class SipServer extends ContainerLifeCycle {

    private SipConnector[] connectors;

    public SipServer(int port) {

        SipConnector connector = new UdpConnector();
        connector.setPort(port);

        setConnectors(new SipConnector[] { connector });
    }

    protected void doStart() throws Exception {
        super.doStart();

        if (connectors != null) {
            for (SipConnector connector : connectors) {
                connector.start();
            }
        }
    }

    public void setConnectors(SipConnector[] connectors) {
        this.connectors = connectors;
    }

    public static void main(String[] args) throws Exception {
        new SipServer(5070).start();
    }
}
