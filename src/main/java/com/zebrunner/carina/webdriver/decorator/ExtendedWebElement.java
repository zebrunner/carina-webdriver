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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.zebrunner.carina.utils.factory.ICustomTypePageFactory;
import com.zebrunner.carina.webdriver.helper.IClipboardHelper;
import com.zebrunner.carina.webdriver.helper.ICommonsHelper;
import com.zebrunner.carina.webdriver.helper.IExtendedWebElementHelper;
import com.zebrunner.carina.webdriver.helper.IPageActionsHelper;
import com.zebrunner.carina.webdriver.helper.IPageDataHelper;
import com.zebrunner.carina.webdriver.helper.IPageStorageHelper;
import com.zebrunner.carina.webdriver.helper.IWaitHelper;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hamcrest.BaseMatcher;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;
import org.openqa.selenium.WrapsElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Locatable;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.decorators.Decorated;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.zebrunner.carina.utils.IWebElement;
import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.encryptor.EncryptorUtils;
import com.zebrunner.carina.utils.messager.Messager;
import com.zebrunner.carina.utils.performance.ACTION_NAME;
import com.zebrunner.carina.utils.resources.L10N;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.core.capability.CapabilityUtils;
import com.zebrunner.carina.webdriver.core.capability.DriverType;
import com.zebrunner.carina.webdriver.listener.DriverListener;

import javax.annotation.Nullable;

/**
 * Base Extended WebElement
 */
