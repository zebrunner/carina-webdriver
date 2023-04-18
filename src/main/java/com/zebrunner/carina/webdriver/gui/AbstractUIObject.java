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
package com.zebrunner.carina.webdriver.gui;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hamcrest.BaseMatcher;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.InvalidArgumentException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Locatable;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.zebrunner.carina.utils.Configuration;
import com.zebrunner.carina.utils.Configuration.Parameter;
import com.zebrunner.carina.utils.CryptoUtils;
import com.zebrunner.carina.utils.IWebElement;
import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.utils.messager.Messager;
import com.zebrunner.carina.utils.performance.ACTION_NAME;
import com.zebrunner.carina.utils.resources.L10N;
import com.zebrunner.carina.webdriver.AbstractContext;
import com.zebrunner.carina.webdriver.decorator.ElementLoadingStrategy;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.decorator.annotations.CaseInsensitiveXPath;
import com.zebrunner.carina.webdriver.decorator.annotations.Localized;
import com.zebrunner.carina.webdriver.listener.DriverListener;
import com.zebrunner.carina.webdriver.locator.ExtendedElementLocator;
import com.zebrunner.carina.webdriver.locator.LocatorType;
import com.zebrunner.carina.webdriver.locator.converter.FormatLocatorConverter;
import com.zebrunner.carina.webdriver.locator.converter.LocalizeLocatorConverter;
import com.zebrunner.carina.webdriver.locator.internal.AbstractUIObjectListHandler;

/**
 *
 * If 'By' of the element is not null, it will be used for element's search and recreating. Important:
 * do not add 'By' for element if it is part of the list, or it you really want to add it, use only XPath with the index. Other types
 * of locators don't support indexing.
 *
 * Broken changes:
 * 2
 * g
 * @param <T>
 */
public abstract class AbstractUIObject<T extends AbstractUIObject<T>> extends AbstractContext implements IWebElement {
    // we should keep both properties: driver and searchContext obligatory
    // driver is used for actions, javascripts execution etc
    // searchContext is used for searching element by default

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Class<T> clazz = null;
    /**
     * @deprecated will be hided
     */
    @Deprecated(forRemoval = true, since = "1.0.3")
    protected By by = null;
    private WebElement element = null;
    /**
     * @deprecated will be hided
     */
    @Deprecated(forRemoval = true, since = "1.0.3")
    protected String name = null;
    private Boolean isLocalized = null;
    private Boolean isSingleElement = null;
    private Boolean isRefreshSupport = null;
    // Converted array of objects to String for dynamic element locators
    private String formatValues = null;
    /**
     * @deprecated will be hided
     */
    @Deprecated(forRemoval = true, since = "1.0.3")
    protected ElementLoadingStrategy loadingStrategy;

    /**
     * Initializes UI object using {@link PageFactory}. Browser area for internal elements initialization is bordered by
     * SearchContext instance.
     * If {@link WebDriver} object is used as search context then whole browser window will be used for initialization
     * of {@link ExtendedWebElement} fields inside.
     * <p>
     * Note: implement this constructor if you want your {@link AbstractUIObject} instances marked with {@link FindBy}
     * to be auto-initialized on {@link AbstractPage} inheritors
     *
     * @param driver        WebDriver instance to initialize UI Object fields using PageFactory
     * @param searchContext Window area that will be used for locating of internal elements
     */
    protected AbstractUIObject(WebDriver driver, SearchContext searchContext) {
        super(driver, searchContext);
        this.loadingStrategy = ElementLoadingStrategy.valueOf(Configuration.get(Parameter.ELEMENT_LOADING_STRATEGY));
    }

    public static class Builder {

        private WebDriver driver;
        private Class<?> clazz;
        private By by = null;
        private SearchContext searchContext;
        private WebElement element;
        private String name;
        private Boolean isLocalized = false;
        private Boolean isSingleElement = true;
        private Boolean isRefreshSupport = true;
        private String formatValues = "";

        public static Builder getInstance() {
            return new Builder();
        }

        public WebDriver getDriver() {
            return driver;
        }

        public Builder setDriver(WebDriver driver) {
            this.driver = driver;
            return this;
        }

        public Class<?> getClazz() {
            return clazz;
        }

        public void setClazz(Class<?> clazz) {
            this.clazz = clazz;
        }

        public By getBy() {
            return by;
        }

        public Builder setBy(By by) {
            this.by = by;
            return this;
        }

        public SearchContext getSearchContext() {
            return searchContext;
        }

        public Builder setSearchContext(SearchContext searchContext) {
            this.searchContext = searchContext;
            return this;
        }

        public WebElement getElement() {
            return element;
        }

        public Builder setElement(WebElement element) {
            this.element = element;
            if (element instanceof Proxy) {
                try {
                    // when element proxied by ExtendedElementLocator
                    InvocationHandler innerProxy = Proxy.getInvocationHandler(element);
                    ExtendedElementLocator locator = (ExtendedElementLocator) (FieldUtils.getDeclaredField(innerProxy.getClass(), "locator", true))
                            .get(innerProxy);

                    this.isLocalized = locator.isLocalized();
                    if (isLocalized) {
                        this.name = locator.getClassName() + "." + name;
                    }

                    this.searchContext = locator.getSearchContext();
                    this.driver = locator.getDriver();

                    // TODO: identify if it is a child element and
                    // 1. get rootBy
                    // 2. append current "by" to the rootBy
                    // -> it should allow to search via regular driver and fluent waits - getBy()
                    this.by = locator.getBy();
                } catch (Exception e) {
                    throw new RuntimeException("Error when try to initialize element from proxy.");
                }
            }
            return this;
        }


        public String getName() {
            return name;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public boolean isLocalized() {
            return isLocalized;
        }

        public Builder setLocalized(boolean localized) {
            isLocalized = localized;
            return this;
        }

        public boolean isSingleElement() {
            return isSingleElement;
        }

        public Builder setSingleElement(boolean singleElement) {
            isSingleElement = singleElement;
            return this;
        }

        public boolean isRefreshSupport() {
            return isRefreshSupport;
        }

        public Builder setRefreshSupport(boolean refreshSupport) {
            isRefreshSupport = refreshSupport;
            return this;
        }

        public String getFormatValues() {
            return formatValues;
        }

        public Builder setFormatValues(String formatValues) {
            this.formatValues = formatValues;
            return this;
        }

        public <T extends AbstractUIObject<T>> T build(Class<T> clazz) {
            Objects.requireNonNull(clazz);
            validate();
            try {
                T object = ConstructorUtils.invokeConstructor(clazz, driver, searchContext);
                object.setClazz(clazz);
                // we check for null before setting value because value that setted by user has more priority
                if (object.getBy() == null) {
                    object.setBy(by);
                }
                if (object.getSearchContext() == null) {
                    object.setSearchContext(searchContext);
                }
                if (object.getElement() == null) {
                    object.setElement(element);
                }
                if (object.getName() == null) {
                    object.setName(name);
                }
                if (object.isLocalized() == null) {
                    object.setLocalized(isLocalized);
                }
                if (object.isSingleElement() == null) {
                    object.setIsSingleElement(isSingleElement);
                }
                if (object.isRefreshSupport() == null) {
                    object.setRefreshSupport(isRefreshSupport);
                }
                if (object.getFormatValues() == null) {
                    object.setFormatValues(formatValues);
                }
                return object;
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Implement appropriate AbstractUIObject constructor for auto-initialization!", e);
            } catch (Exception e) {
                throw new RuntimeException("Error creating UIObject!", e);
            }
        }

        private void validate() {
            if (this.by == null && this.element == null) {
                throw new IllegalArgumentException("By and WebElement must not be null at the same time");
            }
            if (this.name == null) {
                throw new IllegalArgumentException("Name must not be null.");
            }
        }
    }

    // --------------------------------------------------------------------------
    // Getters / Setters
    // --------------------------------------------------------------------------

