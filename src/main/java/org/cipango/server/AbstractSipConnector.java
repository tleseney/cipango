package org.cipango.server;


import org.cipango.server.nio.UdpConnector;
import org.eclipse.jetty.util.component.ContainerLifeCycle;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Executor;

public abstract class AbstractSipConnector extends ContainerLifeCycle implements SipConnector {

    private volatile String host;
    private volatile int port;

    private final Thread[] acceptors;
    private final Executor executor;

    public AbstractSipConnector(Executor executor, int nbAcceptors) {
        this.executor = executor;
        acceptors = new Thread[nbAcceptors];
    }

    public void setHost(String host) {
        if (isRunning())
            throw new IllegalStateException("running");
        this.host = host;
    }

    public void setPort(int port) {
        if (isRunning())
            throw new IllegalStateException("running");
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Executor getExecutor() {
        return executor;
    }

    protected abstract void open() throws IOException;
    protected abstract void accept() throws IOException;

    protected void doStart() throws Exception {
        if (port <= 0)
            port = getTransport().getDefaultPort();

        if (host == null) {
            try {
                host = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                host = InetAddress.getLoopbackAddress().getHostAddress();
            }
        }
        super.doStart();

        open();

        for (int i = 0; i < acceptors.length; i++) {
            Acceptor a = new Acceptor(i);
            getExecutor().execute(a);
        }
    }

    class Acceptor implements Runnable {

        private final int id;

        private Acceptor(int id) {
            this.id = id;
        }

        public void run() {
            final Thread thread = Thread.currentThread();

            synchronized (AbstractSipConnector.this) {
                acceptors[id] = thread;
            }

            try {
                while (isRunning()) {
                    try {
                        accept();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                synchronized (AbstractSipConnector.this) {
                    acceptors[id] = null;
                }
            }
        }
    }

}
