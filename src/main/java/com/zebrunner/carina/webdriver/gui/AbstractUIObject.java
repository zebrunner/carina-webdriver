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

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.testng.Assert;

import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.messager.Messager;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.core.factory.ExtendedPageFactory;
import com.zebrunner.carina.webdriver.decorator.ElementLoadingStrategy;
import com.zebrunner.carina.webdriver.decorator.ExtendedFieldDecorator;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.locator.ExtendedElementLocatorFactory;

public abstract class AbstractUIObject extends ExtendedWebElement {

    /**
     * @deprecated use {@link #getRootExtendedElement()} instead
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    protected WebElement rootElement;
    /**
     * @deprecated use {@link #getRootExtendedElement()} instead
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    protected By rootBy;

    protected ExtendedWebElement uiLoadedMarker;
    private ExtendedWebElement rootExtendedElement;

    /**
     * Initializes UI object using {@link PageFactory}. Whole browser window is used as search context
     *
     * @param driver WebDriver
     */
    protected AbstractUIObject(WebDriver driver) {
        this(driver, driver);
    }

    /**
     * Initializes UI object using {@link PageFactory}. Browser area for internal elements initialization is bordered by
     * SearchContext instance.
     * If {@link WebDriver} object is used as search context then whole browser window will be used for initialization
     * of {@link ExtendedWebElement} fields inside.
     * <p>
     * Note: implement this constructor if you want your {@link AbstractUIObject} instances marked with {@link FindBy}
     * to be auto-initialized on {@link AbstractPage} inheritors
     *
     * @param driver WebDriver instance to initialize UI Object fields using PageFactory
     * @param searchContext Window area that will be used for locating of internal elements
     */
    protected AbstractUIObject(WebDriver driver, SearchContext searchContext) {
        super(driver, searchContext);
        ExtendedElementLocatorFactory factory = new ExtendedElementLocatorFactory(driver, searchContext);
        PageFactory.initElements(new ExtendedFieldDecorator(factory, driver), this);
        ExtendedPageFactory.initElementsContext(this);
    }

    /**
     * Verifies if root {@link WebElement} presents on page.
     * <p>
     * If {@link AbstractUIObject} field on {@link AbstractPage} is marked with {@link FindBy} annotation then this
     * locator will be used to instantiate rootElement
     *
     * @param timeout max timeout for waiting until rootElement appear
     * @return true if rootElement is enabled and visible on browser's screen, false - otherwise
     */
    public boolean isUIObjectPresent(long timeout) {
        return this.rootExtendedElement.isPresent(timeout);
    }

    public boolean isUIObjectPresent() {
        return isUIObjectPresent(Configuration.getRequired(WebDriverConfiguration.Parameter.EXPLICIT_TIMEOUT, Integer.class));
    }

    public ExtendedWebElement getUiLoadedMarker() {
        return uiLoadedMarker;
    }

    public void setUiLoadedMarker(ExtendedWebElement uiLoadedMarker) {
        this.uiLoadedMarker = uiLoadedMarker;
    }

    /**
     * Get the {@link ExtendedWebElement} of the current component
     * 
     * @return see {@link ExtendedWebElement}
     */
    public ExtendedWebElement getRootExtendedElement() {
        return this.rootExtendedElement;
    }

    public void setRootExtendedElement(ExtendedWebElement element) {
        this.rootExtendedElement = element;
    }

    /**
     * @deprecated to interact with the current component
     *             (getting information about the current element) use {@link #rootExtendedElement}
     */
    @Deprecated(since = "8.0.4", forRemoval = true)
    public WebElement getRootElement() {
        return rootElement;
    }

    /**
     * @deprecated to interact with the current component
     *             (getting information about the current element) use {@link #rootExtendedElement}
     */
    @Deprecated(since = "8.0.4", forRemoval = true)
    public void setRootElement(WebElement element) {
        this.rootElement = element;
    }

    /**
     * @deprecated to interact with the current component
     *             (getting information about the current element) use {@link #rootExtendedElement}
     */
    @Deprecated(since = "8.0.4", forRemoval = true)
    public By getRootBy() {
        return rootBy;
    }

    /**
     * @deprecated to interact with the current component
     *             (getting information about the current element) use {@link #rootExtendedElement}
     */
    @Deprecated(since = "8.0.4", forRemoval = true)
    public void setRootBy(By rootBy) {
        this.rootBy = rootBy;
    }

    /**
     * Checks presence of UIObject root element on the page and throws Assertion error in case if it's missing
     */
    public void assertUIObjectPresent() {
        assertUIObjectPresent(getWaitTimeout().toSeconds());
    }

    /**
     * Checks presence of UIObject root element on the page and throws Assertion error in case if it's missing
     * 
     * @param timeout long
     */
    public void assertUIObjectPresent(long timeout) {
        if (!isUIObjectPresent(timeout)) {
            Assert.fail(Messager.UI_OBJECT_NOT_PRESENT.getMessage(this.rootExtendedElement.getNameWithLocator()));
        }
    }

    /**
     * Checks missing of UIObject root element on the page and throws Assertion error in case if it presents
     */
    public void assertUIObjectNotPresent() {
        assertUIObjectNotPresent(getWaitTimeout().toSeconds());
    }

    /**
     * Checks missing of UIObject root element on the page and throws Assertion error in case if it presents
     * 
     * @param timeout long
     */
    public void assertUIObjectNotPresent(long timeout) {
        if (isUIObjectPresent(timeout)) {
            Assert.fail(Messager.UI_OBJECT_PRESENT.getMessage(this.rootExtendedElement.getNameWithLocator()));
        }
    }
}
