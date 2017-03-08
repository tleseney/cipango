package org.cipango.server;

public enum Transport {
    UDP("UDP", 5060, false, false, "SIP+D2U", "_sip._udp");

    private String name;
    private int defaultPort;
    private boolean reliable;
    private boolean secure;

    Transport(String name, int defaultPort, boolean reliable, boolean secure, String service, String srvPrefix) {
        this.name = name;
        this.defaultPort = defaultPort;
        this.reliable = reliable;
        this.secure = secure;
    }

    public String getName() {
        return name;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public boolean isReliable() {
        return reliable;
    }

    public boolean isSecure() {
        return secure;
    }
}
