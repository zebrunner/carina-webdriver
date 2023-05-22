package com.zebrunner.carina.webdriver.proxy.mode;

/**
 * mitmproxy is usually used with a client that uses the proxy to access the Internet.
 * Using reverse proxy mode, you can use mitmproxy to act like a normal HTTP server.
 * For more info check <a href="https://docs.mitmproxy.org/stable/concepts-modes/#reverse-proxy">doc</a>
 */
public class ReverseMode extends Mode {

    private final String host;
    private final Integer port;

    public ReverseMode() {
        this(null, null);
    }

    public ReverseMode(String host) {
        this(host, null);
    }

    public ReverseMode(String host, Integer port) {
        super("reverse");
        this.host = host;
        this.port = port;
    }

    @Override
    public String toString() {
        String name = getName();
        if (host != null) {
            name += HOST_SYMBOL + host;
        }
        if (port != null) {
            name += PORT_SYMBOL + port;
        }
        return name;
    }
}
