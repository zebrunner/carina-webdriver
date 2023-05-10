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

import static io.appium.java_client.pagefactory.utils.WebDriverUnpackUtility.getCurrentContentType;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hamcrest.BaseMatcher;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.InvalidArgumentException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
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
import com.zebrunner.carina.utils.annotations.Beta;
import com.zebrunner.carina.utils.annotations.Internal;
import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.utils.messager.Messager;
import com.zebrunner.carina.utils.resources.L10N;
import com.zebrunner.carina.webdriver.AbstractContext;
import com.zebrunner.carina.webdriver.decorator.ElementLoadingStrategy;
import com.zebrunner.carina.webdriver.listener.DriverListener;
import com.zebrunner.carina.webdriver.locator.LocatorType;
import com.zebrunner.carina.webdriver.locator.converter.LocatorConverter;

import io.appium.java_client.pagefactory.bys.ContentMappedBy;

/**
 * Abstraction over any elements/components.
 *
 * The main rules that the logic of the methods follows are:<br>
 * 1. If 'By' of the element is not null, it will be used for element's search and recreating. <b>Important</b>:
 * do not add 'By' for element if it is part of the list, or if you really want to add it, use only XPath with the index. Other types
 * of locators don't support indexing.<br>
 * 2. The element must contain either a {@link By} or a ready {@link WebElement} or both.<br>
 * 3. The only source of data, such as {@link SearchContext}, {@link By}, and so on, should be the current class, all getters and setters are closed
 * from overriding.<br>
 * 4. If values are specified in child constructors using setters, they will take precedence when creating an element than those that are passed
 * through the builder. This is necessary for those cases when the component is created by the user and, for example, in order not to constantly
 * specify its locator in the builder, you can specify it in the constructor using {@link #setBy(By)}<br>
 * 5. The main method for finding an element to perform actions/updates is the method {@link #findElement()}
 */
public abstract class AbstractUIObject extends AbstractContext implements IWebElement, WebElement {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Class<?> clazz = null;
    private By originalBy = null;
    private By by = null;
    private WebElement element = null;
    // searchContext is used for searching element by default
    private SearchContext searchContext = null;
    // element description (for logging)
    private String name = null;
    private String localizationKey = null;
    private boolean isL10NVerified = false;
    private LinkedList<LocatorConverter> locatorConverters = null;
    /**
     * @deprecated the field will be hidden. Use {@link #getLoadingStrategy()} or {@link #setLoadingStrategy(ElementLoadingStrategy)} instead
     */
    @Deprecated(forRemoval = true, since = "1.0.3")
    protected ElementLoadingStrategy loadingStrategy;

    /**
     * Initializes UI object using {@link PageFactory}. Browser area for internal elements initialization is bordered by
     * current instance.
     * <p>
     * Note: implement this constructor if you want your {@link AbstractUIObject} instances marked with {@link FindBy}
     * to be auto-initialized on {@link AbstractPage} inheritors
     *
     * @param driver {@link WebDriver} instance to initialize UI Object fields using PageFactory
     * @param searchContext ignored, deprecated
     */
    @Deprecated
    public AbstractUIObject(WebDriver driver, @Deprecated(forRemoval = true, since = "1.0.3") SearchContext searchContext) {
        // the constructor must remain public, since when creating classes and inheriting
        // from the current one, a constructor with the protected modifier is automatically created
        this(driver);
    }

    /**
     * Initializes UI object using {@link PageFactory}. Browser area for internal elements initialization is bordered by
     * current instance.
     * <p>
     * Note: implement this constructor if you want your {@link AbstractUIObject} instances marked with {@link FindBy}
     * to be auto-initialized on {@link AbstractPage} inheritors
     *
     * @param driver {@link WebDriver} instance to initialize UI Object fields using PageFactory
     */
    public AbstractUIObject(WebDriver driver) {
        // the constructor must remain public, since when creating classes and inheriting
        // from the current one, a constructor with the protected modifier is automatically created
        super(driver, null);
    }

    /**
     * Builder for creating elements/components inheriting from {@link AbstractUIObject} class
     */
    public static final class Builder {
        // default values
        private WebDriver driver;
        private By by = null;
        private SearchContext searchContext;
        private WebElement element = null;
        private String localizationKey = null;
        // for beautiful logging only
        private String name = "n/a";
        private LinkedList<LocatorConverter> locatorConverters = new LinkedList<>();
        private ElementLoadingStrategy loadingStrategy = ElementLoadingStrategy.valueOf(Configuration.get(Parameter.ELEMENT_LOADING_STRATEGY));

