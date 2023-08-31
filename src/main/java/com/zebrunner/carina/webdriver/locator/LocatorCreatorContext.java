package com.zebrunner.carina.webdriver.locator;

import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;

public class LocatorCreatorContext {

    private final WebDriver webDriver;
    private final SearchContext searchContext;
    private final String platform;
    private final String browserName;
    private final String automation;
    private final String driverType;

    public LocatorCreatorContext(WebDriver webDriver, SearchContext searchContext, String platform, String browserName, String automation, String driverType) {
        this.webDriver = webDriver;
        this.searchContext = searchContext;
        this.platform = platform;
        this.browserName = browserName;
        this.automation = automation;
        this.driverType = driverType;
    }

    public WebDriver getWebDriver() {
        return webDriver;
    }

    public SearchContext getSearchContext() {
        return searchContext;
    }

    public String getPlatform() {
        return platform;
    }

    public String getBrowserName() {
        return browserName;
    }

    public String getAutomation() {
        return automation;
    }

    public String getDriverType() {
        return driverType;
    }
}
