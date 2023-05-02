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
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.annotations.Internal;
import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.webdriver.gui.AbstractUIObject;

/**
 * Represents a single element.<br>
 * It is recommended to use in cases where there is no need to redefine the logic of the AbstractUIObject class
 * and when the element does not (will not) contain nested elements.
 */
public final class ExtendedWebElement extends AbstractUIObject<ExtendedWebElement> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Internal
    public ExtendedWebElement(WebDriver driver, SearchContext searchContext) {
        super(driver, searchContext);
    }

    /**
     * Reinitialize the element according to its locator.
     * If the element is part of the list, then its update will be performed only if the locator is XPath.
     * (if the locator is not XPath in this case, an {@link UnsupportedOperationException} will be thrown in this case.
     * Also, if the current element is part of a list, it is not recommended to use it, since it can lead to
     * undefined behavior if the number of items to which the current item belongs changes.
     *
     * todo this method will not be added to the AbstractUIObject and will be removed in future releases
     * 
     * @throws NoSuchElementException if the element was not found
     * @throws UnsupportedOperationException if refreshing of the element is not supported
     */
    @Deprecated(forRemoval = true, since = "1.0.3")
    public void refresh() {
        if (getBy().isEmpty()) {
            throw new UnsupportedOperationException(
                    String.format("Refresh is not supported for this element:%n name: %s%n", getName()));
        }
        findElement();
    }

    /**
     * Find Extended Web Element on page using By starting search from this
     * object.
     * 
     * @deprecated use {@link #findNestedExtendedWebElement(By)}
     *
     * @param by Selenium By locator
     * @return ExtendedWebElement if exists otherwise null.
     */
    @Deprecated
    @Override
    public ExtendedWebElement findExtendedWebElement(By by) {
        return findExtendedWebElement(by, by.toString(), EXPLICIT_TIMEOUT);
    }

    /**
     * Find Extended Web Element on page using By starting search from this
     * object.
     * 
     * @deprecated use {@link #findNestedExtendedWebElement(By, long)}
     * @param by Selenium By locator
     * @param timeout to wait
     * @return ExtendedWebElement if exists otherwise null.
     */
    @Deprecated
    @Override
    public ExtendedWebElement findExtendedWebElement(By by, long timeout) {
        return findExtendedWebElement(by,
                DescriptionBuilder.getInstance()
                        .setClassName(ExtendedWebElement.class.getSimpleName())
                        .setContextDescription(toString())
                        .build(),
                timeout);
    }

    /**
     * Find Extended Web Element on page using By starting search from this
     * object.
     * 
     * @deprecated use {@link #findNestedExtendedWebElement(By, String)}
     * @param by Selenium By locator
     * @param name Element name
     * @return ExtendedWebElement if exists otherwise null.
     */
    @Deprecated
    @Override
    public ExtendedWebElement findExtendedWebElement(final By by, String name) {
        return findExtendedWebElement(by, name, EXPLICIT_TIMEOUT);
    }

    /**
     * Find Extended Web Element on page using By starting search from this
     * object.
     * 
     * @deprecated use {@link #findNestedExtendedWebElement(By, String, long)}
     * @param by Selenium By locator
     * @param name Element name
     * @param timeout Timeout to find
     * @return ExtendedWebElement if exists otherwise null.
     */
    @Deprecated
    @Override
    public ExtendedWebElement findExtendedWebElement(final By by, String name, long timeout) {
        ExtendedWebElement el = AbstractUIObject.Builder.getInstance()
                .setBy(by)
                .setDescriptionName(name)
                .setDriver(getDriver())
                .setSearchContext(findElement())
                .build(ExtendedWebElement.class);
        if (!el.isPresent(timeout)) {
            throw new NoSuchElementException(SpecialKeywords.NO_SUCH_ELEMENT_ERROR + by.toString());
        }
        return el;
    }

    /**
     * @deprecated use {@link #findNestedExtendedWebElements(By)}
     */
    @Deprecated
    @Override
    public List<ExtendedWebElement> findExtendedWebElements(By by) {
        return findExtendedWebElements(by, EXPLICIT_TIMEOUT);
    }

    /**
     * Get list of {@link ExtendedWebElement}s. Search of elements starts from current {@link ExtendedWebElement}
     * 
     * @deprecated use {@link #findNestedExtendedWebElements(By, long)}
     * @param by see {@link By}
     * @param timeout timeout of checking the presence of the element(s)
     * @return list of ExtendedWebElements if found, empty list otherwise
     */
    @Deprecated
    public List<ExtendedWebElement> findExtendedWebElements(final By by, long timeout) {
        List<ExtendedWebElement> extendedWebElements = new ArrayList<>();
        ExtendedWebElement tempElement = AbstractUIObject.Builder.getInstance()
                .setBy(by)
                .setDriver(getDriver())
                .setSearchContext(this)
                .build(ExtendedWebElement.class);
        if (!tempElement.isPresent(timeout)) {
            LOGGER.info("FAIL: element(s) '{}' is not found!", by);
            return extendedWebElements;
        }

        List<WebElement> webElements = findElements(by);
        int i = 0;
        for (WebElement el : webElements) {
            ExtendedWebElement extEl = AbstractUIObject.Builder.getInstance()
                    .setDescriptionName(DescriptionBuilder.getInstance()
                            .setClassName(ExtendedWebElement.class.getSimpleName())
                            .setContextDescription(toString())
                            .setIndex(String.valueOf(i))
                            .build())
                    .setDriver(getDriver())
                    .setSearchContext(this)
                    .setElement(el)
                    .build(ExtendedWebElement.class);
            extendedWebElements.add(extEl);
            i++;
        }
        return extendedWebElements;
    }

}
