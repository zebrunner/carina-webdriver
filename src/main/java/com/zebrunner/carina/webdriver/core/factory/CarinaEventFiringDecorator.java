package com.zebrunner.carina.webdriver.core.factory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.decorators.Decorated;
import org.openqa.selenium.support.events.EventFiringDecorator;
import org.openqa.selenium.support.events.WebDriverListener;

public class CarinaEventFiringDecorator<T extends WebDriver> extends EventFiringDecorator<T> {

    public CarinaEventFiringDecorator(WebDriverListener... listeners) {
        super(listeners);
    }

    public CarinaEventFiringDecorator(Class<T> targetClass, WebDriverListener... listeners) {
        super(targetClass, listeners);
    }

    @Override
    public Object onError(Decorated<?> target, Method method, Object[] args,
            InvocationTargetException e) throws Throwable {
        // Hotfix for https://bugs.chromium.org/p/chromedriver/issues/detail?id=4440
        if (e.getTargetException() instanceof WebDriverException) {
            WebDriverException exception = (WebDriverException) e.getTargetException();
            if (StringUtils.contains(exception.getMessage(), "No node with given id found")) {
                return super.onError(target, method, args,
                        new InvocationTargetException(new StaleElementReferenceException("stale element reference")));
            }
        }
        return super.onError(target, method, args, e);
    }
}