    public Class<T> getClazz() {
        return clazz;
    }

    protected void setClazz(Class<T> clazz) {
        this.clazz = clazz;
    }

    public By getBy() {
        return by;
    }

    protected void setBy(By by) {
        this.by = by;
    }

    public WebElement getElement() {
        return this.element;
    }

    protected void setElement(WebElement element) {
        this.element = element;
    }

    @Override
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public Boolean isLocalized() {
        return isLocalized;
    }

    protected void setLocalized(boolean localized) {
        isLocalized = localized;
    }

    public Boolean isSingleElement() {
        return isSingleElement;
    }

    protected void setIsSingleElement(boolean singleElement) {
        isSingleElement = singleElement;
    }

    public Boolean isRefreshSupport() {
        return isRefreshSupport;
    }

    protected void setRefreshSupport(boolean refreshSupport) {
        isRefreshSupport = refreshSupport;
    }

    public String getFormatValues() {
        return formatValues;
    }

    protected void setFormatValues(String formatValues) {
        this.formatValues = formatValues;
    }

    public ElementLoadingStrategy getLoadingStrategy() {
        return loadingStrategy;
    }

    protected void setLoadingStrategy(ElementLoadingStrategy loadingStrategy) {
        this.loadingStrategy = loadingStrategy;
    }

    @Override
    public String toString() {
        return name;
    }

    // --------------------------------------------------------------------------
    // Methods for checking presence / visibility of the element
    // --------------------------------------------------------------------------

    /**
     * Check the presence of the {@link AbstractUIObject<T>} on the page.
     *
     * @throws AssertionError f the element is not present on the page
     */
    public void assertUIObjectPresent() {
        assertUIObjectPresent(EXPLICIT_TIMEOUT);
    }

    /**
     * Check the presence of the {@link AbstractUIObject<T>} on the page.
     *
     * @param timeout timeout, in seconds
     * @throws AssertionError f the element is not present on the page
     */
    public void assertUIObjectPresent(long timeout) {
        if (!isUIObjectPresent(timeout)) {
            Assert.fail(Messager.UI_OBJECT_NOT_PRESENT.getMessage(getNameWithLocator()));
        }
    }

    /**
     * Verify if the element is present on the page.
     *
     * @deprecated use {@link #isPresent(long)} instead
     * @param timeout timeout, in seconds
     * @return true if element present on the page, false otherwise
     */
    @Deprecated(forRemoval = true, since = "1.0.1")
    public boolean isUIObjectPresent(long timeout) {
        return isPresent(timeout);
    }

    /**
     * Checks missing of UIObject root element on the page and throws Assertion error in case if it presents
     *
     * todo update comment
     */
    public void assertUIObjectNotPresent() {
        assertUIObjectNotPresent(EXPLICIT_TIMEOUT);
    }

    /**
     * Checks missing of UIObject root element on the page and throws Assertion error in case if it presents
     *
     * todo update comment
     *
     * @param timeout long
     */
    public void assertUIObjectNotPresent(long timeout) {
        if (isUIObjectPresent(timeout)) {
            Assert.fail(Messager.UI_OBJECT_PRESENT.getMessage(getNameWithLocator()));
        }
    }

    /**
     * Check that element present or visible.
     **
     * @return element presence status.
     */
    public boolean isPresent() {
        return isPresent(EXPLICIT_TIMEOUT);
    }

    /**
     * Check that element present or visible within specified timeout.
     *
     * @param timeout timeout.
     * @return element existence status.
     */
    public boolean isPresent(long timeout) {
        boolean res = false;
        try {
            res = waitUntil(getDefaultCondition(), timeout);
        } catch (StaleElementReferenceException e) {
            // there is no sense to continue as StaleElementReferenceException captured
            LOGGER.debug("waitUntil: StaleElementReferenceException", e);
        }
        return res;
    }

    /**
     * Check that element with By present within specified timeout.
     *
     * @deprecated method with incorrect parameters - it should not have by as parameter
     * @param by By.
     * @param timeout timeout.
     * @return element existence status.
     */
    @Deprecated(forRemoval = true, since = "1.0.3")
    public boolean isPresent(By by, long timeout) {
        boolean res = false;
        try {
            res = waitUntil(getDefaultCondition(), timeout);
        } catch (StaleElementReferenceException e) {
            // there is no sense to continue as StaleElementReferenceException captured
            LOGGER.debug("waitUntil: StaleElementReferenceException", e);
        }
        return res;
    }

    /**
     * Check that element present and visible.
     *
     * @return element existence status.
     */
    public boolean isElementPresent() {
        return isElementPresent(EXPLICIT_TIMEOUT);
    }

    /**
     * Check that element present and visible within specified timeout.
     *
     * @param timeout - timeout.
     * @return element existence status.
     */
    public boolean isElementPresent(long timeout) {
        // perform at once super-fast single selenium call and only if nothing found move to waitAction
        if (element != null) {
            try {
                if (element.isDisplayed()) {
                    return true;
                }
            } catch (Exception e) {
                // do nothing as element is not found as expected here
            }
        }

        // [VD] replace presenceOfElementLocated and visibilityOf conditions by single "visibilityOfElementLocated"
        // visibilityOf: Does not check for presence of the element as the error explains it.
        // visibilityOfElementLocated: Checks to see if the element is present and also visible. To check visibility, it makes sure that the element
        // has a height and width greater than 0.
        List<ExpectedCondition<?>> conditions = new ArrayList<>();

        if (element != null) {
            conditions.add(ExpectedConditions.visibilityOf(element));
        }

        if (by != null) {
            if (getSearchContext() instanceof WebElement) {
                conditions.add(ExpectedConditions.visibilityOfNestedElementsLocatedBy((WebElement) getSearchContext(), by));
            } else {
                conditions.add(ExpectedConditions.visibilityOfElementLocated(by));
            }
        }

        if (conditions.isEmpty()) {
            throw new RuntimeException("There should be at least one ExpectedCondition!");
        }

        try {
            return waitUntil(ExpectedConditions.or(conditions.toArray(new ExpectedCondition[0])), timeout);
        } catch (StaleElementReferenceException ignore) {
            // If this ExtendedWebElement's element object is non-null and is stale or if the
            // search context is a web element (e.g. it's a nested element) that
            // is stale, then the various expected conditions can throw a stale element reference
            // exception when checking the visibility or when finding child elements.
            // In those cases we should catch the exception and return false.
            return false;
        }
    }

    /**
     * Check that element not present and not visible within specified timeout.
     *
     * @param timeout - timeout.
     * @return element existence status.
     */
    public boolean isElementNotPresent(long timeout) {
        return !isElementPresent(timeout);
    }

    /**
     * Checks that element clickable.
     *
     * @return element click-ability status.
     */
    public boolean isClickable() {
        return isClickable(EXPLICIT_TIMEOUT);
    }

    /**
     * Check that element clickable within specified timeout.
     *
     * @param timeout - timeout.
     * @return element click-ability status.
     */
    public boolean isClickable(long timeout) {
        List<ExpectedCondition<?>> conditions = new ArrayList<>();

        if (element != null) {
            conditions.add(ExpectedConditions.elementToBeClickable(element));
        }

        if (by != null) {
            if (getSearchContext() instanceof WebElement) {
                ExpectedCondition<?> condition = new ExpectedCondition<WebElement>() {
                    @Override
                    public WebElement apply(WebDriver driver) {
                        List<WebElement> elements = ExpectedConditions.visibilityOfNestedElementsLocatedBy((WebElement) getSearchContext(), by)
                                .apply(driver);
                        try {
                            if (elements != null && !(elements.isEmpty()) && elements.get(0).isEnabled()) {
                                return elements.get(0);
                            }
                            return null;
                        } catch (StaleElementReferenceException e) {
                            return null;
                        }
                    }

                    @Override
                    public String toString() {
                        return "element to be clickable: " + by;
                    }
                };
                conditions.add(condition);
            } else {
                conditions.add(ExpectedConditions.elementToBeClickable(by));
            }
        }

        if (conditions.isEmpty()) {
            throw new RuntimeException("There should be at least one ExpectedCondition!");
        }

        return waitUntil(ExpectedConditions.or(conditions.toArray(new ExpectedCondition[0])), timeout);
    }

