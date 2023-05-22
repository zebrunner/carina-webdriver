package com.zebrunner.carina.webdriver.proxy.mode;

/**
 * In this mode, mitmproxy acts as a SOCKS5 proxy.
 * This is similar to the regular proxy mode, but using SOCKS5 instead of HTTP
 * for connection establishment with the proxy.
 * For more info check <a href="https://docs.mitmproxy.org/stable/concepts-modes/#socks-proxy">doc</a>
 */
public class SocksMode extends Mode {

    public SocksMode() {
        super("socks5");
    }
}
