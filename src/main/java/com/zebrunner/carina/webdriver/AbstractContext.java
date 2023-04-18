package com.zebrunner.carina.webdriver;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.webdriver.decorator.ExtendedFieldDecorator;
import com.zebrunner.carina.webdriver.locator.ExtendedElementLocatorFactory;

public abstract class AbstractContext implements IDriverHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    /**
     * @deprecated will be hided
     */
    @Deprecated(forRemoval = true, since = "1.0.3")
    protected WebDriver driver;
    /**
     * @deprecated will be hided
     */
    @Deprecated(forRemoval = true, since = "1.0.3")
    private SearchContext searchContext;

    protected AbstractContext(WebDriver driver, SearchContext searchContext) {
        Objects.requireNonNull(driver);
        Objects.requireNonNull(searchContext);
        this.driver = driver;
        this.searchContext = searchContext;
        ExtendedElementLocatorFactory factory = new ExtendedElementLocatorFactory(driver, searchContext);
        PageFactory.initElements(new ExtendedFieldDecorator(factory, driver), this);
        // todo and investigate and fix
        // ExtendedPageFactory.initElementsContext(this);
    }

    @Override
    public WebDriver getDriver() {
        return driver;
    }

    public void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    protected SearchContext getSearchContext() {
        return searchContext;
    }

    protected void setSearchContext(SearchContext searchContext) {
        this.searchContext = searchContext;
    }
}