    /**
     * Checks that element visible.
     *
     * @return element visibility status.
     */
    public boolean isVisible() {
        return isVisible(EXPLICIT_TIMEOUT);
    }

    /**
     * Check that element is visible within specified timeout.
     *
     * @param timeout timeout, in seconds
     * @return true if element is visible, false otherwise
     */
    public boolean isVisible(long timeout) {
        List<ExpectedCondition<?>> conditions = new ArrayList<>();
        if (element != null) {
            conditions.add(ExpectedConditions.visibilityOf(element));
        }

        if (by != null) {
            if (getSearchContext() instanceof WebElement) {
                conditions.add(ExpectedConditions.visibilityOfNestedElementsLocatedBy((WebElement) getSearchContext(), by));
            } else {
                conditions.add(ExpectedConditions.visibilityOfElementLocated(by));
            }
        }

        if (conditions.isEmpty()) {
            throw new RuntimeException("There should be at least one ExpectedCondition!");
        }

        boolean res = false;
        try {
            res = waitUntil(ExpectedConditions.or(conditions.toArray(new ExpectedCondition[0])), timeout);
        } catch (StaleElementReferenceException e) {
            // there is no sense to continue as StaleElementReferenceException captured
            LOGGER.debug("waitUntil: StaleElementReferenceException", e);
        }
        return res;
    }

    /**
     * Check that element with text present.
     *
     * @param text of element to check.
     * @return element with text existence status.
     */
    public boolean isElementWithTextPresent(final String text) {
        return isElementWithTextPresent(text, EXPLICIT_TIMEOUT);
    }

    /**
     * Check that element with text present.
     *
     * @param text of element to check.
     * @param timeout - timeout.
     * @return element with text existence status.
     */
    public boolean isElementWithTextPresent(final String text, long timeout) {
        final String decryptedText = CryptoUtils.INSTANCE.decryptIfEncrypted(text);
        List<ExpectedCondition<?>> conditions = new ArrayList<>();

        if (element != null) {
            conditions.add(ExpectedConditions.textToBePresentInElement(element, decryptedText));
        }
        if (by != null) {
            if (getSearchContext() instanceof WebElement) {
                ExpectedCondition<?> condition = new ExpectedCondition<Boolean>() {
                    @Override
                    public Boolean apply(WebDriver driver) {
                        try {
                            String elementText = getSearchContext().findElement(by).getText();
                            return elementText.contains(text);
                        } catch (StaleElementReferenceException e) {
                            return false;
                        }
                    }

                    @Override
                    public String toString() {
                        return String.format("text ('%s') to be present in element found by %s",
                                text, by);
                    }
                };

                conditions.add(condition);
            } else {
                conditions.add(ExpectedConditions.textToBePresentInElementLocated(by, decryptedText));
            }
        }

        if (conditions.isEmpty()) {
            throw new RuntimeException("There should be at least one ExpectedCondition!");
        }

        return waitUntil(ExpectedConditions.or(conditions.toArray(new ExpectedCondition[0])), timeout);
        // TODO: restore below code as only projects are migrated to "isElementWithContainTextPresent"
        // return waitUntil(ExpectedConditions.and(ExpectedConditions.presenceOfElementLocated(getBy()),
        // ExpectedConditions.textToBe(getBy(), decryptedText)), timeout);

    }

    public void assertElementWithTextPresent(final String text) {
        assertElementWithTextPresent(text, EXPLICIT_TIMEOUT);
    }

    public void assertElementWithTextPresent(final String text, long timeout) {
        if (!isElementWithTextPresent(text, timeout)) {
            Assert.fail(Messager.ELEMENT_WITH_TEXT_NOT_PRESENT.getMessage(getNameWithLocator(), text));
        }
    }

    public void assertElementPresent() {
        assertElementPresent(EXPLICIT_TIMEOUT);
    }

    public void assertElementPresent(long timeout) {
        if (!isPresent(timeout)) {
            Assert.fail(Messager.ELEMENT_NOT_PRESENT.getMessage(getNameWithLocator()));
        }
    }

    /**
     * Wait until element disappear
     *
     * @param timeout long
     * @return boolean true if element disappeared and false if still visible
     */
    public boolean waitUntilElementDisappear(final long timeout) {
        boolean res = false;
        try {
            if (element == null) {
                // if element not found it will cause NoSuchElementException
                findElement();
            }
            // if element is stale, it will cause StaleElementReferenceException
            if (element.isDisplayed()) {
                LOGGER.info("Element {} detected. Waiting until disappear...", element.getTagName());
            } else {
                LOGGER.info("Element {} is not detected, i.e. disappeared", element.getTagName());
                // no sense to continue as element is not displayed so return asap
                return true;
            }
            res = waitUntil(ExpectedConditions.or(ExpectedConditions.stalenessOf(element), ExpectedConditions.invisibilityOf(element)), timeout);
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            // element not present so means disappear
            LOGGER.debug("Element disappeared as exception catched: {}", e.getMessage());
            res = true;
        }
        return res;
    }

    /**
     * @deprecated use {@link #isPresent(long)} instead
     */
    @Deprecated(forRemoval = true, since = "1.0.1")
    public boolean isUIObjectPresent() {
        return isUIObjectPresent(Configuration.getInt(Parameter.EXPLICIT_TIMEOUT));
    }

    // --------------------------------------------------------------------------
    // Methods for interacting with the element
    // --------------------------------------------------------------------------

    /**
     * Click on element.
     */
    public void click() {
        click(EXPLICIT_TIMEOUT);
    }

    /**
     * Click on element.
     *
     * @param timeout to wait
     */
    public void click(long timeout) {
        click(timeout, getDefaultCondition());
    }

    /**
     * Click on element.
     *
     * @param timeout to wait
     * @param waitCondition to check element conditions before action
     */
    public void click(long timeout, ExpectedCondition<?> waitCondition) {
        doAction(ACTION_NAME.CLICK, timeout, waitCondition);
    }

    /**
     * Click on element by javascript.
     */
    public void clickByJs() {
        clickByJs(EXPLICIT_TIMEOUT);
    }

    /**
     * Click on element by javascript.
     *
     * @param timeout to wait
     */
    public void clickByJs(long timeout) {
        clickByJs(timeout, getDefaultCondition());
    }

    /**
     * Click on element by javascript.
     *
     * @param timeout to wait
     * @param waitCondition to check element conditions before action
     */
    public void clickByJs(long timeout, ExpectedCondition<?> waitCondition) {
        doAction(ACTION_NAME.CLICK_BY_JS, timeout, waitCondition);
    }

    /**
     * Click on element by Actions.
     */
    public void clickByActions() {
        clickByActions(EXPLICIT_TIMEOUT);
    }

    /**
     * Click on element by Actions.
     *
     * @param timeout to wait
     */
    public void clickByActions(long timeout) {
        clickByActions(timeout, getDefaultCondition());
    }

    /**
     * Click on element by Actions.
     *
     * @param timeout to wait
     * @param waitCondition to check element conditions before action
     */
    public void clickByActions(long timeout, ExpectedCondition<?> waitCondition) {
        doAction(ACTION_NAME.CLICK_BY_ACTIONS, timeout, waitCondition);
    }

    /**
     * Double Click on element.
     */
    public void doubleClick() {
        doubleClick(EXPLICIT_TIMEOUT);
    }

    /**
     * Double Click on element.
     *
     * @param timeout to wait
     */
    public void doubleClick(long timeout) {
        doubleClick(timeout, getDefaultCondition());
    }

