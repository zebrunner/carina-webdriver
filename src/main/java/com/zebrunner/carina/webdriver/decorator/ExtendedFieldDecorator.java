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
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.pagefactory.ElementLocator;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;
import org.openqa.selenium.support.pagefactory.FieldDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.webdriver.decorator.annotations.CaseInsensitiveXPath;
import com.zebrunner.carina.webdriver.gui.AbstractUIObject;
import com.zebrunner.carina.webdriver.locator.ExtendedElementLocator;
import com.zebrunner.carina.webdriver.locator.LocatorUtils;
import com.zebrunner.carina.webdriver.locator.internal.AbstractUIObjectListHandler;

public class ExtendedFieldDecorator implements FieldDecorator {

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
        if (!((AbstractUIObject.class.isAssignableFrom(field.getType()) &&
        // for uiLoaderMarker and other abstract fields
        // todo add support of abstract fields which implementation will be choosed by @Device annotation
                !Modifier.isAbstract(field.getType().getModifiers())) ||
                isDecoratableList(field))) {
            return null;
        }

        ElementLocator locator;
        try {
            locator = factory.createLocator(field);
        } catch (Exception e) {
            LOGGER.error("Error while creating locator!", e);
            return null;
        }
        if (locator == null) {
            return null;
        }

        if (AbstractUIObject.class.isAssignableFrom(field.getType())) {
            return proxyForAbstractUIObject(loader, field, locator);
        }

        if (List.class.isAssignableFrom(field.getType())) {
            Type listType = getListType(field);

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
            if (!(AbstractUIObject.class.isAssignableFrom((Class<?>) listType))) {
                return false;
            }
        } catch (ClassCastException e) {
            return false;
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractUIObject<T>> T proxyForAbstractUIObject(ClassLoader loader, Field field, ElementLocator locator) {
        ExtendedElementLocator extendedElementLocator = (ExtendedElementLocator) locator;
        AbstractUIObject.Builder builder = AbstractUIObject.Builder.getInstance()
                .setBy(extendedElementLocator.getBy())
//                .setElement(proxy)
                .setDriver(extendedElementLocator.getDriver())
                .setSearchContext(extendedElementLocator.getSearchContext())
                .setDescriptionName(AbstractUIObject.DescriptionBuilder.getInstance()
                        .setFieldName(extendedElementLocator.getFieldName())
                        .setClassName(extendedElementLocator.getClassName())
                        .build());
        extendedElementLocator.getLocalizeName()
                .ifPresent(builder::setLocalizationKey);
        LocatorUtils.getL10NLocatorConverter(extendedElementLocator.getBy())
                .ifPresent(converter -> builder.getLocatorConverters().add(converter));

        LocatorUtils.getCaseInsensitiveLocatorConverter(webDriver, field.getAnnotation(CaseInsensitiveXPath.class))
                .ifPresent(converter -> builder.getLocatorConverters().addLast(converter));
        return builder.build((Class<T>) field.getType());
    }

    @SuppressWarnings("unchecked")
    protected <T extends AbstractUIObject<T>> List<T> proxyForListUIObjects(ClassLoader loader, Field field, ElementLocator locator) {
        return (List<T>) Proxy.newProxyInstance(loader,
                new Class[] { List.class },
                new AbstractUIObjectListHandler<T>(loader, (Class<T>) getListType(field), webDriver, locator, field));
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