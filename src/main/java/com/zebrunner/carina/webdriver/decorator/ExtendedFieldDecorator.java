/*******************************************************************************
 * Copyright 2020-2022 Zebrunner Inc (https://www.zebrunner.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.zebrunner.carina.webdriver.decorator;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

import com.zebrunner.carina.webdriver.gui.AbstractPage;
import com.zebrunner.carina.webdriver.helper.IExtendedWebElementHelper;
import com.zebrunner.carina.webdriver.locator.converter.LocatorConverter;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;
import org.openqa.selenium.WrapsElement;
import org.openqa.selenium.interactions.Locatable;
import org.openqa.selenium.support.pagefactory.ElementLocator;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;
import org.openqa.selenium.support.pagefactory.FieldDecorator;
import org.openqa.selenium.support.pagefactory.internal.LocatingElementHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.webdriver.gui.AbstractUIObject;
import com.zebrunner.carina.webdriver.locator.ExtendedElementLocator;
import com.zebrunner.carina.webdriver.locator.internal.AbstractUIObjectListHandler;
import com.zebrunner.carina.webdriver.locator.internal.LocatingListHandler;

public class ExtendedFieldDecorator implements FieldDecorator, IExtendedWebElementHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final ElementLocatorFactory factory;
    private final WebDriver webDriver;

    public ExtendedFieldDecorator(ElementLocatorFactory factory, WebDriver webDriver) {
        this.factory = factory;
        this.webDriver = webDriver;
    }

    /**
     * @param field page element to be decorated
     */
    public Object decorate(ClassLoader loader, Field field) {
        if (!(ExtendedWebElement.class.isAssignableFrom(field.getType()) ||
                AbstractUIObject.class.isAssignableFrom(field.getType()) ||
                isDecoratableList(field))) {
            return null;
        }
        if (AbstractPage.class.isAssignableFrom(field.getType())) {
            try {
                return ((Class<? extends AbstractPage>) field.getType()).getConstructor(WebDriver.class, SearchContext.class)
                        .newInstance(webDriver, webDriver);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                ExceptionUtils.rethrow(e);
            } catch (NoSuchMethodException e) {
                // ignore
            }
            return null;
        }

        ExtendedElementLocator locator;
        try {
            locator = (ExtendedElementLocator) factory.createLocator(field);
        } catch (Exception e) {
            LOGGER.error("Error while creating locator!", e);
            return null;
        }
        if (locator == null) {
            return null;
        }

        if (ExtendedWebElement.class.isAssignableFrom(field.getType())) {
            return generateExtendedWebElement(field, locator);
        }
        if (AbstractUIObject.class.isAssignableFrom(field.getType())) {
            return proxyForAbstractUIObject(loader, field, locator);
        }

        if (List.class.isAssignableFrom(field.getType())) {
            Type listType = getListType(field);
            if (ExtendedWebElement.class.isAssignableFrom((Class<?>) listType)) {
                return proxyForListLocator(loader, field, locator, (Class<?>) listType);
            }

            if (AbstractUIObject.class.isAssignableFrom((Class<?>) listType)) {
                return proxyForListUIObjects(loader, field, locator);
            }
        }
        return null;
    }

    private boolean isDecoratableList(Field field) {
        if (!List.class.isAssignableFrom(field.getType())) {
            return false;
        }

        Type listType = getListType(field);
        if (listType == null) {
            return false;
        }

        try {
            if (!(ExtendedWebElement.class.isAssignableFrom((Class<?>) listType) || AbstractUIObject.class.isAssignableFrom((Class<?>) listType))) {
                return false;
            }
        } catch (ClassCastException e) {
            return false;
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    protected <T extends ExtendedWebElement> T generateExtendedWebElement(Field field, ExtendedElementLocator locator) {
        try {
            Class<?> type = field.getType();
            if (ClassUtils.isAssignable(type, AbstractUIObject.class)) {
                type = ExtendedWebElement.class;
            }
            T extendedWebElement = (T) ConstructorUtils.invokeConstructor(type, locator.getDriver(), locator.getSearchContext());
            By originalBy = locator.getOriginalBy();
            String uuid = extendedWebElement.getUuid();
            LinkedList<LocatorConverter> converters = locator.getLocatorConverters();
            extendedWebElement.setLocator(buildConvertedBy(originalBy, converters));
            extendedWebElement.setName(field.getName());
            LOCATOR_CONVERTERS.putIfAbsent(uuid, converters);
            ORIGINAL_LOCATORS.putIfAbsent(uuid, originalBy);
            return extendedWebElement;
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T extends AbstractUIObject> T proxyForAbstractUIObject(ClassLoader loader, Field field, ExtendedElementLocator locator) {
        InvocationHandler handler = new LocatingElementHandler(locator);
        WebElement proxy = (WebElement) Proxy.newProxyInstance(loader,
                new Class[] { WebElement.class, WrapsElement.class, WrapsDriver.class, Locatable.class, TakesScreenshot.class },
                handler);
        Class<? extends AbstractUIObject> clazz = (Class<? extends AbstractUIObject>) field.getType();
        T uiObject;
        try {
            uiObject = (T) clazz.getConstructor(WebDriver.class, SearchContext.class).newInstance(
                    webDriver, proxy);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Implement appropriate AbstractUIObject constructor for auto-initialization!", e);
        } catch (Exception e) {
            throw new RuntimeException("Error creating UIObject!", e);
        }

        uiObject.setRootExtendedElement(generateExtendedWebElement(field, locator));
        uiObject.setName(field.getName());
        uiObject.setRootElement(proxy);
        uiObject.setRootBy(getLocatorBy(locator));
        return uiObject;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected <T extends ExtendedWebElement> List<T> proxyForListLocator(ClassLoader loader, Field field, ExtendedElementLocator locator, Class<?> clazz) {
        InvocationHandler handler = new LocatingListHandler(loader, locator, field, clazz);
        return (List<T>) Proxy.newProxyInstance(loader, new Class[] { List.class }, handler);
    }

    @SuppressWarnings("unchecked")
    protected <T extends AbstractUIObject> List<T> proxyForListUIObjects(ClassLoader loader, Field field, ExtendedElementLocator locator) {
        InvocationHandler handler = new AbstractUIObjectListHandler<T>(loader, (Class<?>) getListType(field), webDriver,
                locator, field.getName(), field);
        return (List<T>) Proxy.newProxyInstance(loader, new Class[] { List.class }, handler);
    }

    private Type getListType(Field field) {
        // Type erasure in Java isn't complete. Attempt to discover the generic
        // type of the list.
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) {
            return null;
        }

        return ((ParameterizedType) genericType).getActualTypeArguments()[0];
    }

    private By getLocatorBy(ElementLocator locator) {
        By rootBy = null;
        // TODO: get root by annotation from ElementLocator to be able to append by for those elements and reuse fluent waits
        try {
            Field byContextField = null;

            byContextField = locator.getClass().getDeclaredField("by");
            byContextField.setAccessible(true);
            rootBy = (By) byContextField.get(locator);

        } catch (NoSuchFieldException e) {
            LOGGER.error("getLocatorBy->NoSuchFieldException failure", e);
        } catch (IllegalAccessException e) {
            LOGGER.error("getLocatorBy->IllegalAccessException failure", e);
        } catch (ClassCastException e) {
            LOGGER.error("getLocatorBy->ClassCastException failure", e);
        } catch (Exception e) {
            LOGGER.error("Unable to get rootBy via reflection!", e);
        }
        return rootBy;
    }
}