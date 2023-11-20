package com.zebrunner.carina.webdriver.decorator;

import com.zebrunner.carina.utils.commons.SpecialKeywords;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class ExtendedWebElementHandler implements InvocationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final SearchContext searchContext;
    private final By by;
    private WebElement element;

    public ExtendedWebElementHandler(@Nullable By by, @Nullable WebElement element, SearchContext searchContext) {
        this.by = by;
        this.element = element;
        this.searchContext = searchContext;
    }

    @Override
    public Object invoke(Object object, Method method, Object[] objects) throws Throwable {
        if (by == null) {
            if (element == null) {
                throw new IllegalStateException("By and WebElement both could not be null.");
            }
            if ("getWrappedElement".equals(method.getName())) {
                return element;
            }

            try {
                return method.invoke(element, objects);
            } catch (InvocationTargetException e) {
                // Unwrap the underlying exception
                throw e.getCause();
            }
        }

        try {
            List<WebElement> elements = searchContext.findElements(by);
            if (elements.size() == 1) {
                element = elements.get(0);
            } else if (elements.size() > 1) {
                element = elements.get(0);
                LOGGER.debug("{} elements detected by: {}", elements.size(), by.toString());
            }

            if (element == null) {
                throw new NoSuchElementException(SpecialKeywords.NO_SUCH_ELEMENT_ERROR + by);
            }
        } catch (NoSuchElementException e) {
            if ("toString".equals(method.getName())) {
                return "Proxy element for: " + by;
            }
            throw e;
        }

        if ("getWrappedElement".equals(method.getName())) {
            return element;
        }

        try {
            return method.invoke(element, objects);
        } catch (InvocationTargetException e) {
            // Unwrap the underlying exception
            throw e.getCause();
        }
    }
}