        /**
         * Get instance Builder
         * 
         * @return {@link Builder}
         */
        public static Builder getInstance() {
            return new Builder();
        }

        public WebDriver getDriver() {
            return driver;
        }

        /**
         * Set WebDriver. <b>Required parameter.</b>
         * 
         * @param driver {@link WebDriver}
         * @return {@link Builder}
         */
        public Builder setDriver(WebDriver driver) {
            this.driver = driver;
            return this;
        }

        public By getBy() {
            return by;
        }

        /**
         * Set a locator to find an element. See {@link By}, {@link io.appium.java_client.AppiumBy}.
         * <b>Required parameter if element parameter is not specified</b>
         * 
         * @param by see {@link By}, {@link io.appium.java_client.AppiumBy}
         * @return {@link Builder}
         */
        public Builder setBy(@Nullable By by) {
            this.by = by;
            return this;
        }

        public SearchContext getSearchContext() {
            return searchContext;
        }

        /**
         * Set the context in which the current element should be searched for. <b>Required parameter.</b>
         * 
         * @param searchContext see {@link SearchContext}. Can be {@link WebDriver} or {@link WebElement}
         * @return {@link Builder}
         */
        public Builder setSearchContext(@Nonnull SearchContext searchContext) {
            Objects.requireNonNull(searchContext);
            this.searchContext = searchContext;
            return this;
        }

        public WebElement getElement() {
            return element;
        }

        /**
         * Specify the element that will be used to perform the actions.
         * <b>Required parameter if {@link By} parameter is not specified</b>.
         * 
         * @param element see {@link WebElement}
         * @return {@link Builder}
         */
        public Builder setElement(@Nullable WebElement element) {
            this.element = element;
            return this;
        }

        public Optional<String> getLocalizationKey() {
            return Optional.ofNullable(localizationKey);
        }

        /**
         * Specify a localization key that will be used to search for localization in resources.
         * <b>optional parameter</b>
         * 
         * @param key for example {@code WikipediaLocalePage.welcomeTextId}
         * @return {@link Builder}
         */
        public Builder setLocalizationKey(@Nullable String key) {
            this.localizationKey = key;
            return this;
        }

        public String getDescriptionName() {
            return name;
        }

        /**
         * Specify the name of the element to be used when logging / in errors.
         * <b>optional parameter</b>
         * 
         * @param name name of the element
         * @return {@link Builder}
         */
        public Builder setDescriptionName(@Nonnull String name) {
            Objects.requireNonNull(name);
            this.name = name;
            return this;
        }

        @Internal
        public LinkedList<LocatorConverter> getLocatorConverters() {
            return this.locatorConverters;
        }

        /**
         * Set the converters used to change the locator.
         * 
         * @param converters see {@link LocatorConverter}
         * @return {@link Builder}
         */
        @Internal
        public Builder setLocatorConverters(@Nonnull LinkedList<LocatorConverter> converters) {
            Objects.requireNonNull(converters);
            this.locatorConverters = converters;
            return this;
        }

        public ElementLoadingStrategy getLoadingStrategy() {
            return loadingStrategy;
        }

        /**
         * Set element loading strategy.
         * 
         * @param loadingStrategy see {@link ElementLoadingStrategy}
         */
        public Builder setLoadingStrategy(@Nonnull ElementLoadingStrategy loadingStrategy) {
            Objects.requireNonNull(loadingStrategy);
            this.loadingStrategy = loadingStrategy;
            return this;
        }

