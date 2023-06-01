package com.zebrunner.carina.webdriver.proxy.mode;

/**
 * In transparent mode, traffic is directed into a proxy at the network layer, without any client configuration required.
 * This makes transparent proxying ideal for situations where you can’t change client behaviour.
 * For more info check <a href="https://docs.mitmproxy.org/stable/concepts-modes/#transparent-proxy">doc</a>
 */
public class TransparentMode extends Mode {

    public TransparentMode() {
        super("transparent");
    }
}
