package com.zebrunner.carina.webdriver.proxy.mode;

/**
 * The WireGuard mode works in the same way as transparent mode,
 * except that setup and routing client traffic to mitmproxy are different.
 * In this mode, mitmproxy runs an internal WireGuard server, which devices can be
 * connected to by using standard WireGuard client applications.
 * For more info check <a href="https://docs.mitmproxy.org/stable/concepts-modes/#wireguard-transparent-proxy">doc</a>
 */
public class WireGuardMode extends Mode {

    private final Integer port;

    public WireGuardMode() {
        this(null);
    }

    public WireGuardMode(Integer port) {
        super("wireguard");
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