    /**
     * Double Click on element.
     *
     * @param timeout to wait
     * @param waitCondition
     *            to check element conditions before action
     */
    public void doubleClick(long timeout, ExpectedCondition<?> waitCondition) {
        doAction(ACTION_NAME.DOUBLE_CLICK, timeout, waitCondition);
    }

    /**
     * Mouse RightClick on element.
     */
    public void rightClick() {
        rightClick(EXPLICIT_TIMEOUT);
    }

    /**
     * Mouse RightClick on element.
     *
     * @param timeout to wait
     */
    public void rightClick(long timeout) {
        rightClick(timeout, getDefaultCondition());
    }

    /**
     * Mouse RightClick on element.
     *
     * @param timeout to wait
     * @param waitCondition to check element conditions before action
     */
    public void rightClick(long timeout, ExpectedCondition<?> waitCondition) {
        doAction(ACTION_NAME.RIGHT_CLICK, timeout, waitCondition);
    }

    /**
     * MouseOver (Hover) an element.
     */
    public void hover() {
        hover(null, null);
    }

    /**
     * MouseOver (Hover) an element.
     * 
     * @param xOffset x offset for moving
     * @param yOffset y offset for moving
     */
    public void hover(Integer xOffset, Integer yOffset) {
        doAction(ACTION_NAME.HOVER, EXPLICIT_TIMEOUT, getDefaultCondition(), xOffset, yOffset);
    }

    /**
     * Click onto element if it present.
     *
     * @return boolean return true if clicked
     */
    public boolean clickIfPresent() {
        return clickIfPresent(EXPLICIT_TIMEOUT);
    }

    /**
     * Click onto element if present.
     *
     * @param timeout timeout
     * @return boolean return true if clicked
     */
    public boolean clickIfPresent(long timeout) {
        boolean present = isElementPresent(timeout);
        if (present) {
            click();
        }
        return present;
    }

    /**
     * Send Keys to element.
     *
     * @param keys Keys
     */
    public void sendKeys(Keys keys) {
        sendKeys(keys, EXPLICIT_TIMEOUT);
    }

    /**
     * Send Keys to element.
     *
     * @param keys Keys
     * @param timeout to wait
     */
    public void sendKeys(Keys keys, long timeout) {
        sendKeys(keys, timeout, getDefaultCondition());
    }

    /**
     * Send Keys to element.
     *
     * @param keys Keys
     * @param timeout to wait
     * @param waitCondition to check element conditions before action
     */
    public void sendKeys(Keys keys, long timeout, ExpectedCondition<?> waitCondition) {
        doAction(ACTION_NAME.SEND_KEYS, timeout, waitCondition, keys);
    }

    /**
     * Type text to element.
     *
     * @param text String
     */
    public void type(String text) {
        type(text, EXPLICIT_TIMEOUT);
    }

    /**
     * Type text to element.
     *
     * @param text String
     * @param timeout to wait
     */
    public void type(String text, long timeout) {
        type(text, timeout, getDefaultCondition());
    }

    /**
     * Type text to element.
     *
     * @param text String
     * @param timeout to wait
     * @param waitCondition to check element conditions before action
     */
    public void type(String text, long timeout, ExpectedCondition<?> waitCondition) {
        doAction(ACTION_NAME.TYPE, timeout, waitCondition, text);
    }

    /**
     * Scroll to element (applied only for desktop).
     * Useful for desktop with React
     */
    public void scrollTo() {
        if (Configuration.getDriverType().equals(SpecialKeywords.MOBILE)) {
            LOGGER.debug("scrollTo javascript is unsupported for mobile devices!");
            return;
        }
        try {
            Locatable locatableElement = (Locatable) this.findElement();
            // [VD] onScreen should be updated onto onPage as only 2nd one
            // returns real coordinates without scrolling... read below material
            // for details
            // https://groups.google.com/d/msg/selenium-developers/nJR5VnL-3Qs/uqUkXFw4FSwJ

            // [CB] onPage -> inViewPort
            // https://code.google.com/p/selenium/source/browse/java/client/src/org/openqa/selenium/remote/RemoteWebElement.java?r=abc64b1df10d5f5d72d11fba37fabf5e85644081
            int y = locatableElement.getCoordinates().inViewPort().getY();
            int offset = R.CONFIG.getInt("scroll_to_element_y_offset");
            ((JavascriptExecutor) getDriver()).executeScript("window.scrollBy(0," + (y - offset) + ");");
        } catch (Exception e) {
            // do nothing
        }
    }

    /*
     * Inputs file path to specified element.
     *
     * @param filePath path
     */
    public void attachFile(String filePath) {
        doAction(ACTION_NAME.ATTACH_FILE, EXPLICIT_TIMEOUT, getDefaultCondition(), filePath);
    }

    /**
     * Check checkbox
     * <p>
     * for checkbox Element
     */
    public void check() {
        doAction(ACTION_NAME.CHECK, EXPLICIT_TIMEOUT, getDefaultCondition());
    }

    /**
     * Uncheck checkbox
     * <p>
     * for checkbox Element
     */
    public void uncheck() {
        doAction(ACTION_NAME.UNCHECK, EXPLICIT_TIMEOUT, getDefaultCondition());
    }

    /**
     * Selects text in specified select element.
     *
     * @param selectText select text
     * @return true if item selected, otherwise false.
     */
    public boolean select(final String selectText) {
        return (boolean) doAction(ACTION_NAME.SELECT, EXPLICIT_TIMEOUT, getDefaultCondition(), selectText);
    }

    /**
     * Select multiple text values in specified select element.
     *
     * @param values final String[]
     * @return boolean.
     */
    public boolean select(final String[] values) {
        return (boolean) doAction(ACTION_NAME.SELECT_VALUES, EXPLICIT_TIMEOUT, getDefaultCondition(), values);
    }

    /**
     * Selects value according to text value matcher.
     *
     * @param matcher {@link} BaseMatcher
     * @return true if item selected, otherwise false.
     *         <p>
     *         Usage example: BaseMatcher&lt;String&gt; match=new
     *         BaseMatcher&lt;String&gt;() { {@literal @}Override public boolean
     *         matches(Object actual) { return actual.toString().contains(RequiredText);
     *         } {@literal @}Override public void describeTo(Description description) {
     *         } };
     */
    public boolean selectByMatcher(final BaseMatcher<String> matcher) {
        return (boolean) doAction(ACTION_NAME.SELECT_BY_MATCHER, EXPLICIT_TIMEOUT, getDefaultCondition(), matcher);
    }

    /**
     * Selects first value according to partial text value.
     *
     * @param partialSelectText select by partial text
     * @return true if item selected, otherwise false.
     */
    public boolean selectByPartialText(final String partialSelectText) {
        return (boolean) doAction(ACTION_NAME.SELECT_BY_PARTIAL_TEXT, EXPLICIT_TIMEOUT, getDefaultCondition(),
                partialSelectText);
    }

    /**
     * Selects item by index in specified select element.
     *
     * @param index to select by
     * @return true if item selected, otherwise false.
     */
    public boolean select(final int index) {
        return (boolean) doAction(ACTION_NAME.SELECT_BY_INDEX, EXPLICIT_TIMEOUT, getDefaultCondition(), index);
    }

    // --------------------------------------------------------------------------
    // Methods for getting data about element
    // --------------------------------------------------------------------------

    /**
     * Get element text.
     *
     * @return String text
     */
    public String getText() {
        return (String) doAction(ACTION_NAME.GET_TEXT, EXPLICIT_TIMEOUT, getDefaultCondition());
    }

    /**
     * Get element location.
     *
     * @return Point location
     */
    public Point getLocation() {
        return (Point) doAction(ACTION_NAME.GET_LOCATION, EXPLICIT_TIMEOUT, getDefaultCondition());
    }

