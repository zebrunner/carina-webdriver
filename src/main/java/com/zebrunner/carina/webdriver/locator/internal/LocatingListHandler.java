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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;
import org.openqa.selenium.WrapsElement;
import org.openqa.selenium.interactions.Locatable;
import org.openqa.selenium.support.pagefactory.ElementLocator;

import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.locator.ExtendedElementLocator;
import com.zebrunner.carina.webdriver.locator.LocatorType;
import com.zebrunner.carina.webdriver.locator.LocatorUtils;

public class LocatingListHandler<T extends ExtendedWebElement> implements InvocationHandler {
    private final ExtendedElementLocator locator;
    private final String name;
    private final ClassLoader loader;
    private final Class<?> clazz;

    public LocatingListHandler(ClassLoader loader, ExtendedElementLocator locator, Field field, Class<?> clazz) {
        this.loader = loader;
        this.locator = locator;
        this.name = field.getName();
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
//    	if (!waitUntil(ExpectedConditions.or(ExpectedConditions.presenceOfElementLocated(by),
//    			ExpectedConditions.visibilityOfElementLocated(by)))) {
//    		LOGGER.error("List is not present: " + by);
//    	}

        List<WebElement> elements = locator.findElements();
        By by = getLocatorBy(locator);
        Optional<LocatorType> locatorType = LocatorUtils.getLocatorType(by);
        boolean isByForListSupported = locatorType.isPresent() && locatorType.get().isIndexSupport();
        String locatorAsString = by.toString();
        List<T> extendedWebElements = null;
        int i = 0;
        if (elements != null) {
            extendedWebElements = new ArrayList<>();
            for (WebElement element : elements) {
                T extendedElement;
                try {
                    extendedElement = (T) ConstructorUtils.invokeConstructor(clazz, locator.getDriver(),
                            locator.getSearchContext());
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(
                            "Implement appropriate AbstractUIObject constructor for auto-initialization!", e);
                } catch (Exception e) {
                    throw new RuntimeException("Error creating ExtendedWebElement!", e);
                }
                if (isByForListSupported) {
                    extendedElement.setLocator(locatorType.get().buildLocatorWithIndex(locatorAsString, i));
                }
                extendedElement.setName(name + i);
                extendedElement.setElement(element);
                extendedWebElements.add(extendedElement);
                i++;
            }
        }
        try {
            return method.invoke(extendedWebElements, objects);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private By getLocatorBy(ElementLocator locator) {
        try {
            ExtendedElementLocator extendedElementLocator = (ExtendedElementLocator) locator;
            return extendedElementLocator.getBy();
        } catch (ClassCastException e) {
            throw new RuntimeException("Cannot get by from locator", e);
        }
    }

}
