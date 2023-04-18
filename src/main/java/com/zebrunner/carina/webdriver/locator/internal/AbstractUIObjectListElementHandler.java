package com.zebrunner.carina.webdriver.locator.internal;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.commons.SpecialKeywords;

public class AbstractUIObjectListElementHandler implements InvocationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final SearchContext searchContext;
    private final By xpathWithIndex;

    public AbstractUIObjectListElementHandler(SearchContext searchContext, By byXpathWithIndex) {
        this.searchContext = searchContext;
        this.xpathWithIndex = byXpathWithIndex;
    }

    public Object invoke(Object object, Method method, Object[] objects) throws Throwable {

        if ("toString".equals(method.getName())) {
            return "Proxy element for: " + xpathWithIndex.toString();
        }

        List<WebElement> elements = this.searchContext.findElements(xpathWithIndex);
            WebElement findElement = null;
            if (elements.size() >= 1) {
                findElement = elements.get(0);
            }
            if (findElement == null) {
                throw new NoSuchElementException(SpecialKeywords.NO_SUCH_ELEMENT_ERROR + xpathWithIndex);
            }

        try {
            return method.invoke(findElement, objects);
        } catch (InvocationTargetException e) {
            // Unwrap the underlying exception
            throw e.getCause();
        }
    }
}