    /**
     * Get element size.
     *
     * @return Dimension size
     */
    public Dimension getSize() {
        return (Dimension) doAction(ACTION_NAME.GET_SIZE, EXPLICIT_TIMEOUT, getDefaultCondition());
    }

    /**
     * Get element attribute.
     *
     * @param name of attribute
     * @return String attribute value
     */
    public String getAttribute(String name) {
        return (String) doAction(ACTION_NAME.GET_ATTRIBUTE, EXPLICIT_TIMEOUT, getDefaultCondition(), name);
    }

    /**
     * Get checkbox state.
     *
     * @return - current state
     */
    public boolean isChecked() {
        return (boolean) doAction(ACTION_NAME.IS_CHECKED, EXPLICIT_TIMEOUT, getDefaultCondition());
    }

    /**
     * Get selected elements from one-value select.
     *
     * @return selected value
     */
    public String getSelectedValue() {
        return (String) doAction(ACTION_NAME.GET_SELECTED_VALUE, EXPLICIT_TIMEOUT, getDefaultCondition());
    }

    /**
     * Get selected elements from multi-value select.
     *
     * @return selected values
     */
    @SuppressWarnings("unchecked")
    public List<String> getSelectedValues() {
        return (List<String>) doAction(ACTION_NAME.GET_SELECTED_VALUES, EXPLICIT_TIMEOUT, getDefaultCondition());
    }

    // --------------------------------------------------------------------------
    // Methods for creating new elements
    // --------------------------------------------------------------------------

    /**
     * Get element with formatted locator.<br>
     * <p>
     * 1. If element created using {@link org.openqa.selenium.support.FindBy} or same annotations:<br>
     * If parameters were passed to the method, the element will be recreated with a new locator,
     * and if the format method with parameters was already called for this element, the element locator
     * will be recreated based on the original.<br>
     * <b>All original element statuses {@link CaseInsensitiveXPath},
     * {@link Localized} are saved for new element</b>
     *
     * <p>
     * 2. If element created using constructor (it is not recommended to create element by hands):<br>
     * If parameters were passed to the method, the element will be recreated with a new locator.
     *
     * <p>
     * For all cases: if the method is called with no parameters, no locator formatting is applied, but the element will be "recreated".<br>
     *
     * <b>This method does not change the object on which it is called</b>.
     *
     * @return new {@link ExtendedWebElement} with formatted locator
     */
    public T format(Object... objects) {
        T formattedElement;
        if (element instanceof Proxy) {
            /*
             * if element created using annotation (FindBy, ExtendedFindBy and so on), it will be proxy, so we get it and re-generate locator
             * with FormatLocatorConverter
             */
            try {
                InvocationHandler innerProxy = Proxy.getInvocationHandler(element);
                ExtendedElementLocator innerLocator = (ExtendedElementLocator) (FieldUtils.getDeclaredField(innerProxy.getClass(),
                        "locator", true))
                                .get(innerProxy);

                if (Arrays.stream(objects).findAny().isPresent()) {
                    // start of case when we have L10N only on this step
                    boolean isTextContainsL10N = Arrays.stream(objects)
                            .map(String::valueOf)
                            .anyMatch(text -> LocalizeLocatorConverter.getL10nPattern().matcher(text).find());
                    if (isTextContainsL10N) {
                        boolean isAlreadyContainsL10NConverter = innerLocator.getLocatorConverters()
                                .stream()
                                .anyMatch(LocalizeLocatorConverter.class::isInstance);
                        if (!isAlreadyContainsL10NConverter) {
                            LocalizeLocatorConverter converter = new LocalizeLocatorConverter();
                            innerLocator.getLocatorConverters().addFirst(converter);
                        }
                    }
                    // end of case when we have L10N only on this step

                    if (innerLocator.getLocatorConverters().stream()
                            .anyMatch(FormatLocatorConverter.class::isInstance)) {
                        LOGGER.debug("Called format method of ExtendedWebElement class with parameters, but FormatLocatorConverter already exists "
                                + "for element: '{}', so locator will be recreated from original locator with new format parameters.", this.name);
                        innerLocator.getLocatorConverters()
                                .removeIf(FormatLocatorConverter.class::isInstance);
                    }

                    FormatLocatorConverter converter = new FormatLocatorConverter(objects);
                    innerLocator.getLocatorConverters().addFirst(converter);
                    innerLocator.buildConvertedBy();
                }
                WebElement proxy = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(),
                        new Class[] { WebElement.class, WrapsElement.class, Locatable.class },
                        innerProxy);
                formattedElement = AbstractUIObject.Builder.getInstance()
                        .setElement(proxy)
                        .setName(this.name)
                        .build(clazz);

            } catch (Exception e) {
                throw new RuntimeException("Something went wrong when try to format locator.", e);
            }
        } else {
            By by = this.by;
            if (Arrays.stream(objects).findAny().isPresent()) {
                String locator = by.toString();
                by = Arrays.stream(LocatorType.values())
                        .filter(lt -> lt.is(locator))
                        .findFirst()
                        .orElseThrow(
                                () -> new RuntimeException(String.format("Locator formatting failed - no suitable locator type found for formatting. "
                                        + "Investigate why '%s' was not formatted", locator)))
                        .buildLocatorFromString(locator, objects);
                LOGGER.debug("Formatted locator is : {}", by);
            }

            // todo add objects
            formattedElement = AbstractUIObject.Builder.getInstance()
                    .setBy(by)
                    .setName(name)
                    .setDriver(getDriver())
                    .setSearchContext(getSearchContext())
                    .build(clazz);
        }

        formattedElement.setName(this.name);
        return formattedElement;
    }

    /**
     * Get list of elements with formatted locator.<br>
     *
     * <p>
     * 1. If element created using {@link org.openqa.selenium.support.FindBy} or same annotations:<br>
     * If parameters were passed to the method, the result elements will be created with a new locator,
     * and if the format method with parameters was already called for this element, the element locator
     * will be recreated based on the original.<br>
     * <br>
     * <b>All original element statuses {@link CaseInsensitiveXPath},
     * {@link Localized} are saved for new elements</b>
     *
     * <p>
     * 2. If element created using constructor (it is not recommended to create element by hands):<br>
     * If parameters were passed to the method, the result elements will be created with a new locator.
     *
     * For all cases: if the method is called with no parameters, no locator formatting is applied, but elements will be created using original
     * locator.<br>
     *
     * <b>This method does not change the object on which it is called</b>.
     *
     * @param objects parameters
     * @return {@link List} of {@link ExtendedWebElement} if found, empty list otherwise
     */
    public List<T> formatToList(Object... objects) {
        List<T> extendedWebElementList = new ArrayList<>();
        try {
            if (element instanceof Proxy) {
                /*
                 * if element created using annotation (FindBy, ExtendedFindBy and so on), it will be proxy, so we get it and re-generate locator
                 * with FormatLocatorConverter
                 */
                InvocationHandler innerProxy = Proxy.getInvocationHandler(element);
                ExtendedElementLocator innerLocator = (ExtendedElementLocator) (FieldUtils.getDeclaredField(innerProxy.getClass(),
                        "locator", true))
                                .get(innerProxy);
                if (Arrays.stream(objects).findAny().isPresent()) {
                    // start of case when we have L10N only on this step
                    boolean isTextContainsL10N = Arrays.stream(objects)
                            .map(String::valueOf)
                            .anyMatch(text -> LocalizeLocatorConverter.getL10nPattern().matcher(text).find());
                    if (isTextContainsL10N) {
                        boolean isAlreadyContainsL10NConverter = innerLocator.getLocatorConverters()
                                .stream()
                                .anyMatch(LocalizeLocatorConverter.class::isInstance);
                        if (!isAlreadyContainsL10NConverter) {
                            LocalizeLocatorConverter converter = new LocalizeLocatorConverter();
                            innerLocator.getLocatorConverters().addFirst(converter);
                        }
                    }
                    // end of case when we have L10N only on this step

                    if (innerLocator.getLocatorConverters().stream()
                            .anyMatch(FormatLocatorConverter.class::isInstance)) {
                        LOGGER.debug(
                                "Called formatToList method of ExtendedWebElement class with parameters, but FormatLocatorConverter already exists "
                                        + "for element: '{}', so locator will be recreated from original locator with new format parameters.",
                                this.name);
                        innerLocator.getLocatorConverters()
                                .removeIf(FormatLocatorConverter.class::isInstance);
                    }
                    FormatLocatorConverter converter = new FormatLocatorConverter(objects);
                    innerLocator.getLocatorConverters()
                            .addFirst(converter);
                    innerLocator.buildConvertedBy();
                    innerLocator.getLocatorConverters()
                            .remove(converter);
                }
                ClassLoader classLoader = getClass().getClassLoader();
                InvocationHandler handler = new AbstractUIObjectListHandler<T>(classLoader, clazz, getDriver(), innerLocator, this.name,
                        innerLocator.getField());
                extendedWebElementList = (List<T>) Proxy.newProxyInstance(classLoader, new Class[] { List.class }, handler);
            } else {
                Objects.requireNonNull(by);
                By by = this.by;
                if (Arrays.stream(objects).findAny().isPresent()) {
                    String locator = this.by.toString();
                    by = Arrays.stream(LocatorType.values())
                            .filter(lt -> lt.is(locator))
                            .findFirst()
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            String.format("Locator formatting failed - no suitable locator type found for formatting. "
                                                    + "Investigate why '%s' was not formatted", locator)))
                            .buildLocatorFromString(locator, objects);
                }
                int i = 0;
                for (WebElement el : getSearchContext().findElements(by)) {
                    // todo add objects
                    T extEl = AbstractUIObject.Builder.getInstance()
                            .setElement(el)
                            .setName(String.format("%s - [%s]", name, i++))
                            .setDriver(getDriver())
                            .setSearchContext(getSearchContext())
                            .setFormatValues(String.valueOf(objects))
                            .build(clazz);
                    extendedWebElementList.add(extEl);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong when try to format locator to list", e);
        }
        return extendedWebElementList;
    }

