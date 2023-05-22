package com.zebrunner.carina.webdriver.proxy.mode;

public abstract class Mode {

    protected static final String PORT_SYMBOL = "@";
    protected static final String HOST_SYMBOL = ":";

    protected String name;

    protected Mode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
