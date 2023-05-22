package com.zebrunner.carina.webdriver.proxy.mode;

/**
 * This mode will listen for incoming DNS queries and use the resolve
 * capabilities of your operation system to return an answer.
 * For more info check <a href="https://docs.mitmproxy.org/stable/concepts-modes/#dns-server">doc</a>
 */
public class DnsMode extends Mode {
    private final Integer port;

    public DnsMode() {
        this(null);
    }

    public DnsMode(Integer port) {
        super("dns");
        this.port = port;
    }

    @Override
    public String toString() {
        String name = getName();
        if (port != null) {
            name += PORT_SYMBOL + port;
        }
        return name;
    }
}
