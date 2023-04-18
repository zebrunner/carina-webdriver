package com.zebrunner.carina.webdriver.locator.converter;

import java.util.Objects;

import com.zebrunner.carina.webdriver.locator.LocatorType;

public class IndexedLocatorConverter implements LocatorConverter {

    private final Integer index;

    public IndexedLocatorConverter(Integer index) {
        Objects.requireNonNull(index);
        this.index = index;
    }

    @Override
    public String convert(String by) {
        if (!LocatorType.BY_XPATH.is(by)) {
            throw new IllegalArgumentException("IndexedLocatorConverter support only Xpath.");
        }
        return LocatorType.BY_XPATH.buildLocatorWithIndex(by, index).toString();
    }
}