public class ExtendedWebElement implements IWebElement, WebElement, IExtendedWebElementHelper, ICommonsHelper, IWaitHelper, ICustomTypePageFactory,
        IClipboardHelper, IPageStorageHelper, IPageDataHelper, IPageActionsHelper, Cloneable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final String udid;
    protected WebDriver driver;
    private SearchContext searchContext;
    private By by = null;
    /**
     * @deprecated will be hided in the next release. Use {@link #getElement()} instead
     */
    @Deprecated
    protected WebElement element = null;
    private String name = "n/a";
    private ElementLoadingStrategy loadingStrategy;
    private boolean isLocalized = false;

    /**
     * Required constructor
     * 
     * @param driver {@link WebDriver}
     * @param searchContext {@link SearchContext}
     */
    public ExtendedWebElement(WebDriver driver, SearchContext searchContext) {
        this.driver = driver;
        this.searchContext = searchContext;
        this.udid = UUID.randomUUID().toString() + System.currentTimeMillis();
        this.loadingStrategy = ElementLoadingStrategy.valueOf(Configuration.getRequired(WebDriverConfiguration.Parameter.ELEMENT_LOADING_STRATEGY));
    }

    /**
     * Optional constructor
     * 
     * @param by {@link By}
     * @param name name of the element
     * @param driver {@link WebDriver}
     * @param searchContext {@link WebElement}
     * @param formatValues unused
     */
    public ExtendedWebElement(By by, String name, WebDriver driver, SearchContext searchContext, @Deprecated Object[] formatValues) {
        this(by, name, driver, searchContext);
    }

    /**
     * Optional constructor
     *
     * @param element {@link WebElement}
     * @param name name of the element
     * @param by {@link By}
     */
    public ExtendedWebElement(WebElement element, String name, By by) {
        this(by, name, ((WrapsDriver) element).getWrappedDriver(), ((WrapsDriver) element).getWrappedDriver());
        this.element = element;
    }

    /**
     * Optional constructor
     * 
     * @param by {@link By}
     * @param name name of the element
     * @param driver {@link WebDriver}
     * @param searchContext {@link SearchContext}
     */
    public ExtendedWebElement(By by, String name, WebDriver driver, SearchContext searchContext) {
        this(driver, searchContext);
        this.by = by;
        this.name = name;
    }

    ///////////// GETTERS/SETTERS (FINAL) /////////////

    @Override
    public final WebDriver getDriver() {
        return driver;
    }

    public final void setDriver(WebDriver driver) {
        this.driver = Objects.requireNonNull(driver);
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public final String getUuid() {
        return udid;
    }

    /**
     * Get locator
     * 
     * @return {@link By} if available, null otherwise
     */
    public final By getBy() {
        return this.by;
    }

    /**
     * Set locator
     * 
     * @param by {@link By}
     */
    public final void setBy(@Nullable By by) {
        this.by = by;
    }

    public final SearchContext getSearchContext() {
        return searchContext;
    }

    public final void setSearchContext(SearchContext searchContext) {
        this.searchContext = Objects.requireNonNull(searchContext);
    }

    public final ElementLoadingStrategy getLoadingStrategy() {
        return loadingStrategy;
    }

    public final void setLoadingStrategy(ElementLoadingStrategy loadingStrategy) {
        this.loadingStrategy = Objects.requireNonNull(loadingStrategy);
    }

    /**
     * Get Selenium WebElement (Proxy).
     * 
     * @return {@link WebElement}
     */
    public final WebElement getElement() {
        InvocationHandler handler = new ExtendedWebElementHandler(by, element, searchContext);
        return (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] { WebElement.class, WrapsElement.class, WrapsDriver.class, Locatable.class, TakesScreenshot.class },
                handler);
    }

    public final void setElement(WebElement element) {
        this.element = element;
    }

    public final String getNameWithLocator() {
        if (this.by != null) {
            return this.name + String.format(" (%s)", by);
        } else {
            return this.name + " (n/a)";
        }
    }

    ///////////// PRESENCE/VISIBILITY /////////////

    /**
     * Check that element present or visible (depends on {@link ElementLoadingStrategy}).
     *
     * @return true if element present or visible, false otherwise
     */
    public boolean isPresent() {
        return isPresent(getDefaultWaitTimeout());
    }

    /**
     * Check that element present or visible (depends on {@link ElementLoadingStrategy})
     *
     * @param timeout timeout, in seconds
     * @return true if element present or visible, false otherwise
     */
    public boolean isPresent(long timeout) {
        return isPresent(Duration.ofSeconds(timeout));
    }

    /**
     * Check that element present or visible (depends on {@link ElementLoadingStrategy})
     *
     * @param timeout {@link Duration}
     * @return true if element present or visible, false otherwise
     */
    public boolean isPresent(Duration timeout) {
        clearElementState();
        boolean res = false;
        try {
            res = waitUntil(getDefaultElementWaitCondition(), timeout);
        } catch (StaleElementReferenceException | NoSuchElementException e) {
            // there is no sense to continue as StaleElementReferenceException captured
        }
        return res;
    }

    /**
     * @deprecated wrong implementation
     */
    @Deprecated(forRemoval = true)
    public boolean isPresent(By by, long timeout) {
        return isPresent(timeout);
    }

    /**
     * Check that element present and visible.
     *
     * @return true if present and visible, false otherwise
     */
    public boolean isElementPresent() {
        return isElementPresent(getDefaultWaitTimeout());
    }

    /**
     * Check that element present and visible.
     * 
     * @param timeout timeout, in seconds
     * @return true if present and visible, false otherwise
     */
    public boolean isElementPresent(long timeout) {
        return isElementPresent(Duration.ofSeconds(timeout));
    }

    /**
     * Check that element present and visible
     *
     * @param timeout {@link Duration}
     * @return true if present and visible, false otherwise
     */
    public boolean isElementPresent(Duration timeout) {
        clearElementState();
        // perform at once super-fast single selenium call and only if nothing found move to waitAction
        if (element != null) {
            try {
                if (element.isDisplayed()) {
                    return true;
                }
            } catch (StaleElementReferenceException e) {
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
            if (searchContext instanceof WebElement) {
                conditions.add(ExpectedConditions.visibilityOfNestedElementsLocatedBy((WebElement) searchContext, by));
            } else {
                conditions.add(ExpectedConditions.visibilityOfElementLocated(by));
            }
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
     * Check that element not present and not visible
     *
     * @param timeout timeout, in seconds
     * @return true if element is not present and not visible, false otherwise
     */
    public boolean isElementNotPresent(long timeout) {
        return !isElementPresent(timeout);
    }

    /**
     * Checks that element clickable
     *
     * @return true if element is clickable, false otherwise
     */
    public boolean isClickable() {
        return isClickable(getDefaultWaitTimeout());
    }

    /**
     * Checks that element clickable
     *
     * @param timeout timeout, in seconds
     * @return true if element is clickable, false otherwise
     */
    public boolean isClickable(long timeout) {
        return isClickable(Duration.ofSeconds(timeout));
    }

    /**
     * Check that element clickable
     *
     * @param timeout {@link Duration}
     * @return true if clickable, false otherwise
     */
    public boolean isClickable(Duration timeout) {
        clearElementState();
        List<ExpectedCondition<?>> conditions = new ArrayList<>();
        if (element != null) {
            conditions.add(ExpectedConditions.elementToBeClickable(element));
        }
        if (by != null) {
            if (searchContext instanceof WebElement) {
                ExpectedCondition<?> condition = new ExpectedCondition<WebElement>() {
                    @Override
                    public WebElement apply(WebDriver driver) {
                        List<WebElement> elements = ExpectedConditions.visibilityOfNestedElementsLocatedBy((WebElement) searchContext, by)
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
        try {
            return waitUntil(ExpectedConditions.or(conditions.toArray(new ExpectedCondition[0])), timeout);
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    /**
     * Checks that element visible
     *
     * @return true if element is visible, false otherwise
     */
    public boolean isVisible() {
        return isVisible(getDefaultWaitTimeout());
    }

    /**
     * Checks that element visible
     *
     * @param timeout timeout, in seconds
     * @return true if element is visible, false otherwise
     */
    public boolean isVisible(long timeout) {
        return isVisible(Duration.ofSeconds(timeout));
    }

    /**
     * Check that element is visible
     *
     * @param timeout {@link Duration}
     * @return true if element is visible, false otherwise
     */
    public boolean isVisible(Duration timeout) {
        clearElementState();
        List<ExpectedCondition<?>> conditions = new ArrayList<>();
        if (element != null) {
            conditions.add(ExpectedConditions.visibilityOf(element));
        }
        if (by != null) {
            if (searchContext instanceof WebElement) {
                conditions.add(ExpectedConditions.visibilityOfNestedElementsLocatedBy((WebElement) searchContext, by));
            } else {
                conditions.add(ExpectedConditions.visibilityOfElementLocated(by));
            }
        }
        boolean res = false;
        try {
            res = waitUntil(ExpectedConditions.or(conditions.toArray(new ExpectedCondition[0])), timeout);
        } catch (StaleElementReferenceException e) {
            // there is no sense to continue as StaleElementReferenceException captured
        }
        return res;
    }

    /**
     * Check that element with text present
     *
     * @param text of element to check.
     * @return true if element with such test present, false otherwise
     */
    public boolean isElementWithTextPresent(final String text) {
        return isElementWithTextPresent(text, getDefaultWaitTimeout());
    }

    /**
     * Check that element with text present
     *
     * @param text of element to check.
     * @param timeout timeout, in seconds
     * @return true if element with such test present, false otherwise
     */
    public boolean isElementWithTextPresent(final String text, long timeout) {
        return isElementWithTextPresent(text, Duration.ofSeconds(timeout));
    }

    /**
     * Check that element with text present
     *
     * @param text of element to check.
     * @param timeout {@link Duration}
     * @return true if element with such test present, false otherwise
     */
    public boolean isElementWithTextPresent(final String text, Duration timeout) {
        clearElementState();
        final String decryptedText = EncryptorUtils.decrypt(text);
        List<ExpectedCondition<?>> conditions = new ArrayList<>();
        if (element != null) {
            conditions.add(ExpectedConditions.textToBePresentInElement(element, decryptedText));
        }
        if (by != null) {
            if (searchContext instanceof WebElement) {
                ExpectedCondition<?> condition = new ExpectedCondition<Boolean>() {
                    @Override
                    public Boolean apply(WebDriver driver) {
                        WebElement e = searchContext.findElement(by);
                        try {
                            String elementText = e.getText();
                            return elementText.contains(text);
                        } catch (StaleElementReferenceException ex) {
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

            return waitUntil(ExpectedConditions.or(conditions.toArray(new ExpectedCondition[0])), timeout);
    }

    ///////////// ELEMENT INFORMATION /////////////

    /**
     * Get checkbox state
     *
     * @return true if element is checked, false otherwise
     */
    public boolean isChecked() {
        return (boolean) doAction(ACTION_NAME.IS_CHECKED, getDefaultWaitTimeout(), getDefaultElementWaitCondition());
    }

    /**
     * Get selected elements from one-value select
     *
     * @return selected value
     */
    public String getSelectedValue() {
        return (String) doAction(ACTION_NAME.GET_SELECTED_VALUE, getDefaultWaitTimeout(), getDefaultElementWaitCondition());
    }

    /**
     * Get selected elements from multi-value select
     *
     * @return selected values
     */
    @SuppressWarnings("unchecked")
    public List<String> getSelectedValues() {
        return (List<String>) doAction(ACTION_NAME.GET_SELECTED_VALUES, getDefaultWaitTimeout(), getDefaultElementWaitCondition());
    }

    /**
     * Get element text
     *
     * @return text
     */
    public String getText() {
        return (String) doAction(ACTION_NAME.GET_TEXT, getDefaultWaitTimeout(), getDefaultElementWaitCondition());
    }

    /**
     * Get element location
     *
     * @return {@link Point}
     */
    public Point getLocation() {
        return (Point) doAction(ACTION_NAME.GET_LOCATION, getDefaultWaitTimeout(), getDefaultElementWaitCondition());
    }

    /**
     * Get element size.
     *
     * @return {@link Dimension}
     */
    public Dimension getSize() {
        return (Dimension) doAction(ACTION_NAME.GET_SIZE, getDefaultWaitTimeout(), getDefaultElementWaitCondition());
    }

    @Override
    public Rectangle getRect() {
       return findElement().getRect();
    }

    @Override
    public String getCssValue(String propertyName) {
        return findElement().getCssValue(propertyName);
    }

    /**
     * Get element attribute.
     *
     * @param name of attribute
     * @return attribute value
     */
    public String getAttribute(String name) {
        return (String) doAction(ACTION_NAME.GET_ATTRIBUTE, getDefaultWaitTimeout(), getDefaultElementWaitCondition(), name);
    }

    @Override
    public boolean isSelected() {
        try {
            return findElement().isSelected();
        }catch (NoSuchElementException e) {
            throw new StaleElementReferenceException("stale element reference");
        }
    }

    @Override
    public boolean isEnabled() {
        try {
            return findElement().isEnabled();
        }catch (NoSuchElementException e) {
            throw new StaleElementReferenceException("stale element reference");
        }
    }

    @Override
    public String getTagName() {
        return findElement().getTagName();
    }

    @Override
    public boolean isDisplayed() {
        try {
            return findElement().isDisplayed();
        } catch (NoSuchElementException e) {
            throw new StaleElementReferenceException("stale element reference");
        }
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        throw new NotImplementedException();
    }

    ///////////// ACTIONS /////////////

    @Override
    public void submit() {
        throw new NotImplementedException();
    }

    @Override
    public void sendKeys(CharSequence... keysToSend) {
        throw new NotImplementedException();
    }

    @Override
    public void clear() {
        throw new NotImplementedException();
    }

    /**
     * Click on element
     */
    public void click() {
        click(getDefaultWaitTimeout());
    }

    /**
     * Click on element
     *
     * @param timeout timeout, in seconds
     */
    public void click(long timeout) {
        click(Duration.ofSeconds(timeout));
    }

    /**
     * Click on element
     *
     * @param timeout {@link Duration}
     */
    public void click(Duration timeout) {
        click(timeout, getDefaultElementWaitCondition());
    }

    /**
     * Click on element
     *
     * @param timeout timeout, in seconds
     * @param waitCondition {@link ExpectedCondition}
     */
    public void click(long timeout, ExpectedCondition<?> waitCondition) {
        click(Duration.ofSeconds(timeout), waitCondition);
    }

    /**
     * Click on element
     *
     * @param timeout {@link Duration}
     * @param waitCondition {@link ExpectedCondition} wait condition before action
     */
    public void click(Duration timeout, ExpectedCondition<?> waitCondition) {
        doAction(ACTION_NAME.CLICK, timeout, waitCondition);
    }

    /**
     * Click on element by javascript
     */
    public void clickByJs() {
        clickByJs(getDefaultWaitTimeout());
    }

    /**
     * Click on element by javascript
     *
     * @param timeout timeout, in seconds
     */
    public void clickByJs(long timeout) {
        clickByJs(Duration.ofSeconds(timeout));
    }

    /**
     * Click on element by javascript
     *
     * @param timeout {@link Duration}
     */
    public void clickByJs(Duration timeout) {
        clickByJs(timeout, getDefaultElementWaitCondition());
    }

    /**
     * Click on element by javascript
     *
     * @param timeout timeout, in seconds
     * @param waitCondition {@link ExpectedCondition}
     */
    public void clickByJs(long timeout, ExpectedCondition<?> waitCondition) {
        clickByJs(Duration.ofSeconds(timeout), waitCondition);
    }

    /**
     * Click on element by javascript
     *
     * @param timeout {@link Duration}
     * @param waitCondition to check element conditions before action
     */
    public void clickByJs(Duration timeout, ExpectedCondition<?> waitCondition) {
        doAction(ACTION_NAME.CLICK_BY_JS, timeout, waitCondition);
    }

    /**
     * Click on element by {@link Actions}
     */
    public void clickByActions() {
        clickByActions(getDefaultWaitTimeout());
    }

    /**
     * Click on element by {@link Actions}
     *
     * @param timeout timeout, in seconds
     */
    public void clickByActions(long timeout) {
        clickByActions(Duration.ofSeconds(timeout));
    }

    /**
     * Click on element by {@link Actions}
     *
     * @param timeout {@link Duration}
     */
    public void clickByActions(Duration timeout) {
        clickByActions(timeout, getDefaultElementWaitCondition());
    }

    /**
     * Click on element by {@link Actions}
     *
     * @param timeout timeout, in seconds
     * @param waitCondition {@link ExpectedCondition}
     */
    public void clickByActions(long timeout, ExpectedCondition<?> waitCondition) {
        clickByActions(Duration.ofSeconds(timeout), waitCondition);
    }

    /**
     * Click on element by {@link Actions}
     *
     * @param timeout {@link Duration}
     * @param waitCondition to check element conditions before action
     */
    public void clickByActions(Duration timeout, ExpectedCondition<?> waitCondition) {
        doAction(ACTION_NAME.CLICK_BY_ACTIONS, timeout, waitCondition);
    }

    /**
     * Double Click on element.
     */
    public void doubleClick() {
        doubleClick(getDefaultWaitTimeout());
    }

    public void doubleClick(long timeout) {
        doubleClick(Duration.ofSeconds(timeout));
    }

    /**
     * Double Click on element.
     *
     * @param timeout to wait
     */
    public void doubleClick(Duration timeout) {
        doubleClick(timeout, getDefaultElementWaitCondition());
    }

    public void doubleClick(long timeout, ExpectedCondition<?> waitCondition) {
        doubleClick(Duration.ofSeconds(timeout), waitCondition);
    }

    /**
     * Double Click on element.
     *
     * @param timeout to wait
     * @param waitCondition
     *            to check element conditions before action
     */
    public void doubleClick(Duration timeout, ExpectedCondition<?> waitCondition) {
        doAction(ACTION_NAME.DOUBLE_CLICK, timeout, waitCondition);
    }

    /**
     * Mouse RightClick on element.
     */
    public void rightClick() {
        rightClick(getDefaultWaitTimeout());
    }

    public void rightClick(long timeout) {
        rightClick(Duration.ofSeconds(timeout));
    }

    /**
     * Mouse RightClick on element.
     *
     * @param timeout to wait
     */
    public void rightClick(Duration timeout) {
        rightClick(timeout, getDefaultElementWaitCondition());
    }

    public void rightClick(long timeout, ExpectedCondition<?> waitCondition) {
        rightClick(Duration.ofSeconds(timeout), waitCondition);
    }

    /**
     * Mouse RightClick on element.
     *
     * @param timeout to wait
     * @param waitCondition to check element conditions before action
     */
    public void rightClick(Duration timeout, ExpectedCondition<?> waitCondition) {
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
        doAction(ACTION_NAME.HOVER, getDefaultWaitTimeout(), getDefaultElementWaitCondition(), xOffset, yOffset);
    }

    /**
     * Click onto element if it present.
     *
     * @return boolean return true if clicked
     */
    public boolean clickIfPresent() {
        return clickIfPresent(getDefaultWaitTimeout());
    }

    public boolean clickIfPresent(long timeout) {
        return clickIfPresent(Duration.ofSeconds(timeout));
    }

    /**
     * Click onto element if present.
     *
     * @param timeout timeout
     * @return boolean return true if clicked
     */
    public boolean clickIfPresent(Duration timeout) {
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
        sendKeys(keys, getDefaultWaitTimeout());
    }

    public void sendKeys(Keys keys, long timeout) {
        sendKeys(keys, Duration.ofSeconds(timeout));
    }

    /**
     * Send Keys to element.
     *
     * @param keys Keys
     * @param timeout to wait
     */
    public void sendKeys(Keys keys, Duration timeout) {
        sendKeys(keys, timeout, getDefaultElementWaitCondition());
    }

    public void sendKeys(Keys keys, long timeout, ExpectedCondition<?> waitCondition) {
        sendKeys(keys, Duration.ofSeconds(timeout), waitCondition);
    }

    /**
     * Send Keys to element.
     *
     * @param keys Keys
     * @param timeout to wait
     * @param waitCondition to check element conditions before action
     */
    public void sendKeys(Keys keys, Duration timeout, ExpectedCondition<?> waitCondition) {
        doAction(ACTION_NAME.SEND_KEYS, timeout, waitCondition, keys);
    }

    /**
     * Type text to element.
     *
     * @param text String
     */
    public void type(String text) {
        type(text, getDefaultWaitTimeout());
    }

    public void type(String text, long timeout) {
        type(text, Duration.ofSeconds(timeout));
    }

    /**
     * Type text to element.
     *
     * @param text String
     * @param timeout to wait
     */
    public void type(String text, Duration timeout) {
        type(text, timeout, getDefaultElementWaitCondition());
    }

    public void type(String text, long timeout, ExpectedCondition<?> waitCondition) {
        type(text, Duration.ofSeconds(timeout), waitCondition);
    }

    /**
     * Type text to element.
     *
     * @param text String
     * @param timeout to wait
     * @param waitCondition to check element conditions before action
     */
    public void type(String text, Duration timeout, ExpectedCondition<?> waitCondition) {
        doAction(ACTION_NAME.TYPE, timeout, waitCondition, text);
    }

    /**
     * Scroll to element (applied only for desktop).
     * Useful for desktop with React
     */
    public void scrollTo() {
        if (DriverType.MOBILE.equals(CapabilityUtils.getDriverType(((HasCapabilities) getDriver()).getCapabilities()))) {
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
            int offset = Configuration.getRequired(WebDriverConfiguration.Parameter.SCROLL_TO_ELEMENT_Y_OFFSET, Integer.class);
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
        doAction(ACTION_NAME.ATTACH_FILE, getDefaultWaitTimeout(), getDefaultElementWaitCondition(), filePath);
    }

    /**
     * Check checkbox
     * <p>
     * for checkbox Element
     */
    public void check() {
        doAction(ACTION_NAME.CHECK, getDefaultWaitTimeout(), getDefaultElementWaitCondition());
    }

    /**
     * Uncheck checkbox
     * <p>
     * for checkbox Element
     */
    public void uncheck() {
        doAction(ACTION_NAME.UNCHECK, getDefaultWaitTimeout(), getDefaultElementWaitCondition());
    }

    /**
     * Selects text in specified select element.
     *
     * @param selectText select text
     * @return true if item selected, otherwise false.
     */
    public boolean select(final String selectText) {
        return (boolean) doAction(ACTION_NAME.SELECT, getDefaultWaitTimeout(), getDefaultElementWaitCondition(), selectText);
    }

    /**
     * Select multiple text values in specified select element.
     *
     * @param values final String[]
     * @return boolean.
     */
    public boolean select(final String[] values) {
        return (boolean) doAction(ACTION_NAME.SELECT_VALUES, getDefaultWaitTimeout(), getDefaultElementWaitCondition(), values);
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
        return (boolean) doAction(ACTION_NAME.SELECT_BY_MATCHER, getDefaultWaitTimeout(), getDefaultElementWaitCondition(), matcher);
    }

    /**
     * Selects first value according to partial text value.
     *
     * @param partialSelectText select by partial text
     * @return true if item selected, otherwise false.
     */
    public boolean selectByPartialText(final String partialSelectText) {
        return (boolean) doAction(ACTION_NAME.SELECT_BY_PARTIAL_TEXT, getDefaultWaitTimeout(), getDefaultElementWaitCondition(),
                partialSelectText);
    }

    /**
     * Selects item by index in specified select element.
     *
     * @param index to select by
     * @return true if item selected, otherwise false.
     */
    public boolean select(final int index) {
        return (boolean) doAction(ACTION_NAME.SELECT_BY_INDEX, getDefaultWaitTimeout(), getDefaultElementWaitCondition(), index);
    }

    ///////////// ASSERTS /////////////

    public void assertElementWithTextPresent(final String text) {
        assertElementWithTextPresent(text, getDefaultWaitTimeout());
    }

    public void assertElementWithTextPresent(final String text, long timeout) {
        assertElementWithTextPresent(text, Duration.ofSeconds(timeout));
    }

    public void assertElementWithTextPresent(final String text, Duration timeout) {
        if (!isElementWithTextPresent(text, timeout)) {
            Assert.fail(Messager.ELEMENT_WITH_TEXT_NOT_PRESENT.getMessage(getNameWithLocator(), text));
        }
    }

    public void assertElementPresent() {
        assertElementPresent(getDefaultWaitTimeout());
    }

    public void assertElementPresent(long timeout) {
        assertElementPresent(Duration.ofSeconds(timeout));
    }

    public void assertElementPresent(Duration timeout) {
        if (!isPresent(timeout)) {
            Assert.fail(Messager.ELEMENT_NOT_PRESENT.getMessage(getNameWithLocator()));
        }
    }

    ///////////// OTHER /////////////

    /**
     * Find Extended Web Element on page using By starting search from this
     * object.
     *
     * @param by Selenium By locator
     * @return ExtendedWebElement if exists otherwise null.
     */
    public ExtendedWebElement findExtendedWebElement(By by) {
        return findExtendedWebElement(this, by);
    }

    /**
     * Find Extended Web Element on page using By starting search from this
     * object.
     *
     * @param by Selenium By locator
     * @param timeout to wait
     * @return ExtendedWebElement if exists otherwise null.
     */
    public ExtendedWebElement findExtendedWebElement(By by, long timeout) {
        return findExtendedWebElement(this, by, Duration.ofSeconds(timeout));
    }

    /**
     * Find Extended Web Element on page using By starting search from this
     * object.
     *
     * @param by Selenium By locator
     * @param name Element name
     * @return ExtendedWebElement if exists otherwise null.
     */
    public ExtendedWebElement findExtendedWebElement(final By by, String name) {
        return findExtendedWebElement(this, by, name);
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
    public ExtendedWebElement findExtendedWebElement(final By by, String name, long timeout) {
        return findExtendedWebElement(this, by, name, Duration.ofSeconds(timeout));
    }

    public List<ExtendedWebElement> findExtendedWebElements(By by) {
        return findExtendedWebElements(this, by, getDefaultWaitTimeout());
    }

    /**
     * Get list of {@link ExtendedWebElement}s. Search of elements starts from current {@link ExtendedWebElement}
     *
     * @param by see {@link By}
     * @param timeout timeout of checking the presence of the element(s)
     * @return list of ExtendedWebElements if found, empty list otherwise
     */
    public List<ExtendedWebElement> findExtendedWebElements(final By by, long timeout) {
        return findExtendedWebElements(this, by, Duration.ofSeconds(timeout));
    }

    /**
     * Wait until element disappear
     *
     * @param timeout long
     * @return boolean true if element disappeared and false if still visible
     */
    public boolean waitUntilElementDisappear(final long timeout) {
        clearElementState();
        boolean res = false;
        try {
            if (this.element == null) {
                // if element not found it will cause NoSuchElementException
                findElement();
            }
            // if element is stale, it will cause StaleElementReferenceException
            if (this.element.isDisplayed()) {
                LOGGER.info("Element {} detected. Waiting until disappear...", this.element.getTagName());
            } else {
                LOGGER.info("Element {} is not detected, i.e. disappeared", this.element.getTagName());
                // no sense to continue as element is not displayed so return asap
                return true;
            }
            res = waitUntil(ExpectedConditions.or(ExpectedConditions.stalenessOf(this.element),
                    ExpectedConditions.invisibilityOf(this.element)),
                    timeout);
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            // element not present so means disappear
            LOGGER.debug("Element disappeared as exception catched: {}", e.getMessage());
            res = true;
        }
        return res;
    }

    public ExtendedWebElement format(Object... objects) {
        return format(this, objects);
    }

    public List<ExtendedWebElement> formatToList(Object... objects) {
        return formatToList(this, objects);
    }

    @Override
    public List<WebElement> findElements(By by) {
        List<WebElement> elements = List.of();
        try {
            // do not use getBy method - getText method, for example, takes 3s instead of 1s or even ~600ms!
            // elements = searchContext.findElements(getBy(by, searchContext));
            elements = findElement().findElements(by);
        } catch (NoSuchElementException e) {
            LOGGER.debug("Unable to find elements: {}", e.getMessage());
        }
        return elements;
    }

    @Override
    public WebElement findElement(By by) {
        List<WebElement> elements = findElement().findElements(by);
        WebElement foundElement = null;
        if (elements.size() == 1) {
            foundElement = elements.get(0);
        } else if (elements.size() > 1) {
            foundElement = elements.get(0);
            LOGGER.debug("{} elements detected by: {}", elements.size(), by);
        }
        if (foundElement == null) {
            throw new NoSuchElementException(SpecialKeywords.NO_SUCH_ELEMENT_ERROR + by);
        }
        return foundElement;
    }

    public interface ActionSteps {
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
            Assert.fail("Unsupported UI action name" + actionName.toString());
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
    private Object doAction(ACTION_NAME actionName, Duration timeout, ExpectedCondition<?> waitCondition) {
        // [VD] do not remove null args otherwise all actions without arguments will be broken!
        Object nullArgs = null;
        return doAction(actionName, timeout, waitCondition, nullArgs);
    }

    private Object doAction(ACTION_NAME actionName, Duration timeout, ExpectedCondition<?> waitCondition,
            Object... inputArgs) {
        clearElementState();

        if (waitCondition != null) {
            // do verification only if waitCondition is not null
            if (!waitUntil(waitCondition, timeout)) {
                // TODO: think about raising exception otherwise we do extra call and might wait and hangs especially for mobile/appium
                LOGGER.error(Messager.ELEMENT_CONDITION_NOT_VERIFIED.getMessage(actionName.getKey(), getNameWithLocator()));
            }
        }

        if (isLocalized) {
            isLocalized = false; // single verification is enough for this particular element
            L10N.verify(this);
        }

        Object output = null;

        try {
            this.element = findElement();
            output = overrideAction(actionName, inputArgs);
        } catch (StaleElementReferenceException e) {
            // TODO: analyze mobile testing for staled elements. Potentially it should be fixed by appium java client already
            // sometime Appium instead printing valid StaleElementException generate java.lang.ClassCastException:
            // com.google.common.collect.Maps$TransformedEntriesMap cannot be cast to java.lang.String
            LOGGER.debug("catched StaleElementReferenceException: ", e);
            // try to find again using driver context and do action
            // [AS] do not try to refresh element if it created as part of list,
            // because it can find first element or different (not original) element - unexpected behaviour
            if (by != null) {
                this.element = this.findElement();
                output = overrideAction(actionName, inputArgs);
            } else {
                throw e;
            }
        }
        return output;
    }

    // single place for all supported UI actions in carina core
    private Object overrideAction(ACTION_NAME actionName, Object... inputArgs) {
        return executeAction(actionName, new ActionSteps() {
            @Override
            public void doClick() {
                DriverListener.setMessages(Messager.ELEMENT_CLICKED.getMessage(getName()),
                        Messager.ELEMENT_NOT_CLICKED.getMessage(getNameWithLocator()));

                element.click();
            }

            @Override
            public void doClickByJs() {
                DriverListener.setMessages(Messager.ELEMENT_CLICKED.getMessage(getName()),
                        Messager.ELEMENT_NOT_CLICKED.getMessage(getNameWithLocator()));

                LOGGER.info("Do click by JavascriptExecutor for element: {}", getNameWithLocator());
                JavascriptExecutor executor = (JavascriptExecutor) getDriver();
                executor.executeScript("arguments[0].click();", element);
            }

            @Override
            public void doClickByActions() {
                DriverListener.setMessages(Messager.ELEMENT_CLICKED.getMessage(getName()),
                        Messager.ELEMENT_NOT_CLICKED.getMessage(getNameWithLocator()));

                LOGGER.info("Do click by Actions for element: {}", getNameWithLocator());
                Actions actions = new Actions(getDriver());
                actions.moveToElement(element).click().perform();
            }

            @Override
            public void doDoubleClick() {
                DriverListener.setMessages(Messager.ELEMENT_DOUBLE_CLICKED.getMessage(getName()),
                        Messager.ELEMENT_NOT_DOUBLE_CLICKED.getMessage(getNameWithLocator()));

                WebDriver drv = getDriver();
                Actions action = new Actions(drv);
                action.moveToElement(element).doubleClick(element).build().perform();
            }

            @Override
            public void doHover(Integer xOffset, Integer yOffset) {
                DriverListener.setMessages(Messager.ELEMENT_HOVERED.getMessage(getName()),
                        Messager.ELEMENT_NOT_HOVERED.getMessage(getNameWithLocator()));

                WebDriver drv = getDriver();
                Actions action = new Actions(drv);
                if (xOffset != null && yOffset != null) {
                    action.moveToElement(element, xOffset, yOffset).build().perform();
                } else {
                    action.moveToElement(element).build().perform();
                }
            }

            @Override
            public void doSendKeys(Keys keys) {
                DriverListener.setMessages(Messager.KEYS_SEND_TO_ELEMENT.getMessage(keys.toString(), getName()),
                        Messager.KEYS_NOT_SEND_TO_ELEMENT.getMessage(keys.toString(), getNameWithLocator()));
                element.sendKeys(keys);
            }

            @Override
            public void doType(String text) {
                final String decryptedText = EncryptorUtils.decrypt(text);
                DriverListener.setMessages(Messager.KEYS_CLEARED_IN_ELEMENT.getMessage(getName()),
                        Messager.KEYS_NOT_CLEARED_IN_ELEMENT.getMessage(getNameWithLocator()));
                element.clear();

                String textLog = (!decryptedText.equals(text) ? "********" : text);

                DriverListener.setMessages(Messager.KEYS_SEND_TO_ELEMENT.getMessage(textLog, getName()),
                        Messager.KEYS_NOT_SEND_TO_ELEMENT.getMessage(textLog, getNameWithLocator()));
                element.sendKeys(decryptedText);
            }

            @Override
            public void doAttachFile(String filePath) {
                final String decryptedText = EncryptorUtils.decrypt(FilenameUtils.separatorsToUnix(filePath));

                String textLog = (!decryptedText.equals(filePath) ? "********" : filePath);

                DriverListener.setMessages(Messager.FILE_ATTACHED.getMessage(textLog, getName()),
                        Messager.FILE_NOT_ATTACHED.getMessage(textLog, getNameWithLocator()));

                ((JavascriptExecutor) getDriver()).executeScript("arguments[0].style.display = 'block';", element);
                WebElement originalWebElement = element instanceof Decorated<?> ? (WebElement) ((Decorated<?>) element).getOriginal()
                        : element;
                ((RemoteWebElement) originalWebElement).setFileDetector(new LocalFileDetector());
                originalWebElement.sendKeys(decryptedText);
            }

            @Override
            public String doGetText() {
                String text = element.getText();
                LOGGER.debug(Messager.ELEMENT_ATTRIBUTE_FOUND.getMessage("Text", text, getName()));
                return text;
            }

            @Override
            public Point doGetLocation() {
                Point point = element.getLocation();
                LOGGER.debug(Messager.ELEMENT_ATTRIBUTE_FOUND.getMessage("Location", point.toString(), getName()));
                return point;
            }

            @Override
            public Dimension doGetSize() {
                Dimension dim = element.getSize();
                LOGGER.debug(Messager.ELEMENT_ATTRIBUTE_FOUND.getMessage("Size", dim.toString(), getName()));
                return dim;
            }

            @Override
            public String doGetAttribute(String name1) {
                String attribute = element.getAttribute(name1);
                LOGGER.debug(Messager.ELEMENT_ATTRIBUTE_FOUND.getMessage(name1, attribute, getName()));
                return attribute;
            }

            @Override
            public void doRightClick() {
                DriverListener.setMessages(Messager.ELEMENT_RIGHT_CLICKED.getMessage(getName()),
                        Messager.ELEMENT_NOT_RIGHT_CLICKED.getMessage(getNameWithLocator()));

                WebDriver drv = getDriver();
                Actions action = new Actions(drv);
                action.moveToElement(element).contextClick(element).build().perform();
            }

            @Override
            public void doCheck() {
                DriverListener.setMessages(Messager.CHECKBOX_CHECKED.getMessage(getName()), null);

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
                DriverListener.setMessages(Messager.CHECKBOX_UNCHECKED.getMessage(getName()), null);

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

                boolean res = element.isSelected();
                if (element.getAttribute("checked") != null) {
                    res |= element.getAttribute("checked").equalsIgnoreCase("true");
                }
                return res;
            }

            @Override
            public boolean doSelect(String text) {
                final String decryptedSelectText = EncryptorUtils.decrypt(text);

                String textLog = (!decryptedSelectText.equals(text) ? "********" : text);

                DriverListener.setMessages(Messager.SELECT_BY_TEXT_PERFORMED.getMessage(textLog, getName()),
                        Messager.SELECT_BY_TEXT_NOT_PERFORMED.getMessage(textLog, getNameWithLocator()));

                final Select s = new Select(findElement());
                // [VD] do not use selectByValue as modern controls could have only visible value without value
                s.selectByVisibleText(decryptedSelectText);
                return true;
            }

            @Override
            public boolean doSelectValues(String[] values) {
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

                DriverListener.setMessages(Messager.SELECT_BY_MATCHER_TEXT_PERFORMED.getMessage(matcher.toString(), getName()),
                        Messager.SELECT_BY_MATCHER_TEXT_NOT_PERFORMED.getMessage(matcher.toString(), getNameWithLocator()));

                final Select s = new Select(findElement());
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

                DriverListener.setMessages(
                        Messager.SELECT_BY_TEXT_PERFORMED.getMessage(partialSelectText, getName()),
                        Messager.SELECT_BY_TEXT_NOT_PERFORMED.getMessage(partialSelectText, getNameWithLocator()));

                final Select s = new Select(findElement());
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
                DriverListener.setMessages(
                        Messager.SELECT_BY_INDEX_PERFORMED.getMessage(String.valueOf(index), getName()),
                        Messager.SELECT_BY_INDEX_NOT_PERFORMED.getMessage(String.valueOf(index), getNameWithLocator()));

                final Select s = new Select(findElement());
                s.selectByIndex(index);
                return true;
            }

            @Override
            public String doGetSelectedValue() {
                final Select s = new Select(findElement());
                return s.getAllSelectedOptions().get(0).getText();
            }

            @Override
            public List<String> doGetSelectedValues() {
                final Select s = new Select(findElement());
                List<String> values = new ArrayList<>();
                for (WebElement we : s.getAllSelectedOptions()) {
                    values.add(we.getText());
                }
                return values;
            }

        }, inputArgs);
    }

    /**
     * Get default waiting condition depends on {@link ElementLoadingStrategy}
     *
     * @return {@link ExpectedCondition}
     */
    @SuppressWarnings("squid:S1452")
    protected ExpectedCondition<?> getDefaultElementWaitCondition() {
        clearElementState();
        if(loadingStrategy == ElementLoadingStrategy.NONE) {
            return (ExpectedCondition<Boolean>) input -> true;
        }
        List<ExpectedCondition<?>> conditions = new ArrayList<>();
        if (loadingStrategy == ElementLoadingStrategy.BY_PRESENCE || loadingStrategy == ElementLoadingStrategy.BY_PRESENCE_OR_VISIBILITY) {
            if (element != null) {
                conditions.add(ExpectedConditions.not(ExpectedConditions.stalenessOf(element)));
            }
            if (by != null) {
                conditions.add(getSearchContext() instanceof WebElement
                        ? ExpectedConditions.presenceOfNestedElementLocatedBy((WebElement) searchContext, by)
                        : ExpectedConditions.presenceOfElementLocated(by));
            }
        }
        if (loadingStrategy == ElementLoadingStrategy.BY_VISIBILITY || loadingStrategy == ElementLoadingStrategy.BY_PRESENCE_OR_VISIBILITY) {
            if (element != null) {
                conditions.add(ExpectedConditions.not(ExpectedConditions.invisibilityOf(element)));
            }
            if (by != null) {
                conditions.add(getSearchContext() instanceof WebElement
                        ?  ExpectedConditions.visibilityOfNestedElementsLocatedBy((WebElement) searchContext, by)
                        : ExpectedConditions.visibilityOfElementLocated(by));
            }
        }
        return ExpectedConditions.or(conditions.toArray(new ExpectedCondition<?>[0]));
    }

    ///////////// UTILITY METHODS /////////////

    private WebElement findElement() {
        if (by == null) {
            if (element == null) {
                throw new IllegalArgumentException("Both 'By' and 'WebElement' could not be null.");
            }
            return element;
        }

        List<WebElement> elements = searchContext.findElements(by);
        if (elements.isEmpty()) {
            throw new NoSuchElementException(SpecialKeywords.NO_SUCH_ELEMENT_ERROR + this.by.toString());
        }
        if (elements.size() > 1) {
            LOGGER.info("returned first but found {} elements by locator: {}", elements.size(), getBy());
        }
        element = elements.get(0);
        return element;
    }

    /**
     * todo add description
     */
    protected void clearElementState() {
        if (element == null && by == null) {
            throw new IllegalStateException(String.format("By and WebElement both could not be null. Element: %s", getDetailedInfo()));
        }
        if(by != null) {
            element = null;
        }
    }

    private String getDetailedInfo() {
        return this.getClass() + "{" +
                "udid='" + udid + '\'' +
                ", driver=" + driver +
                ", searchContext=" + searchContext +
                ", by=" + by +
                ", element=" + element +
                ", name='" + name + '\'' +
                ", loadingStrategy=" + loadingStrategy +
                ", isLocalized=" + isLocalized +
                '}';
    }

    /**
     * @deprecated useless method
     */
    @Deprecated(forRemoval = true)
    public void refresh() {
        // nothing
    }

    @Override
    public String toString() {
        return name;
    }

    @SuppressWarnings({ "squid:S2975", "squid:S1182" })
    @Override
    public Object clone() throws CloneNotSupportedException {
        try {
            ExtendedWebElement clone = ConstructorUtils.invokeConstructor(this.getClass(), getDriver(), getSearchContext());
            clone.setBy(by);
            clone.setElement(element);
            clone.setName(name);
            return clone;
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            return ExceptionUtils.rethrow(e);
        }
    }
}