    /**
     * Find Extended Web Element on page using By starting search from this
     * object.
     *
     * @param by Selenium By locator
     * @return ExtendedWebElement if exists otherwise null.
     */
    public T findNestedExtendedWebElement(By by) {
        return findNestedExtendedWebElement(by, by.toString(), EXPLICIT_TIMEOUT);
    }

    /**
     * Find Extended Web Element on page using By starting search from this
     * object.
     *
     * @param by Selenium By locator
     * @param timeout to wait
     * @return ExtendedWebElement if exists otherwise null.
     */
    public T findNestedExtendedWebElement(By by, long timeout) {
        return findNestedExtendedWebElement(by, by.toString(), timeout);
    }

    /**
     * Find Extended Web Element on page using By starting search from this
     * object.
     *
     * @param by Selenium By locator
     * @param name Element name
     * @return ExtendedWebElement if exists otherwise null.
     */
    public T findNestedExtendedWebElement(final By by, String name) {
        return findNestedExtendedWebElement(by, name, EXPLICIT_TIMEOUT);
    }

    /**
     * Find Extended Web Element on page using By starting search from this
     * object.
     *
     * @param by Selenium By locator
     * @param name Element name
     * @param timeout Timeout to find
     * @return ExtendedWebElement if exists otherwise null.
     */
    public T findNestedExtendedWebElement(final By by, String name, long timeout) {
        T el = AbstractUIObject.Builder.getInstance()
                .setBy(by)
                .setName(name)
                .setDriver(getDriver())
                .setSearchContext(findElement())
                .build(clazz);
        if (!el.isPresent(timeout)) {
            throw new NoSuchElementException(SpecialKeywords.NO_SUCH_ELEMENT_ERROR + by.toString());
        }
        return el;
    }

    public List<T> findNestedExtendedWebElements(By by) {
        return findNestedExtendedWebElements(by, EXPLICIT_TIMEOUT);
    }

    /**
     * Get list of {@link T}s. Search of elements starts from current {@link T}
     *
     * @param by see {@link By}
     * @param timeout timeout of checking the presence of the element(s)
     * @return list of ExtendedWebElements if found, empty list otherwise
     */
    public List<T> findNestedExtendedWebElements(final By by, long timeout) {
        List<T> extendedWebElements = new ArrayList<>();
        T tempElement = AbstractUIObject.Builder.getInstance()
                .setBy(by)
                .setName("temp element")
                .setDriver(getDriver())
                .setSearchContext(findElement())
                .build(clazz);
        if (!tempElement.isPresent(timeout)) {
            LOGGER.info("FAIL: element(s) '{}' is not found!", by);
            return extendedWebElements;
        }

        List<WebElement> webElements = findElement().findElements(by);
        int i = 0;
        for (WebElement el : webElements) {
            T extEl = AbstractUIObject.Builder.getInstance()
                    .setName(String.format("%s - [%d]", clazz.getSimpleName(), i))
                    .setDriver(getDriver())
                    .setSearchContext(findElement())
                    .setElement(el)
                    .build(clazz);
            extendedWebElements.add(extEl);
            i++;
        }
        return extendedWebElements;
    }

    // --------------------------------------------------------------------------
    // Utility methods (for current and inherited classes only) (should have protected visibility)
    // --------------------------------------------------------------------------

    /**
     * Get element waiting condition depends on element loading strategy
     */
    protected ExpectedCondition<?> getDefaultCondition() {
        List<ExpectedCondition<?>> conditions = new ArrayList<>();
        // generate the most popular waitCondition to check if element visible or present
        // need to get root element from with we will try to find element by By
        switch (loadingStrategy) {
        case BY_PRESENCE: {
            if (element != null) {
                conditions.add(ExpectedConditions.not(ExpectedConditions.stalenessOf(element)));
            }
            if (by != null) {
                if (getSearchContext() instanceof WebElement) {
                    conditions.add(ExpectedConditions.presenceOfNestedElementLocatedBy((WebElement) getSearchContext(), by));
                } else {
                    conditions.add(ExpectedConditions.presenceOfElementLocated(by));
                }
            }
            break;
        }
        case BY_VISIBILITY: {
            if (element != null) {
                conditions.add(ExpectedConditions.not(ExpectedConditions.invisibilityOf(element)));
            }
            if (by != null) {
                if (getSearchContext() instanceof WebElement) {
                    conditions.add(ExpectedConditions.visibilityOfNestedElementsLocatedBy((WebElement) getSearchContext(), by));
                } else {
                    conditions.add(ExpectedConditions.visibilityOfElementLocated(by));
                }
            }
            break;
        }
        case BY_PRESENCE_OR_VISIBILITY:
            if (element != null) {
                conditions.add(ExpectedConditions.not(ExpectedConditions.stalenessOf(element)));
                conditions.add(ExpectedConditions.not(ExpectedConditions.invisibilityOf(element)));
            }
            if (by != null) {
                if (getSearchContext() instanceof WebElement) {
                    conditions.add(ExpectedConditions.presenceOfNestedElementLocatedBy((WebElement) getSearchContext(), by));
                    conditions.add(ExpectedConditions.visibilityOfNestedElementsLocatedBy((WebElement) getSearchContext(), by));
                } else {
                    conditions.add(ExpectedConditions.presenceOfElementLocated(by));
                    conditions.add(ExpectedConditions.visibilityOfElementLocated(by));
                }
            }
            break;
        }

        if (conditions.isEmpty()) {
            throw new InvalidArgumentException("There should be at least one ExpectedCondition!");
        }
        return ExpectedConditions.or(conditions.toArray(new ExpectedCondition<?>[0]));
    }

