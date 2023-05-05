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

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.pagefactory.ElementLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.webdriver.decorator.annotations.CaseInsensitiveXPath;
import com.zebrunner.carina.webdriver.gui.AbstractUIObject;
import com.zebrunner.carina.webdriver.locator.ExtendedElementLocator;
import com.zebrunner.carina.webdriver.locator.ImmutableUIList;
import com.zebrunner.carina.webdriver.locator.LocatorType;
import com.zebrunner.carina.webdriver.locator.LocatorUtils;

public class AbstractUIObjectListHandler<T extends AbstractUIObject> implements InvocationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Class<T> clazz;
    private final WebDriver webDriver;
    private final ExtendedElementLocator locator;
    private final By locatorBy;
    private final Field field;

    public AbstractUIObjectListHandler(ClassLoader loader, Class<T> clazz, WebDriver webDriver, ElementLocator locator, Field field) {
        this.clazz = clazz;
        this.webDriver = webDriver;
        this.locator = (ExtendedElementLocator) locator;
        this.locatorBy = getLocatorBy(locator);
        this.field = field;
    }

    @SuppressWarnings("unchecked")
    public Object invoke(Object object, Method method, Object[] objects) throws Throwable {
    	
		// Hotfix for huge and expected regression in carina: we lost managed
		// time delays with lists manipulations
		// Temporary we are going to restore explicit waiter here with hardcoded
		// timeout before we find better solution
		// Pros: super fast regression issue which block UI execution
		// Cons: there is no way to manage timeouts in this places

        // #1458: AbstractUIObjectListHandler waitUntil pause
//    	waitUntil(ExpectedConditions.and(ExpectedConditions.presenceOfElementLocated(locatorBy),
//    			ExpectedConditions.visibilityOfElementLocated(locatorBy)));

        AbstractUIObject.Builder tmpBld =  AbstractUIObject.Builder.getInstance()
                .setDriver(webDriver)
                .setSearchContext(locator.getSearchContext())
                .setBy(locatorBy);

        LocatorUtils.getL10NLocatorConverter(locatorBy)
                .ifPresent(converter -> tmpBld.getLocatorConverters().add(converter));

        LocatorUtils.getCaseInsensitiveLocatorConverter(webDriver, field.getAnnotation(CaseInsensitiveXPath.class))
                .ifPresent(converter -> tmpBld.getLocatorConverters().addLast(converter));

    	List<WebElement> elements = locator.getSearchContext().findElements(tmpBld.build(clazz)
                .getBy()
                .orElseThrow());
        List<T> uIObjects = new ArrayList<>();
        int index = 0;
        if (elements != null) {
            for (WebElement element : elements) {
                AbstractUIObject.Builder builder = AbstractUIObject.Builder.getInstance()
                        .setDriver(webDriver)
                        .setSearchContext(locator.getSearchContext())
                        .setDescriptionName(AbstractUIObject.DescriptionBuilder.getInstance()
                                .setFieldName(locator.getFieldName())
                                .setClassName(clazz.getSimpleName())
                                // we cannot call toString for search context because at this moment there are no full seachContext object
                                .setContextDescription(locator.getClassName())
                                .setIndex(String.valueOf(index))
                                .build())
                        .setElement(element);

                LocatorUtils.getL10NLocatorConverter(locatorBy)
                        .ifPresent(converter -> builder.getLocatorConverters().add(converter));

                LocatorUtils.getCaseInsensitiveLocatorConverter(webDriver, field.getAnnotation(CaseInsensitiveXPath.class))
                        .ifPresent(converter -> builder.getLocatorConverters().addLast(converter));

                int finalIndex = index;
                locator.getLocalizeName().ifPresent(key -> builder.setLocalizationKey(key + finalIndex));

                if (field.isAnnotationPresent(ImmutableUIList.class)) {
                    if (!(this.locatorBy instanceof By.ByXPath)) {
                        throw new UnsupportedOperationException(
                                "Dynamic re-creation of a list item is only available for an item created with Xpath.");
                    }
                    builder.setBy(LocatorType.BY_XPATH.buildLocatorWithIndex(this.locator.getBy().toString(), index));
                }

                uIObjects.add(builder.build(clazz));
                index++;
            }
        }

        try {
            return method.invoke(uIObjects, objects);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
    
    private By getLocatorBy(ElementLocator locator) {
        //TODO: get root by annotation from ElementLocator to be able to append by for those elements and reuse fluent waits
        By rootBy = null;
        try {
            Field byContextField = null;
            byContextField = locator.getClass().getDeclaredField("by");
            byContextField.setAccessible(true);
            rootBy = (By) byContextField.get(locator);
        } catch (Exception e) {
            LOGGER.error("Error when trying to get By from locator.", e);
        }
        return rootBy;
    }
}
