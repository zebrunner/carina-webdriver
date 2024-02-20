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
package com.zebrunner.carina.webdriver.locator.internal;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.zebrunner.carina.webdriver.helper.IExtendedWebElementHelper;
import com.zebrunner.carina.webdriver.locator.ImmutableUIList;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.locator.ExtendedElementLocator;
import com.zebrunner.carina.webdriver.locator.LocatorType;
import com.zebrunner.carina.webdriver.locator.LocatorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocatingListHandler implements InvocationHandler, IExtendedWebElementHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final ExtendedElementLocator locator;
    private final Class<?> clazz;
    private final Field field;

    public LocatingListHandler(ExtendedElementLocator locator, Field field, Class<?> clazz) {
        this.locator = locator;
        this.field = field;
        this.clazz = clazz;
    }

    @SuppressWarnings("unchecked")
    public Object invoke(Object object, Method method, Object[] objects) throws Throwable {
        // Hotfix for huge and expected regression in carina: we lost managed
        // time delays with lists manipulations
        // Temporary we are going to restore explicit waiter here with hardcoded
        // timeout before we find better solution
        // Pros: super fast regression issue which block UI execution
        // Cons: there is no way to manage timeouts in this places
        // if (!waitUntil(ExpectedConditions.or(ExpectedConditions.presenceOfElementLocated(by),
        // ExpectedConditions.visibilityOfElementLocated(by)))) {
        // LOGGER.error("List is not present: " + by);
        // }

        List<ExtendedWebElement> extendedElements = new ArrayList<>();
        int i = 0;
        List<WebElement> elements;
        try {
            elements = locator.getSearchContext().findElements(buildConvertedBy(locator.getBy(), locator.getLocatorConverters()));
        } catch (NoSuchElementException e) {
            LOGGER.debug("Unable to find elements: {}", e.getMessage());
            elements = List.of();
        }

        for (WebElement element : elements) {
            ExtendedWebElement extendedElement;
            try {
                if (ConstructorUtils.getAccessibleConstructor(clazz, WebDriver.class, SearchContext.class) != null) {
                    extendedElement = (ExtendedWebElement) ConstructorUtils.invokeConstructor(clazz,
                            new Object[] { locator.getDriver(), locator.getSearchContext() },
                            new Class<?>[] { WebDriver.class, SearchContext.class });
                } else if (ConstructorUtils.getAccessibleConstructor(clazz, WebDriver.class) != null) {
                    // If class inherit AbstractUIObject and contains only 'WebDriver' parameter
                    extendedElement = (ExtendedWebElement) ConstructorUtils.invokeConstructor(clazz, new Object[] { locator.getDriver() },
                            new Class<?>[] { WebDriver.class });
                } else {
                    throw new NoSuchMethodException(
                            String.format("Could not find suitable constructor (WebDriver) or (WebDriver, SearchContext) in '%s' class.", clazz));
                }
            } catch (NoSuchMethodException e) {
                LOGGER.error(
                        "Implement appropriate AbstractUIObject constructor in '{}' class for auto-initialization (WebDriver,SearchContext). Message: {}",
                        clazz.getName(),
                        e.getMessage(), e);
                return ExceptionUtils.rethrow(e);
            } catch (Exception e) {
                LOGGER.error("Exception when  creating list of '{}' elements. Message: {}", field.getName(), e.getMessage(), e);
                return ExceptionUtils.rethrow(e);
            }
            if (field.isAnnotationPresent(ImmutableUIList.class)) {
                Optional<LocatorType> locatorType = LocatorUtils.getLocatorType(buildConvertedBy(locator.getBy(), locator.getLocatorConverters()));
                if (locatorType.isPresent() && locatorType.get().isIndexSupport()) {
                    extendedElement.setBy(locatorType.get().buildLocatorWithIndex(buildConvertedBy(locator.getBy(), locator.getLocatorConverters()).toString(), i));
                } else {
                    throw new IllegalStateException(String.format("'%s' locator does not supported by '%s' annotation.", locator.getBy(),
                            ImmutableUIList.class.getSimpleName()));
                }
            }
            extendedElement.setName(field.getName() + i);
            extendedElement.setElement(element);
            extendedElements.add(extendedElement);
            i++;
        }
        try {
            return method.invoke(extendedElements, objects);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
