package com.zebrunner.carina.webdriver;

import java.util.Objects;

import javax.annotation.Nullable;

import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

import com.zebrunner.carina.utils.Configuration;
import com.zebrunner.carina.utils.annotations.Internal;
import com.zebrunner.carina.webdriver.core.factory.ExtendedPageFactory;
import com.zebrunner.carina.webdriver.decorator.ExtendedFieldDecorator;
import com.zebrunner.carina.webdriver.locator.ExtendedElementLocatorFactory;

/**
 * Abstraction over an area on the page.
 */
@Internal
public abstract class AbstractContext implements IDriverHelper {

    public static final long SHORT_TIMEOUT = Configuration.getLong(Configuration.Parameter.EXPLICIT_TIMEOUT) / 3;
    /**
     * @deprecated will be hided. Use {@link #getDriver()} or {@link #setDriver(WebDriver)} instead
     */
    @Deprecated(forRemoval = true, since = "1.0.3")
    protected WebDriver driver;

    protected AbstractContext(WebDriver driver, @Nullable SearchContext searchContext) {
        Objects.requireNonNull(driver);
        this.driver = driver;
        ExtendedElementLocatorFactory factory = new ExtendedElementLocatorFactory(driver,
                searchContext != null ? searchContext : (SearchContext) this);
        PageFactory.initElements(new ExtendedFieldDecorator(factory, driver), this);
        ExtendedPageFactory.initElementsContext(this);
    }

    @Override
    public final WebDriver getDriver() {
        return driver;
    }

    public final void setDriver(WebDriver driver) {
        this.driver = driver;
    }

}