        /**
         * Create element/component. Parameters will be validated before the element is created.
         * If the parameters were specified in the constructor using setters, they will take precedence over those specified in the builder.
         * 
         * @param clazz class descendant of class {@link AbstractUIObject}
         * @param <T> {@link AbstractUIObject}
         * @return {@link T}
         */
        public <T extends AbstractUIObject> T build(Class<T> clazz) {
            Objects.requireNonNull(clazz);
            try {
                T object;
                Constructor<T> constructor = ConstructorUtils.getAccessibleConstructor(clazz, WebDriver.class);
                if (constructor != null) {
                    object = constructor.newInstance(driver);
                } else {
                    constructor = ConstructorUtils.getAccessibleConstructor(clazz, WebDriver.class, SearchContext.class);
                    if (constructor == null) {
                        throw new NoSuchMethodException(String
                                .format("Cannot find constructor with parameters (WebDriver) or (WebDriver, SearchContext) for '%s' class from '%s' search context.",
                                        clazz, searchContext));
                    }
                    object = constructor.newInstance(driver, null);
                }
                object.setClazz(clazz);
                // we check for null before setting value because value that setted by user in constructor has more priority

                // we should set converters before setting locator
                if (object.getLocatorConverters() == null) {
                    object.setLocatorConverters(locatorConverters);
                }

                if (object.getBy().isEmpty()) {
                    object.setBy(by);
                } else {
                    // for recreating locator (if user set by using constructor)
                    object.buildBy();
                }

                if (object.getSearchContext() == null) {
                    object.setSearchContext(searchContext);
                }

                if (object.getElement().isEmpty()) {
                    object.setElement(element);
                }

                if (object.getLocalizationKey().isEmpty()) {
                    object.setLocalizationKey(localizationKey);
                }

                if (object.getDescriptionName() == null) {
                    object.setDescriptionName(name);
                }

                if (object.getLoadingStrategy() == null) {
                    object.setLoadingStrategy(loadingStrategy);
                }

                validate(object);
                return object;
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Implement appropriate AbstractUIObject constructor for auto-initialization!", e);
            } catch (Exception e) {
                throw new RuntimeException("Error creating UIObject!", e);
            }
        }

        private void validate(AbstractUIObject uiObject) {
            if (uiObject.getBy().isEmpty() && uiObject.getElement().isEmpty()) {
                throw new IllegalArgumentException("By and WebElement must not be null at the same time.");
            }

            if (uiObject.getDriver() == null) {
                throw new IllegalArgumentException("WebDriver must not be null.");
            }

            if (uiObject.getSearchContext() == null) {
                throw new IllegalArgumentException("Search Context must not be null.");
            }

            if (uiObject.getLocatorConverters() == null) {
                throw new IllegalStateException("LocatorConverters must not be null.");
            }

            if (uiObject.getLoadingStrategy() == null) {
                throw new IllegalStateException("Loading strategy must not be null.");
            }
        }
    }

    /**
     * Build by from original by using converters
     */
    @Internal
    protected void buildBy() {
        if (originalBy == null) {
            by = null;
            return;
        }
        if (!locatorConverters.isEmpty()) {
            String byAsString = originalBy.toString();
            for (LocatorConverter converter : locatorConverters) {
                byAsString = converter.convert(byAsString);
            }

            String finalByAsString = byAsString;
            by = Arrays.stream(LocatorType.values())
                    .filter(locatorType -> locatorType.is(finalByAsString))
                    .findFirst()
                    .orElseThrow()
                    .buildLocatorFromString(byAsString);
        } else {
            by = originalBy;
        }
    }

    // --------------------------------------------------------------------------
    // Getters / Setters
    // Must be final because the only data source is the current class.
    // --------------------------------------------------------------------------

    public final Class<?> getClazz() {
        return clazz;
    }

    public final <T extends AbstractUIObject> void setClazz(Class<T> clazz) {
        this.clazz = clazz;
    }

    public final Optional<By> getBy() {
        return Optional.ofNullable(by);
    }

    @Internal
    public final Optional<By> getOriginalBy() {
        return Optional.ofNullable(originalBy);
    }

    public final void setBy(@Nullable By by) {
        originalBy = by;
        buildBy();
    }

    public final Optional<WebElement> getElement() {
        return Optional.ofNullable(element);
    }

    public final void setElement(@Nullable WebElement element) {
        this.element = element;
    }

    // todo refactor IWebElement interface - change to getLocalizeName

    /**
     * Get localization name
     * 
     * @return localization name if exists, "" otherwise
     */
    @Internal
    @Override
    public final String getName() {
        // todo refactor - change to getLocalizeName
        return localizationKey;
    }

    /**
     * Set localization name
     * 
     * @param name the key by which the search element will be produced in the localization resources
     */
    @Internal
    public final void setName(@Nullable String name) {
        this.localizationKey = name;
    }

    public final Optional<String> getLocalizationKey() {
        return Optional.ofNullable(localizationKey);
    }

    public final void setLocalizationKey(@Nullable String key) {
        this.localizationKey = key;
    }

    public final ElementLoadingStrategy getLoadingStrategy() {
        return loadingStrategy;
    }

    public final void setLoadingStrategy(@Nonnull ElementLoadingStrategy loadingStrategy) {
        Objects.requireNonNull(loadingStrategy);
        this.loadingStrategy = loadingStrategy;
    }

    public final SearchContext getSearchContext() {
        return searchContext;
    }

    public final void setSearchContext(@Nonnull SearchContext searchContext) {
        Objects.requireNonNull(searchContext);
        this.searchContext = searchContext;
    }

