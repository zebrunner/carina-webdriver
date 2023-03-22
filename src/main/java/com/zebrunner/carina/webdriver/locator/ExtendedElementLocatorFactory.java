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
package com.zebrunner.carina.webdriver.locator;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.gui.AbstractUIObject;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.support.FindAll;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.pagefactory.AbstractAnnotations;
import org.openqa.selenium.support.pagefactory.ElementLocator;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.webdriver.IDriverPool;
import com.zebrunner.carina.webdriver.decorator.annotations.AccessibilityId;
import com.zebrunner.carina.webdriver.decorator.annotations.ClassChain;
import com.zebrunner.carina.webdriver.decorator.annotations.Predicate;

import io.appium.java_client.internal.CapabilityHelpers;
import io.appium.java_client.remote.MobileCapabilityType;

public final class ExtendedElementLocatorFactory implements ElementLocatorFactory, IDriverPool {
    static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final SearchContext searchContext;
    private final WebDriver webDriver;
    private final String platform;
    private final String automation;
    private final String driverType;

    private final Object pageObject;

    public <T extends AbstractUIObject>  ExtendedElementLocatorFactory(WebDriver webDriver, SearchContext searchContext, T pageObject) {
        this.webDriver = webDriver;
        this.searchContext = searchContext;
        this.pageObject = pageObject;
        if (this.webDriver instanceof HasCapabilities) {
            Capabilities capabilities = ((HasCapabilities) this.webDriver).getCapabilities();
            this.platform = CapabilityHelpers.getCapability(capabilities, CapabilityType.PLATFORM_NAME, String.class);
            this.automation = CapabilityHelpers.getCapability(capabilities, MobileCapabilityType.AUTOMATION_NAME, String.class);
            String browserName = CapabilityHelpers.getCapability(capabilities, CapabilityType.BROWSER_NAME, String.class);
            this.driverType = detectDriverType(browserName, platform);
        } else {
            LOGGER.error("Driver should realize HasCapabilities class!");
            this.platform = null;
            this.automation = null;
            this.driverType = null;
        }
    }

    public ElementLocator createLocator(Field field) {
        if (this.driverType == null) {
            return null;
        }

        AbstractAnnotations annotations = null;
        if (!SpecialKeywords.DESKTOP.equals(driverType)) {
            // todo create Annotations for every type of annotations
            if (field.isAnnotationPresent(ExtendedFindBy.class) ||
                    field.isAnnotationPresent(ClassChain.class) ||
                    field.isAnnotationPresent(AccessibilityId.class) ||
                    field.isAnnotationPresent(Predicate.class)) {
                annotations = new ExtendedAnnotations(field);
            } else {
                ExtendedAppiumAnnotations builder = new ExtendedAppiumAnnotations(platform, automation);
                builder.setAnnotated(field);
                annotations = builder;
            }
        } else if (field.getAnnotation(FindBy.class) != null ||
                    field.getAnnotation(FindBys.class) != null ||
                field.getAnnotation(FindAll.class) != null ||
                field.getAnnotation(FindAny.class) != null) {
            annotations = new ExtendedSeleniumAnnotations(field);
        }

        if (annotations == null) {
            return null;
        }

        ExtendedElementLocator extendedElementLocator = null;
        Context context = field.getAnnotation(Context.class);
        try {
            if (context == null) {
                extendedElementLocator = new ExtendedElementLocator(webDriver, searchContext, field, annotations);
            } else {
                extendedElementLocator = initLocatorWithContext(context, field, annotations);
            }
        } catch (Exception e) {
            LOGGER.debug("Cannot create extended element locator", e);
        }

        return extendedElementLocator;
    }

    private ExtendedElementLocator initLocatorWithContext(Context context, Field field, AbstractAnnotations annotations){
        Class<?> page = field.getDeclaringClass();
        Field contextField = null;
        while (contextField == null && !page.isAssignableFrom(AbstractUIObject.class)) {
            try {
                contextField = page.getDeclaredField(context.dependsOn());
            } catch (NoSuchFieldException e) {
                page = page.getSuperclass();
            }
        }

        try {
            contextField.setAccessible(true);
            ExtendedWebElement element = (ExtendedWebElement) contextField.get(pageObject);
            return new ExtendedElementLocator(webDriver, element.getElement(), field, annotations);
        } catch (NullPointerException e) {
            throw new RuntimeException("Cannot find context element: " + context.dependsOn(), e);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new RuntimeException("Cannot get context field from " + page.getName(), e);
        }
    }

    private String detectDriverType(String browserName, String platform) {
        String type = null;
        if (SpecialKeywords.ANDROID.equalsIgnoreCase(platform) ||
                SpecialKeywords.IOS.equalsIgnoreCase(platform) ||
                SpecialKeywords.TVOS.equalsIgnoreCase(platform)) {
            type = SpecialKeywords.MOBILE;
        } else if (!StringUtils.isEmpty(browserName)) {
            type = SpecialKeywords.DESKTOP;
        } else if (SpecialKeywords.WINDOWS.equalsIgnoreCase(platform)) {
            type = SpecialKeywords.WINDOWS;
        } else if (SpecialKeywords.MAC.equalsIgnoreCase(platform)) {
            type = SpecialKeywords.MAC;
        }

        if (type == null) {
            LOGGER.error("Cannot detect driver type by capabilities");
        }
        return type;
    }
}
