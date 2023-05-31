package com.zebrunner.carina.webdriver.proxy;

public final class FilterCondition {

    private final String condition;

    public FilterCondition(String condition) {
        validateFilterCondition(condition);
        this.condition = condition;
    }

    @Override
    public String toString() {
        return condition;
    }

    private static void validateFilterCondition(String regex) {
        if (regex.contains("\"") || regex.contains("\'")) {
            throw new IllegalArgumentException(String.format("Filter condition '%s' should not contains \" or '", regex));
        }
    }
}
