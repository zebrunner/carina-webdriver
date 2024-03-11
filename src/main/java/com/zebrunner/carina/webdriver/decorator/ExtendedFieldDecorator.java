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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;
import org.openqa.selenium.support.pagefactory.FieldDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.webdriver.gui.AbstractPage;
import com.zebrunner.carina.webdriver.helper.IExtendedWebElementHelper;
import com.zebrunner.carina.webdriver.locator.ExtendedElementLocator;
import com.zebrunner.carina.webdriver.locator.converter.LocatorConverter;
import com.zebrunner.carina.webdriver.locator.internal.LocatingListHandler;

public class ExtendedFieldDecorator implements FieldDecorator, IExtendedWebElementHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final ElementLocatorFactory locatorFactory;
    private final WebDriver driver;

    public ExtendedFieldDecorator(ElementLocatorFactory locatorFactory, WebDriver driver) {
        this.locatorFactory = locatorFactory;
        this.driver = driver;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object decorate(ClassLoader loader, Field field) {
        Class<?> fieldType = field.getType();
        if (!(ExtendedWebElement.class.isAssignableFrom(fieldType) || AbstractPage.class.isAssignableFrom(fieldType)
                || isDecoratableList(field))) {
            return null;
        }
        if (AbstractPage.class.isAssignableFrom(fieldType)) {
            try {
                return ConstructorUtils.invokeConstructor(fieldType, new Object[] { driver, driver },
                        new Class<?>[] { WebDriver.class, SearchContext.class });
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                ExceptionUtils.rethrow(e);
            } catch (NoSuchMethodException e) {
                // ignore
            }
            return null;
        }

        ExtendedElementLocator locator;
        try {
            locator = (ExtendedElementLocator) locatorFactory.createLocator(field);
        } catch (Exception e) {
            LOGGER.error("Exception while creating locator. Message: {}", e.getMessage(), e);
            return null;
        }
        if (locator == null) {
            return null;
        }

        if (ClassUtils.isAssignable(fieldType, ExtendedWebElement.class)) {
            try {
                ExtendedWebElement element;
                if (ConstructorUtils.getAccessibleConstructor(fieldType, WebDriver.class, SearchContext.class) != null) {
                    element = (ExtendedWebElement) ConstructorUtils.invokeConstructor(fieldType,
                            new Object[] { locator.getDriver(), locator.getSearchContext() },
                            new Class<?>[] { WebDriver.class, SearchContext.class });
                } else if (ConstructorUtils.getAccessibleConstructor(fieldType, WebDriver.class) != null) {
                    element = (ExtendedWebElement) ConstructorUtils.invokeConstructor(fieldType, new Object[] { locator.getDriver() },
                            new Class<?>[] { WebDriver.class });
                } else {
                    throw new NoSuchMethodException(
                            String.format("Could not find suitable constructor (WebDriver, SearchContext) or (WebDriver) in '%s' class.", fieldType));
                }

                element.setBy(buildConvertedBy(locator.getBy(), locator.getLocatorConverters()));
                element.setName(field.getName());

                String uuid = element.getUuid();
                List<LocatorConverter> converters = locator.getLocatorConverters();
                if (!converters.isEmpty()) {
                    LOCATOR_CONVERTERS.put(uuid, locator.getLocatorConverters());
                }
                ORIGINAL_LOCATORS.put(uuid, locator.getBy());
                return element;
            } catch (Exception e) {
                return ExceptionUtils.rethrow(e);
            }
        }

        if (List.class.isAssignableFrom(field.getType())) {
            Type listType = getListType(field);
            if (ExtendedWebElement.class.isAssignableFrom((Class<?>) listType)) {
                return Proxy.newProxyInstance(loader, new Class[] { List.class },
                        new LocatingListHandler(locator, field, (Class<?>) listType));
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
            if (!(ExtendedWebElement.class.isAssignableFrom((Class<?>) listType))) {
                return false;
            }
        } catch (ClassCastException e) {
            return false;
        }
        return true;
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
}
