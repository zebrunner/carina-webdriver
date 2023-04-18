package com.zebrunner.carina.webdriver.locator.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.pagefactory.ElementLocator;

import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.webdriver.locator.ExtendedElementLocator;

public class AbstractUIObjectListElementHandler implements InvocationHandler {
    private WebElement element;
    private final ExtendedElementLocator locator;
    private final boolean isImmutable;
    private By listElementLocator = null;

    public AbstractUIObjectListElementHandler(WebElement element, ElementLocator locator, boolean isImmutable) {
        this.element = element;
        this.locator = (ExtendedElementLocator) locator;
        this.isImmutable = isImmutable;
    }

    public Object invoke(Object object, Method method, Object[] objects) throws Throwable {

        if ("toString".equals(method.getName())) {
            return "Proxy element for: " + element.toString();
        }

        if ("getWrappedElement".equals(method.getName())) {
            return element;
        }

        if (isImmutable) {
            if (listElementLocator == null) {
                throw new NullPointerException("listElementLocator cannot be null");
            }

            // TODO: test how findElements work for web and android
            // maybe migrate to the latest appium java driver and reuse original findElement!
            List<WebElement> elements = locator.getSearchContext().findElements(listElementLocator);

            WebElement findElement = null;
            if (elements.size() >= 1) {
                findElement = elements.get(0);
            }

            if (findElement == null) {
                throw new NoSuchElementException(SpecialKeywords.NO_SUCH_ELEMENT_ERROR + listElementLocator);
            }
            element = findElement;
        }

        try {
            return method.invoke(element, objects);
        } catch (InvocationTargetException e) {
            // Unwrap the underlying exception
            throw e.getCause();
        }
    }

    public void setByForListElement(By by) {
        this.listElementLocator = by;
    }
}
