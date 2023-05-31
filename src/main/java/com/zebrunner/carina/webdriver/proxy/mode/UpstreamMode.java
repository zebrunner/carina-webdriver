package com.zebrunner.carina.webdriver.proxy.mode;

/**
 * If you want to chain proxies by adding mitmproxy in front of a different proxy appliance,
 * you can use mitmproxy’s upstream mode. In upstream mode, all requests are unconditionally
 * transferred to an upstream proxy of your choice.
 * For more info check <a href="https://docs.mitmproxy.org/stable/concepts-modes/#upstream-proxy">doc</a>
 */
public class UpstreamMode extends Mode {

    private final String host;

    public UpstreamMode(String host) {
        super("upstream");
        this.host = host;
    }

    @Override
    public String toString() {
        String name = getName();
        if (host != null) {
            name += HOST_SYMBOL + host;
        }
        return name;
    }
}