    /**
     * Used for getting element for action.
     * 
     * @return see {@link WebElement}
     */
    protected WebElement findElement() {
        // if there is no by, then we simply return the element
        if (by == null) {
            return element;
        }

        // if element is proxy, we should return it as is because it already contains logic of searching element
        if (element instanceof Proxy) {
            return element;
        }

        List<WebElement> elements = getSearchContext().findElements(by);
        if (elements.isEmpty()) {
            throw new NoSuchElementException(SpecialKeywords.NO_SUCH_ELEMENT_ERROR + by.toString());
        }
        if (elements.size() > 1) {
            LOGGER.warn("Found '{}' items by locator: {}", elements.size(), by);
        }
        element = elements.get(0);
        return element;
    }

    /**
     * Get name of the element with locator.
     * 
     * @return name with locator
     */
    public String getNameWithLocator() {
        if (by != null) {
            return name + formatValues + String.format(" (%s)", by);
        } else {
            return name + formatValues + " (n/a)";
        }
    }

    // --------------------------------------------------------------------------
    // Utility methods (for internal usage only) (should have private visibility)
    // --------------------------------------------------------------------------

    private Object executeAction(ACTION_NAME actionName, ActionSteps actionSteps, Object... inputArgs) {
        Object result = null;
        switch (actionName) {
        case CLICK:
            actionSteps.doClick();
            break;
        case CLICK_BY_JS:
            actionSteps.doClickByJs();
            break;
        case CLICK_BY_ACTIONS:
            actionSteps.doClickByActions();
            break;
        case DOUBLE_CLICK:
            actionSteps.doDoubleClick();
            break;
        case HOVER:
            actionSteps.doHover((Integer) inputArgs[0], (Integer) inputArgs[1]);
            break;
        case RIGHT_CLICK:
            actionSteps.doRightClick();
            break;
        case GET_TEXT:
            result = actionSteps.doGetText();
            break;
        case GET_LOCATION:
            result = actionSteps.doGetLocation();
            break;
        case GET_SIZE:
            result = actionSteps.doGetSize();
            break;
        case GET_ATTRIBUTE:
            result = actionSteps.doGetAttribute((String) inputArgs[0]);
            break;
        case SEND_KEYS:
            actionSteps.doSendKeys((Keys) inputArgs[0]);
            break;
        case TYPE:
            actionSteps.doType((String) inputArgs[0]);
            break;
        case ATTACH_FILE:
            actionSteps.doAttachFile((String) inputArgs[0]);
            break;
        case CHECK:
            actionSteps.doCheck();
            break;
        case UNCHECK:
            actionSteps.doUncheck();
            break;
        case IS_CHECKED:
            result = actionSteps.doIsChecked();
            break;
        case SELECT:
            result = actionSteps.doSelect((String) inputArgs[0]);
            break;
        case SELECT_VALUES:
            result = actionSteps.doSelectValues((String[]) inputArgs);
            break;
        case SELECT_BY_MATCHER:
            result = actionSteps.doSelectByMatcher((BaseMatcher<String>) inputArgs[0]);
            break;
        case SELECT_BY_PARTIAL_TEXT:
            result = actionSteps.doSelectByPartialText((String) inputArgs[0]);
            break;
        case SELECT_BY_INDEX:
            result = actionSteps.doSelectByIndex((int) inputArgs[0]);
            break;
        case GET_SELECTED_VALUE:
            result = actionSteps.doGetSelectedValue();
            break;
        case GET_SELECTED_VALUES:
            result = actionSteps.doGetSelectedValues();
            break;
        default:
            Assert.fail("Unsupported UI action name" + actionName);
            break;
        }
        return result;
    }

    /**
     * doAction on element.
     *
     * @param actionName
     *            ACTION_NAME
     * @param timeout
     *            long
     * @param waitCondition
     *            to check element conditions before action
     * @return
     *         Object
     */
    private Object doAction(ACTION_NAME actionName, long timeout, ExpectedCondition<?> waitCondition) {
        // [VD] do not remove null args otherwise all actions without arguments will be broken!
        Object nullArgs = null;
        return doAction(actionName, timeout, waitCondition, nullArgs);
    }

    private Object doAction(ACTION_NAME actionName, long timeout, @Nullable ExpectedCondition<?> waitCondition, Object... inputArgs) {
        if (waitCondition != null) {
            // do verification only if waitCondition is not null
            if (!waitUntil(waitCondition, timeout)) {
                throw new NoSuchElementException(Messager.ELEMENT_CONDITION_NOT_VERIFIED.getMessage(actionName.getKey(), getNameWithLocator()));
            }
        }
        
        if (isLocalized) {
            isLocalized = false; // single verification is enough for this particular element
            L10N.verify(this);
        }
        
        findElement();
        return overrideAction(actionName, inputArgs);
    }

