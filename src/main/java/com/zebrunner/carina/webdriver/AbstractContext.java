package com.zebrunner.carina.webdriver;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.webdriver.core.factory.ExtendedPageFactory;
import com.zebrunner.carina.webdriver.decorator.ExtendedFieldDecorator;
import com.zebrunner.carina.webdriver.locator.ExtendedElementLocatorFactory;

public abstract class AbstractContext implements IDriverHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * @deprecated will be hided. Use {@link #getDriver()} or {@link #setDriver(WebDriver)} instead
     */
    @Deprecated(forRemoval = true, since = "1.0.3")
    protected WebDriver driver;

    /**
     * @deprecated will be hided. Use {@link #getSearchContext()} or {@link #setSearchContext(SearchContext)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.0.3")
    protected SearchContext searchContext;

    protected AbstractContext(WebDriver driver, SearchContext searchContext) {
        Objects.requireNonNull(driver);
        Objects.requireNonNull(searchContext);
        this.driver = driver;
        this.searchContext = searchContext;
        ExtendedElementLocatorFactory factory = new ExtendedElementLocatorFactory(driver, searchContext);
        PageFactory.initElements(new ExtendedFieldDecorator(factory, driver), this);
        // todo check is @Context annotation works correctly
        ExtendedPageFactory.initElementsContext(this);
    }

    @Override
    public final WebDriver getDriver() {
        return driver;
    }

    public final void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    public final SearchContext getSearchContext() {
        return searchContext;
    }

    public final void setSearchContext(SearchContext searchContext) {
        this.searchContext = searchContext;
    }
}