    public final String getDescriptionName() {
        return name;
    }

    public final void setDescriptionName(@Nonnull String name) {
        Objects.requireNonNull(name);
        this.name = name;
    }

    public final LinkedList<LocatorConverter> getLocatorConverters() {
        return locatorConverters;
    }

    public final void setLocatorConverters(@Nonnull LinkedList<LocatorConverter> locatorConverters) {
        Objects.requireNonNull(locatorConverters);
        this.locatorConverters = locatorConverters;
    }

    @Override
    public String toString() {
        return name;
    }

    // --------------------------------------------------------------------------
    // Methods for checking presence / visibility of the element
    // --------------------------------------------------------------------------

    @Beta
    @Override
    public boolean isDisplayed() {
        try {
            return findElement().isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    @Beta
    @Override
    public boolean isEnabled() {
        return findElement().isEnabled();
    }

    @Beta
    public boolean isSelected() {
        return findElement().isSelected();
    }

    /**
     * Check the presence of the {@link AbstractUIObject} on the page.
     *
     * @throws AssertionError f the element is not present on the page
     */
    public void assertUIObjectPresent() {
        assertUIObjectPresent(EXPLICIT_TIMEOUT);
    }

    /**
     * Check the presence of the {@link AbstractUIObject} on the page.
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
    @Override
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
            conditions.add(ExpectedConditions.not(ExpectedConditions.invisibilityOf(element)));
        }

        if (by != null) {
            if (searchContext instanceof WebElement) {
                conditions.add(ExpectedConditions.visibilityOfNestedElementsLocatedBy((WebElement) searchContext, by));
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
            if (searchContext instanceof WebElement) {
                conditions.add(ExpectedConditions.visibilityOfNestedElementsLocatedBy((WebElement) searchContext, by));
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
            if (searchContext instanceof WebElement) {
                ExpectedCondition<?> condition = new ExpectedCondition<Boolean>() {
                    @Override
                    public Boolean apply(WebDriver driver) {
                        try {
                            String elementText = searchContext.findElement(by).getText();
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

    @Override
    public void submit() {
        doAction(ACTION_NAME.SUBMIT, EXPLICIT_TIMEOUT, getDefaultCondition());
    }

    @Override
    public void sendKeys(CharSequence... keysToSend) {
        doAction(ACTION_NAME.SEND_KEYS_CHAR_SEQUENCE, EXPLICIT_TIMEOUT, getDefaultCondition(), List.of(keysToSend));

    }

    @Beta
    @Override
    public void clear() {
        doAction(ACTION_NAME.CLEAR, EXPLICIT_TIMEOUT, getDefaultCondition());
    }

    /**
     * Click on element.
     */
    @Override
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

    @Override
    public String getTagName() {
        return (String) doAction(ACTION_NAME.GET_TAG_NAME, EXPLICIT_TIMEOUT, getDefaultCondition());
    }

    @Override
    public Rectangle getRect() {
        return (Rectangle) doAction(ACTION_NAME.GET_RECT, EXPLICIT_TIMEOUT, getDefaultCondition());
    }

    @Override
    public String getCssValue(String propertyName) {
        return (String) doAction(ACTION_NAME.GET_CSS_VALUE, EXPLICIT_TIMEOUT, getDefaultCondition(), propertyName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        return (X) doAction(ACTION_NAME.GET_SCREENSHOT, EXPLICIT_TIMEOUT, getDefaultCondition(), target);
    }

    @Override
    public String getDomProperty(String name) {
        return (String) doAction(ACTION_NAME.GET_DOM_PROPERTY, EXPLICIT_TIMEOUT, getDefaultCondition(), name);
    }

    @Override
    public String getDomAttribute(String name) {
        return (String) doAction(ACTION_NAME.GET_DOM_ATTRIBUTE, EXPLICIT_TIMEOUT, getDefaultCondition(), name);
    }

    @Override
    public String getAriaRole() {
        return (String) doAction(ACTION_NAME.GET_DOM_ATTRIBUTE, EXPLICIT_TIMEOUT, getDefaultCondition());
    }

    @Override
    public String getAccessibleName() {
        return (String) doAction(ACTION_NAME.GET_ACCESSIBLE_NAME, EXPLICIT_TIMEOUT, getDefaultCondition());
    }

    /**
     * Get element text.
     *
     * @return String text
     */
    @Override
    public String getText() {
        return (String) doAction(ACTION_NAME.GET_TEXT, EXPLICIT_TIMEOUT, getDefaultCondition());
    }

    /**
     * Get element location.
     *
     * @return Point location
     */
    @Override
    public Point getLocation() {
        return (Point) doAction(ACTION_NAME.GET_LOCATION, EXPLICIT_TIMEOUT, getDefaultCondition());
    }

    /**
     * Get element size.
     *
     * @return Dimension size
     */
    @Override
    public Dimension getSize() {
        return (Dimension) doAction(ACTION_NAME.GET_SIZE, EXPLICIT_TIMEOUT, getDefaultCondition());
    }

    /**
     * Get element attribute.
     *
     * @param name of attribute
     * @return String attribute value
     */
    @Override
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

    @Override
    public List<WebElement> findElements(By by) {
        List<WebElement> elements = new ArrayList<>();
        try {
            elements = findElement().findElements(getBy(by, searchContext));
        } catch (NoSuchElementException e) {
            LOGGER.debug("Unable to find elements: {}", e.getMessage());
        }
        return elements;
    }

    @Override
    public WebElement findElement(By by) {
        Objects.requireNonNull(by);
        List<WebElement> elements = findElement().findElements(getBy(by, searchContext));

        WebElement nestedElement = null;
        if (elements.size() == 1) {
            nestedElement = elements.get(0);
        } else if (elements.size() > 1) {
            nestedElement = elements.get(0);
            LOGGER.debug("{} elements detected by: {}", elements.size(), by);
        }

        if (nestedElement == null) {
            throw new NoSuchElementException(SpecialKeywords.NO_SUCH_ELEMENT_ERROR + by);
        }
        return nestedElement;
    }

    /**
     * Find {@link T} on page using {@link By} starting search from current element
     *
     * @param clazz class of element that will be created
     * @param by see {@link By}
     * @return {@link T}
     * @throws NoSuchElementException if element was not found
     */
    public <T extends AbstractUIObject> T findNestedElement(Class<T> clazz, By by) {
        return findNestedElement(clazz, by, by.toString(), EXPLICIT_TIMEOUT);
    }

    /**
     * Find {@link T} on page using {@link By} starting search from current element.
     *
     * @param clazz class of element that will be created
     * @param by see {@link By}
     * @param timeout timeout, in seconds
     * @return {@link T}
     * @throws NoSuchElementException if element was not found
     */
    public <T extends AbstractUIObject> T findNestedElement(Class<T> clazz, By by, long timeout) {
        return findNestedElement(clazz, by, clazz.getSimpleName(), timeout);
    }

    /**
     * Find {@link T} on page using By starting search from current element
     *
     * @param clazz class of element that will be created
     * @param by see {@link By}
     * @param name name of an element
     * @return {@link T}
     * @throws NoSuchElementException is element was not found
     */
    public <T extends AbstractUIObject> T findNestedElement(Class<T> clazz, final By by, String name) {
        return findNestedElement(clazz, by, name, EXPLICIT_TIMEOUT);
    }

    /**
     * Find {@link T} on page using {@link By} starting search from current element.
     *
     * @param clazz class of element that will be created
     * @param by see {@link By}
     * @param name name of the element
     * @param timeout timeout in seconds
     * @return {@link T}
     * @throws NoSuchElementException if element was not found
     */
    public <T extends AbstractUIObject> T findNestedElement(Class<T> clazz, final By by, String name, long timeout) {
        T el = AbstractUIObject.Builder.getInstance()
                .setBy(by)
                .setDescriptionName(name)
                .setDriver(getDriver())
                .setSearchContext(findElement())
                .build(clazz);
        if (!el.isPresent(timeout)) {
            throw new NoSuchElementException(SpecialKeywords.NO_SUCH_ELEMENT_ERROR + by.toString());
        }
        return el;
    }

    public <T extends AbstractUIObject> List<T> findNestedElements(Class<T> clazz, By by) {
        return findNestedElements(clazz, by, EXPLICIT_TIMEOUT);
    }

    /**
     * Get list of {@link T}s. Search of elements starts from current element.
     *
     * @param clazz class of elements that will be created
     * @param by see {@link By}
     * @param timeout timeout of checking the presence of the element(s)
     * @return list of {@link T} if found, empty list otherwise
     */
    public <T extends AbstractUIObject> List<T> findNestedElements(Class<T> clazz, final By by, long timeout) {
        List<T> extendedWebElements = new ArrayList<>();
        T tempElement = AbstractUIObject.Builder.getInstance()
                .setBy(by)
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
                    .setDescriptionName(clazz.getSimpleName() + " [" + i + "]")
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
                if (searchContext instanceof WebElement) {
                    conditions.add(ExpectedConditions.presenceOfNestedElementLocatedBy((WebElement) searchContext, by));
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
                if (searchContext instanceof WebElement) {
                    conditions.add(ExpectedConditions.visibilityOfNestedElementsLocatedBy((WebElement) searchContext, by));
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
                if (searchContext instanceof WebElement) {
                    conditions.add(ExpectedConditions.presenceOfNestedElementLocatedBy((WebElement) searchContext, by));
                    conditions.add(ExpectedConditions.visibilityOfNestedElementsLocatedBy((WebElement) searchContext, by));
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
     * Used for getting/finding current element for action.
     * 
     * @return see {@link WebElement}
     * @throws NoSuchElementException if element was not found
     * @throws IllegalStateException if {@link #by} and {@link #element} null at the same time
     * @throws StaleElementReferenceException if element is Stale and we cannot update it (there are no locator for updating)
     */
    protected WebElement findElement() {
        // if there is no by, then we simply return the element
        if (by == null) {
            if (element == null) {
                throw new IllegalStateException("WebElement and By cannot be null at the same time.");
            }
            // try to throw StaleElementReferenceException if element is Stale
            element.isDisplayed();
            // if there is no locator, then we cannot try to update the element,
            // so we return the element as is
            return element;
        }

        if (element != null) {
            try {
                // there is no point in refinding the element if it still exists on the page
                // we call this method to check if it exists. if not, we got an exception
                element.isDisplayed();
                return element;
            } catch (StaleElementReferenceException e) {
                // do nothing as element is stale. We will try to update it
            }
        }

        List<WebElement> elements = searchContext.findElements(by);
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
            return name + String.format(" (%s)", by);
        } else {
            return name + " (n/a)";
        }
    }

    // --------------------------------------------------------------------------
    // Utility methods (for internal usage only) (should have private visibility)
    // --------------------------------------------------------------------------

    /**
     * From io.appium.java_client.pagefactory.AppiumElementLocator
     * This methods makes sets some settings of the {@link By} according to
     * the given instance of {@link SearchContext}. If there is some {@link ContentMappedBy}
     * then it is switched to the searching for some html or native mobile element.
     * Otherwise nothing happens there.
     *
     * @param currentBy is some locator strategy
     * @param currentContent is an instance of some subclass of the {@link SearchContext}.
     * @return the corrected {@link By} for the further searching
     *
     */
    @Internal
    private By getBy(By currentBy, SearchContext currentContent) {
        if (!ContentMappedBy.class.isAssignableFrom(currentBy.getClass())) {
            return currentBy;
        }

        return ContentMappedBy.class.cast(currentBy)
                .useContent(getCurrentContentType(currentContent));
    }

    @Internal
    private Object executeAction(ACTION_NAME actionName, ActionSteps actionSteps, Object... inputArgs) {
        Object result = null;
        switch (actionName) {
        case SUBMIT:
            actionSteps.doSubmit();
            break;
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
        case SEND_KEYS_CHAR_SEQUENCE:
            actionSteps.doSendKeys((List<CharSequence>) inputArgs[0]);
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
        case GET_TAG_NAME:
            result = actionSteps.doGetTagName();
            break;
        case CLEAR:
            actionSteps.doClear();
            break;
        case GET_RECT:
            result = actionSteps.doGetRect();
            break;
        case GET_CSS_VALUE:
            result = actionSteps.doGetCssValue((String) inputArgs[0]);
            break;
        case GET_DOM_PROPERTY:
            result = actionSteps.doGetDomProperty((String) inputArgs[0]);
            break;
        case GET_SCREENSHOT:
            result = actionSteps.doGetScreenshot((OutputType) inputArgs[0]);
            break;
        case GET_DOM_ATTRIBUTE:
            result = actionSteps.doGetDomAttribute((String) inputArgs[0]);
            break;
        case GET_ARIA_ROLE:
            result = actionSteps.doGetAriaRole();
            break;
        case GET_ACCESSIBLE_NAME:
            result = actionSteps.doGetAccessibleName();
            break;
        default:
            Assert.fail("Unsupported UI action name" + actionName);
            break;
        }
        return result;
    }

    /**
     * doAction on element
     *
     * @param actionName {@link ACTION_NAME}
     * @param timeout timeout, in seconds
     * @param waitCondition to check element conditions before action, see {@link ExpectedCondition}
     * @return {@link Object}
     */
    @Internal
    private Object doAction(ACTION_NAME actionName, long timeout, ExpectedCondition<?> waitCondition) {
        // [VD] do not remove null args otherwise all actions without arguments will be broken!
        Object nullArgs = null;
        return doAction(actionName, timeout, waitCondition, nullArgs);
    }

    private Object doAction(ACTION_NAME actionName, long timeout, @Nullable ExpectedCondition<?> waitCondition, Object... inputArgs) {
        if (waitCondition != null && !waitUntil(waitCondition, timeout)) {
            // do verification only if waitCondition is not null
                throw new NoSuchElementException(Messager.ELEMENT_CONDITION_NOT_VERIFIED.getMessage(actionName.getKey(), getNameWithLocator()));
        }
        
        if (localizationKey != null && !localizationKey.isEmpty() && !isL10NVerified) {
            // single verification is enough for this particular element
            isL10NVerified = true;
            L10N.verify(this);
        }
        findElement();
        return overrideAction(actionName, inputArgs);
    }

    @Internal
    // single place for all supported UI actions in carina core
    private Object overrideAction(ACTION_NAME actionName, Object... inputArgs) {
        return executeAction(actionName, new ActionSteps() {
            @Override
            public void doSubmit() {
                DriverListener.setMessages(String.format("PASS: element '%s' is submitted.", name),
                        String.format("FAIL: element '%s' is not submitted!", getNameWithLocator()));
                element.submit();
                // todo check is submit checked in driver listener
            }

            @Override
            public void doClick() {
                DriverListener.setMessages(Messager.ELEMENT_CLICKED.getMessage(name), Messager.ELEMENT_NOT_CLICKED.getMessage(getNameWithLocator()));
                element.click();
            }

            @Override
            public void doClickByJs() {
                DriverListener.setMessages(Messager.ELEMENT_CLICKED.getMessage(name), Messager.ELEMENT_NOT_CLICKED.getMessage(getNameWithLocator()));
                LOGGER.info("Do click by JavascriptExecutor for element: {}", getNameWithLocator());
                ((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", element);
            }

            @Override
            public void doClickByActions() {
                DriverListener.setMessages(Messager.ELEMENT_CLICKED.getMessage(name), Messager.ELEMENT_NOT_CLICKED.getMessage(getNameWithLocator()));
                LOGGER.info("Do click by Actions for element: {}", getNameWithLocator());
                new Actions(getDriver()).moveToElement(element)
                        .click()
                        .perform();
            }

            @Override
            public void doDoubleClick() {
                DriverListener.setMessages(Messager.ELEMENT_DOUBLE_CLICKED.getMessage(name),
                        Messager.ELEMENT_NOT_DOUBLE_CLICKED.getMessage(getNameWithLocator()));
                new Actions(getDriver()).moveToElement(element)
                        .doubleClick(element)
                        .build()
                        .perform();
            }

            @Override
            public void doHover(@Nullable Integer xOffset, @Nullable Integer yOffset) {
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
                DriverListener.setMessages(Messager.KEYS_SEND_TO_ELEMENT.getMessage(keys.toString(), name),
                        Messager.KEYS_NOT_SEND_TO_ELEMENT.getMessage(keys.toString(), getNameWithLocator()));
                element.sendKeys(keys);
            }

            @Override
            public void doSendKeys(List<CharSequence> keys) {
                Objects.requireNonNull(keys);
                DriverListener.setMessages(Messager.KEYS_SEND_TO_ELEMENT.getMessage(keys.toString(), name),
                        Messager.KEYS_NOT_SEND_TO_ELEMENT.getMessage(keys.toString(), getNameWithLocator()));
                element.sendKeys(keys.toArray(new CharSequence[0]));
            }

            @Override
            public void doType(String text) {
                Objects.requireNonNull(text);
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
                String text = element.getText();
                LOGGER.debug(Messager.ELEMENT_ATTRIBUTE_FOUND.getMessage("Text", text, name));
                return text;
            }

            @Override
            public Point doGetLocation() {
                Point point = element.getLocation();
                LOGGER.debug(Messager.ELEMENT_ATTRIBUTE_FOUND.getMessage("Location", point.toString(), name));
                return point;
            }

            @Override
            public Dimension doGetSize() {
                Dimension dim = element.getSize();
                LOGGER.debug(Messager.ELEMENT_ATTRIBUTE_FOUND.getMessage("Size", dim.toString(), name));
                return dim;
            }

            @Override
            public String doGetAttribute(String attributeName) {
                Objects.requireNonNull(attributeName);
                String attribute = element.getAttribute(attributeName);
                LOGGER.debug(Messager.ELEMENT_ATTRIBUTE_FOUND.getMessage(attributeName, attribute, name));
                return attribute;
            }

            @Override
            public void doRightClick() {
                DriverListener.setMessages(Messager.ELEMENT_RIGHT_CLICKED.getMessage(name),
                        Messager.ELEMENT_NOT_RIGHT_CLICKED.getMessage(getNameWithLocator()));
                Actions action = new Actions(getDriver());
                action.moveToElement(element).contextClick(element).build().perform();
            }

            @Override
            public void doCheck() {
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
                boolean res = element.isSelected();
                if (element.getAttribute("checked") != null) {
                    res |= element.getAttribute("checked").equalsIgnoreCase("true");
                }
                return res;
            }

            @Override
            public boolean doSelect(String text) {
                Objects.requireNonNull(text);
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
                DriverListener.setMessages(
                        Messager.SELECT_BY_INDEX_PERFORMED.getMessage(String.valueOf(index), name),
                        Messager.SELECT_BY_INDEX_NOT_PERFORMED.getMessage(String.valueOf(index), getNameWithLocator()));
                final Select s = new Select(element);
                s.selectByIndex(index);
                return true;
            }

            @Override
            public String doGetSelectedValue() {
                final Select s = new Select(element);
                return s.getAllSelectedOptions().get(0).getText();
            }

            @Override
            public List<String> doGetSelectedValues() {
                final Select s = new Select(element);
                List<String> values = new ArrayList<>();
                for (WebElement we : s.getAllSelectedOptions()) {
                    values.add(we.getText());
                }
                return values;
            }

            @Override
            public void doClear() {
                DriverListener.setMessages(String.format("PASS: element '%s' is cleared.", name),
                        String.format("FAIL: element '%s' is not cleared!", getNameWithLocator()));
                element.clear();
            }

            @Override
            public String doGetTagName() {
                String tagName = element.getTagName();
                LOGGER.debug(String.format("PASS: tag name '%s' for element '%s' is found.", tagName, name));
                return tagName;
            }

            @Override
            public Rectangle doGetRect() {
                Rectangle rectangle = element.getRect();
                LOGGER.debug(String.format("PASS: rectangle '%s' for element '%s' is found.", rectangle, name));
                return rectangle;
            }

            @Override
            public String doGetCssValue(String propertyName) {
                String cssValue = element.getCssValue(propertyName);
                LOGGER.debug(String.format("PASS: css value '%s' for property '%s' for element '%s' is found.", cssValue, propertyName, name));
                return cssValue;
            }

            @Override
            public String doGetDomProperty(String propertyName) {
                String domProperty = element.getDomProperty(propertyName);
                LOGGER.debug(String.format("PASS: value '%s' for dom property '%s' for element '%s' is found.", domProperty, propertyName, name));
                return domProperty;
            }

            @Override
            public <X> X doGetScreenshot(OutputType<X> target) {
                X screenshot = element.getScreenshotAs(target);
                LOGGER.debug(String.format("PASS: took screenshot for element '%s'.", name));
                return screenshot;
            }

            @Override
            public String doGetDomAttribute(String attributeName) {
                String attribute = element.getDomAttribute(attributeName);
                LOGGER.debug(String.format("PASS: value '%s' for dom attribute '%s' for element '%s' is found.", attribute, attributeName, name));
                return attribute;
            }

            @Override
            public String doGetAriaRole() {
                String ariaRole = element.getAriaRole();
                LOGGER.debug(String.format("PASS: aria role '%s' for element '%s' is found.", ariaRole, name));
                return ariaRole;
            }

            @Override
            public String doGetAccessibleName() {
                String accessibleName = element.getAccessibleName();
                LOGGER.debug(String.format("PASS: accessible name '%s' for element '%s' is found.", accessibleName, name));
                return accessibleName;
            }

        }, inputArgs);
    }

    @Internal
    private interface ActionSteps {
        void doSubmit();

        void doClick();

        void doClickByJs();

        void doClickByActions();

        void doDoubleClick();

        void doRightClick();

        void doHover(Integer xOffset, Integer yOffset);

        void doType(String text);

        void doSendKeys(Keys keys);

        void doSendKeys(List<CharSequence> keys);

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

        void doClear();

        String doGetTagName();

        Rectangle doGetRect();

        String doGetCssValue(String propertyName);

        String doGetDomProperty(String propertyName);

        <X> X doGetScreenshot(OutputType<X> target);

        String doGetDomAttribute(String attributeName);

        String doGetAriaRole();

        String doGetAccessibleName();
    }
}