    // single place for all supported UI actions in carina core
    private Object overrideAction(ACTION_NAME actionName, Object... inputArgs) {
        return executeAction(actionName, new ActionSteps() {
            @Override
            public void doClick() {
                Objects.requireNonNull(element);
                DriverListener.setMessages(Messager.ELEMENT_CLICKED.getMessage(name),
                        Messager.ELEMENT_NOT_CLICKED.getMessage(getNameWithLocator()));
                element.click();
            }

            @Override
            public void doClickByJs() {
                Objects.requireNonNull(element);
                DriverListener.setMessages(Messager.ELEMENT_CLICKED.getMessage(name), Messager.ELEMENT_NOT_CLICKED.getMessage(getNameWithLocator()));
                LOGGER.info("Do click by JavascriptExecutor for element: {}", getNameWithLocator());
                ((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", element);
            }

            @Override
            public void doClickByActions() {
                Objects.requireNonNull(element);
                DriverListener.setMessages(Messager.ELEMENT_CLICKED.getMessage(name), Messager.ELEMENT_NOT_CLICKED.getMessage(getNameWithLocator()));
                LOGGER.info("Do click by Actions for element: {}", getNameWithLocator());
                new Actions(getDriver()).moveToElement(element)
                        .click()
                        .perform();
            }

            @Override
            public void doDoubleClick() {
                Objects.requireNonNull(element);
                DriverListener.setMessages(Messager.ELEMENT_DOUBLE_CLICKED.getMessage(name),
                        Messager.ELEMENT_NOT_DOUBLE_CLICKED.getMessage(getNameWithLocator()));
                new Actions(getDriver()).moveToElement(element)
                        .doubleClick(element)
                        .build()
                        .perform();
            }

            @Override
            public void doHover(@Nullable Integer xOffset, @Nullable Integer yOffset) {
                Objects.requireNonNull(element);
                DriverListener.setMessages(Messager.ELEMENT_HOVERED.getMessage(name), Messager.ELEMENT_NOT_HOVERED.getMessage(getNameWithLocator()));
                Actions action = new Actions(getDriver());
                if (xOffset != null && yOffset != null) {
                    action.moveToElement(element, xOffset, yOffset).build().perform();
                } else {
                    action.moveToElement(element).build().perform();
                }
            }

            @Override
            public void doSendKeys(Keys keys) {
                Objects.requireNonNull(keys);
                Objects.requireNonNull(element);
                DriverListener.setMessages(Messager.KEYS_SEND_TO_ELEMENT.getMessage(keys.toString(), name),
                        Messager.KEYS_NOT_SEND_TO_ELEMENT.getMessage(keys.toString(), getNameWithLocator()));
                element.sendKeys(keys);
            }

            @Override
            public void doType(String text) {
                Objects.requireNonNull(text);
                Objects.requireNonNull(element);
                final String decryptedText = CryptoUtils.INSTANCE.decryptIfEncrypted(text);
                DriverListener.setMessages(Messager.KEYS_CLEARED_IN_ELEMENT.getMessage(name),
                        Messager.KEYS_NOT_CLEARED_IN_ELEMENT.getMessage(getNameWithLocator()));
                element.clear();
                String textLog = (!decryptedText.equals(text) ? "********" : text);
                DriverListener.setMessages(Messager.KEYS_SEND_TO_ELEMENT.getMessage(textLog, name),
                        Messager.KEYS_NOT_SEND_TO_ELEMENT.getMessage(textLog, getNameWithLocator()));
                element.sendKeys(decryptedText);
            }

            @Override
            public void doAttachFile(String filePath) {
                Objects.requireNonNull(filePath);
                Objects.requireNonNull(element);
                final String decryptedText = CryptoUtils.INSTANCE.decryptIfEncrypted(filePath);
                String textLog = (!decryptedText.equals(filePath) ? "********" : filePath);
                DriverListener.setMessages(Messager.FILE_ATTACHED.getMessage(textLog, name),
                        Messager.FILE_NOT_ATTACHED.getMessage(textLog, getNameWithLocator()));
                ((JavascriptExecutor) getDriver()).executeScript("arguments[0].style.display = 'block';", element);
                DriverListener.castDriver(getDriver(), RemoteWebDriver.class).setFileDetector(new LocalFileDetector());
                element.sendKeys(decryptedText);
            }

            @Override
            public String doGetText() {
                Objects.requireNonNull(element);
                String text = element.getText();
                LOGGER.debug(Messager.ELEMENT_ATTRIBUTE_FOUND.getMessage("Text", text, name));
                return text;
            }

            @Override
            public Point doGetLocation() {
                Objects.requireNonNull(element);
                Point point = element.getLocation();
                LOGGER.debug(Messager.ELEMENT_ATTRIBUTE_FOUND.getMessage("Location", point.toString(), name));
                return point;
            }

            @Override
            public Dimension doGetSize() {
                Objects.requireNonNull(element);
                Dimension dim = element.getSize();
                LOGGER.debug(Messager.ELEMENT_ATTRIBUTE_FOUND.getMessage("Size", dim.toString(), name));
                return dim;
            }

            @Override
            public String doGetAttribute(String attributeName) {
                Objects.requireNonNull(attributeName);
                Objects.requireNonNull(element);
                String attribute = element.getAttribute(attributeName);
                LOGGER.debug(Messager.ELEMENT_ATTRIBUTE_FOUND.getMessage(attributeName, attribute, name));
                return attribute;
            }

            @Override
            public void doRightClick() {
                Objects.requireNonNull(element);
                DriverListener.setMessages(Messager.ELEMENT_RIGHT_CLICKED.getMessage(name),
                        Messager.ELEMENT_NOT_RIGHT_CLICKED.getMessage(getNameWithLocator()));
                Actions action = new Actions(getDriver());
                action.moveToElement(element).contextClick(element).build().perform();
            }

            @Override
            public void doCheck() {
                Objects.requireNonNull(element);
                DriverListener.setMessages(Messager.CHECKBOX_CHECKED.getMessage(name), null);
                boolean isSelected = element.isSelected();
                if (element.getAttribute("checked") != null) {
                    isSelected |= element.getAttribute("checked").equalsIgnoreCase("true");
                }
                if (!isSelected) {
                    click();
                }
            }

            @Override
            public void doUncheck() {
                Objects.requireNonNull(element);
                DriverListener.setMessages(Messager.CHECKBOX_UNCHECKED.getMessage(name), null);
                boolean isSelected = element.isSelected();
                if (element.getAttribute("checked") != null) {
                    isSelected |= element.getAttribute("checked").equalsIgnoreCase("true");
                }
                if (isSelected) {
                    click();
                }
            }

            @Override
            public boolean doIsChecked() {
                Objects.requireNonNull(element);
                boolean res = element.isSelected();
                if (element.getAttribute("checked") != null) {
                    res |= element.getAttribute("checked").equalsIgnoreCase("true");
                }
                return res;
            }

            @Override
            public boolean doSelect(String text) {
                Objects.requireNonNull(text);
                Objects.requireNonNull(element);
                final String decryptedSelectText = CryptoUtils.INSTANCE.decryptIfEncrypted(text);
                String textLog = (!decryptedSelectText.equals(text) ? "********" : text);
                DriverListener.setMessages(Messager.SELECT_BY_TEXT_PERFORMED.getMessage(textLog, name),
                        Messager.SELECT_BY_TEXT_NOT_PERFORMED.getMessage(textLog, getNameWithLocator()));
                final Select s = new Select(element);
                // [VD] do not use selectByValue as modern controls could have only visible value without value
                s.selectByVisibleText(decryptedSelectText);
                return true;
            }

            @Override
            public boolean doSelectValues(String[] values) {
                Objects.requireNonNull(values);
                boolean result = true;
                for (String value : values) {
                    if (!select(value)) {
                        result = false;
                    }
                }
                return result;
            }

            @Override
            public boolean doSelectByMatcher(BaseMatcher<String> matcher) {
                Objects.requireNonNull(matcher);
                Objects.requireNonNull(element);
                DriverListener.setMessages(Messager.SELECT_BY_MATCHER_TEXT_PERFORMED.getMessage(matcher.toString(), name),
                        Messager.SELECT_BY_MATCHER_TEXT_NOT_PERFORMED.getMessage(matcher.toString(), getNameWithLocator()));
                final Select s = new Select(element);
                String fullTextValue = null;
                for (WebElement option : s.getOptions()) {
                    if (matcher.matches(option.getText())) {
                        fullTextValue = option.getText();
                        break;
                    }
                }
                s.selectByVisibleText(fullTextValue);
                return true;
            }

            @Override
            public boolean doSelectByPartialText(String partialSelectText) {
                Objects.requireNonNull(partialSelectText);
                Objects.requireNonNull(element);
                DriverListener.setMessages(
                        Messager.SELECT_BY_TEXT_PERFORMED.getMessage(partialSelectText, name),
                        Messager.SELECT_BY_TEXT_NOT_PERFORMED.getMessage(partialSelectText, getNameWithLocator()));
                final Select s = new Select(element);
                String fullTextValue = null;
                for (WebElement option : s.getOptions()) {
                    if (option.getText().contains(partialSelectText)) {
                        fullTextValue = option.getText();
                        break;
                    }
                }
                s.selectByVisibleText(fullTextValue);
                return true;
            }

            @Override
            public boolean doSelectByIndex(int index) {
                Objects.requireNonNull(element);
                DriverListener.setMessages(
                        Messager.SELECT_BY_INDEX_PERFORMED.getMessage(String.valueOf(index), name),
                        Messager.SELECT_BY_INDEX_NOT_PERFORMED.getMessage(String.valueOf(index), getNameWithLocator()));
                final Select s = new Select(element);
                s.selectByIndex(index);
                return true;
            }

            @Override
            public String doGetSelectedValue() {
                Objects.requireNonNull(element);
                final Select s = new Select(element);
                return s.getAllSelectedOptions().get(0).getText();
            }

            @Override
            public List<String> doGetSelectedValues() {
                Objects.requireNonNull(element);
                final Select s = new Select(element);
                List<String> values = new ArrayList<>();
                for (WebElement we : s.getAllSelectedOptions()) {
                    values.add(we.getText());
                }
                return values;
            }

        }, inputArgs);
    }

    private interface ActionSteps {
        void doClick();

        void doClickByJs();

        void doClickByActions();

        void doDoubleClick();

        void doRightClick();

        void doHover(Integer xOffset, Integer yOffset);

        void doType(String text);

        void doSendKeys(Keys keys);

        void doAttachFile(String filePath);

        void doCheck();

        void doUncheck();

        boolean doIsChecked();

        String doGetText();

        Point doGetLocation();

        Dimension doGetSize();

        String doGetAttribute(String name);

        boolean doSelect(String text);

        boolean doSelectValues(final String[] values);

        boolean doSelectByMatcher(final BaseMatcher<String> matcher);

        boolean doSelectByPartialText(final String partialSelectText);

        boolean doSelectByIndex(final int index);

        String doGetSelectedValue();

        List<String> doGetSelectedValues();
    }

}
