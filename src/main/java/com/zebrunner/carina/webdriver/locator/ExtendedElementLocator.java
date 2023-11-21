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

import static io.appium.java_client.pagefactory.utils.WebDriverUnpackUtility.getCurrentContentType;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import io.appium.java_client.pagefactory.bys.ContentType;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.pagefactory.AbstractAnnotations;
import org.openqa.selenium.support.pagefactory.ElementLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.webdriver.decorator.annotations.CaseInsensitiveXPath;
import com.zebrunner.carina.webdriver.locator.converter.LocalizeLocatorConverter;
import com.zebrunner.carina.webdriver.locator.converter.LocatorConverter;
import com.zebrunner.carina.webdriver.locator.converter.caseinsensitive.CaseInsensitiveConverter;

/**
 * The default element locator, which will lazily locate an element or an
 * element list on a page. This class is designed for use with the
 * {@link org.openqa.selenium.support.PageFactory} and understands the
 * annotations {@link org.openqa.selenium.support.FindBy} and
 * {@link org.openqa.selenium.support.CacheLookup}.
 */
public final class ExtendedElementLocator implements ElementLocator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final WebDriver driver;
    private final SearchContext searchContext;
    private final By by;
    private final LinkedList<LocatorConverter> locatorConverters = new LinkedList<>();

    /**
     * Creates a new element locator.
     *
     * @param searchContext The context to use when finding the element
     * @param field The field on the Page Object that will hold the located
     *            value
     */
    public ExtendedElementLocator(WebDriver driver, SearchContext searchContext, Field field, AbstractAnnotations annotations) {
        this.driver = driver;
        this.searchContext = searchContext;
        this.by = annotations.buildBy();

        if (LocalizeLocatorConverter.getL10nPattern().matcher(by.toString()).find()) {
            locatorConverters.add(new LocalizeLocatorConverter());
        }
        if (field.isAnnotationPresent(CaseInsensitiveXPath.class)) {
            CaseInsensitiveXPath csx = field.getAnnotation(CaseInsensitiveXPath.class);
            // [AS] do not try to use searchContext for getCurrentContentType method, because it may be a proxy and when we try to
            // get driver from it, there will be 'org.openqa.selenium.NoSuchElementException' because on this moment page is not opened,
            // so we just use driver instead
            locatorConverters.add(new CaseInsensitiveConverter(csx, ContentType.NATIVE_MOBILE_SPECIFIC.equals(getCurrentContentType(driver))));
        }
    }

    public WebElement findElement() {

        if (by == null) {
            throw new NullPointerException("By cannot be null");
        }

        List<WebElement> elements = searchContext.findElements(by);

        WebElement element = null;
        if (elements.size() == 1) {
            element = elements.get(0);
        } else if (elements.size() > 1) {
            element = elements.get(0);
            LOGGER.debug("{} elements detected by: {}", elements.size(), by);
        }

        if (element == null) {
            throw new NoSuchElementException(SpecialKeywords.NO_SUCH_ELEMENT_ERROR + by);
        }
        return element;
    }

    public List<WebElement> findElements() {
        List<WebElement> elements = null;

        try {
            elements = searchContext.findElements(by);

        } catch (NoSuchElementException e) {
            LOGGER.debug("Unable to find elements: {}", e.getMessage());
        }

        if (elements == null) {
            throw new NoSuchElementException(SpecialKeywords.NO_SUCH_ELEMENT_ERROR + by.toString());
        }

        return elements;
    }

    public WebDriver getDriver() {
        return this.driver;
    }

    public By getBy() {
        return by;
    }

    public SearchContext getSearchContext() {
        return this.searchContext;
    }

    public LinkedList<LocatorConverter> getLocatorConverters() {
        return this.locatorConverters;
    }
}
