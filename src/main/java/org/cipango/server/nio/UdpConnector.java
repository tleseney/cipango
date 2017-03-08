package org.cipango.server.nio;

import org.cipango.server.AbstractSipConnector;
import org.cipango.server.Transport;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class UdpConnector extends AbstractSipConnector {

    private static final Logger LOG = Log.getLogger(UdpConnector.class);

    public static final int MAX_DATAGRAM_SIZE = 65536;

    private volatile DatagramChannel channel;
    private InetAddress localAddress;

    public UdpConnector() {
        super(Executors.newCachedThreadPool(), 1);
    }

    public Transport getTransport() {
        return Transport.UDP;
    }

    protected void open() throws IOException {
        channel = DatagramChannel.open();
        channel.configureBlocking(true);
        channel.socket().bind(new InetSocketAddress(InetAddress.getByName(getHost()), getPort()));

        localAddress = channel.socket().getLocalAddress();
    }

    protected void accept() throws IOException {

        if (channel != null && channel.isOpen()) {

            ByteBuffer buffer = ByteBuffer.allocate(MAX_DATAGRAM_SIZE);
            InetSocketAddress remoteAddress = (InetSocketAddress) channel.receive(buffer);

            buffer.put((byte) 65);
            buffer.flip();
            System.out.println(StandardCharsets.UTF_8.decode(buffer).toString());
        }
    }
}
