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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Function;

import com.zebrunner.carina.utils.LogicUtils;
import com.zebrunner.carina.utils.config.StandardConfigurationOption;
import com.zebrunner.carina.webdriver.core.factory.ExtendedPageFactory;
import com.zebrunner.carina.webdriver.decorator.ExtendedFieldDecorator;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.helper.IChromeDevToolsHelper;
import com.zebrunner.carina.webdriver.helper.IClipboardHelper;
import com.zebrunner.carina.webdriver.helper.ICommonsHelper;
import com.zebrunner.carina.webdriver.helper.IExtendedWebElementHelper;
import com.zebrunner.carina.webdriver.helper.IPageActionsHelper;
import com.zebrunner.carina.webdriver.helper.IPageDataHelper;
import com.zebrunner.carina.webdriver.helper.IPageStorageHelper;
import com.zebrunner.carina.webdriver.helper.IWaitHelper;
import com.zebrunner.carina.webdriver.locator.ExtendedElementLocatorFactory;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.RectangleReadOnly;
import com.itextpdf.text.pdf.PdfWriter;
import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.factory.ICustomTypePageFactory;
import com.zebrunner.carina.utils.report.SessionContext;
import com.zebrunner.carina.webdriver.Screenshot;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.decorator.PageOpeningStrategy;
import com.zebrunner.carina.webdriver.screenshot.ExplicitFullSizeScreenshotRule;

/**
 * All page POJO objects should extend this abstract page to get extra logic.
 *
 * @author Alex Khursevich
 */
public abstract class AbstractPage implements IChromeDevToolsHelper, IExtendedWebElementHelper, IClipboardHelper, ICommonsHelper, IPageStorageHelper,
        IPageDataHelper, IPageActionsHelper, IWaitHelper, ICustomTypePageFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    /**
     * @deprecated use {@link #getDefaultWaitTimeout()} instead
     */
    @Deprecated(forRemoval = true, since = "1.2.7")
    public static final long EXPLICIT_TIMEOUT = Configuration.getRequired(WebDriverConfiguration.Parameter.EXPLICIT_TIMEOUT, Long.class);
    /**
     * @deprecated will be hided in the next release. Use {@link #getDriver()} instead
     */
    @Deprecated
    protected final WebDriver driver;
    private PageOpeningStrategy pageOpeningStrategy;
    /**
     * @deprecated will be hided in the next release. Use {@link #getPageURL()}, {@link #setPageURL}, {@link #setPageAbsoluteURL(String)} instead
     */
    @Deprecated
    protected String pageURL;
    /**
     * @deprecated will be hided in the next release. Use {@link #getUiLoadedMarker()}, {@link #setUiLoadedMarker(ExtendedWebElement)} instead
     */
    protected ExtendedWebElement uiLoadedMarker;

    @SuppressWarnings("squid:S5993")
    public AbstractPage(WebDriver driver) {
        this.driver = driver;
        pageURL = Configuration.get(WebDriverConfiguration.Parameter.URL, StandardConfigurationOption.DECRYPT).orElse("");
        pageOpeningStrategy = PageOpeningStrategy.valueOf(Configuration.getRequired(WebDriverConfiguration.Parameter.PAGE_OPENING_STRATEGY));
        ExtendedElementLocatorFactory factory = new ExtendedElementLocatorFactory(driver, driver);
        PageFactory.initElements(new ExtendedFieldDecorator(factory, driver), this);
        ExtendedPageFactory.reinitElementsContext(this);
        uiLoadedMarker = null;
    }

    /**
     * Opens page according to specified in constructor URL.
     */
    public void open() {
        openURL(this.pageURL);
    }

    public boolean isPageOpened() {
        return isPageOpened(getDefaultWaitTimeout());
    }

    public boolean isPageOpened(long timeout) {
        return isPageOpened(Duration.ofSeconds(timeout));
    }

    public boolean isPageOpened(Duration timeout) {
        switch (pageOpeningStrategy) {
        case BY_URL:
            return isPageOpened(this, timeout);
        case BY_ELEMENT:
            if (uiLoadedMarker == null) {
                throw new IllegalStateException("Please specify uiLoadedMarker for the page/screen to validate page opened state");
            }
            return uiLoadedMarker.isElementPresent(timeout);
        case BY_URL_AND_ELEMENT:
            boolean isOpened = isPageOpened(this, timeout);
            if (!isOpened) {
                return false;
            }

            if (uiLoadedMarker != null) {
                isOpened = uiLoadedMarker.isElementPresent(timeout);
            }

            if (!isOpened) {
                LOGGER.warn("Loaded page url is as expected but page loading marker element is not visible: {}", uiLoadedMarker);
            }
            return isOpened;
        default:
            throw new IllegalStateException("Page opening strategy was not applied properly");
        }
    }

    /**
     * Asserts whether page is opened or not. Inside there is a check for expected url matches actual page url.
     * In addition if uiLoadedMarker is specified for the page it will check whether mentioned element presents on page or not.
     */
    public void assertPageOpened() {
        assertPageOpened(getDefaultWaitTimeout());
    }

    public void assertPageOpened(long timeout) {
        assertPageOpened(Duration.ofSeconds(timeout));
    }

    /**
     * Asserts whether page is opened or not. Inside there is a check for expected url matches actual page url.
     * In addition if uiLoadedMarker is specified for the page it will check whether mentioned element presents on page or not.
     *
     * @param timeout Completing of page loading conditions will be verified within specified timeout
     */
    public void assertPageOpened(Duration timeout) {
        switch (pageOpeningStrategy) {
        case BY_URL:
            Assert.assertTrue(isPageOpened(this, timeout), String.format("%s not loaded: url is not as expected", getPageClassName()));
            break;
        case BY_ELEMENT:
            if (uiLoadedMarker == null) {
                throw new IllegalStateException("Please specify uiLoadedMarker for the page/screen to validate page opened state");
            }
            Assert.assertTrue(uiLoadedMarker.isElementPresent(timeout), String.format("%s not loaded: page loading marker element is not visible: %s",
                    getPageClassName(), uiLoadedMarker));
            break;
        case BY_URL_AND_ELEMENT:
            if (!isPageOpened(this, timeout)) {
                Assert.fail(String.format("%s not loaded: url is not as expected", getPageClassName()));
            }

            if (uiLoadedMarker != null) {
                Assert.assertTrue(uiLoadedMarker.isElementPresent(timeout),
                        String.format("%s not loaded: url is correct but page loading marker element is not visible: %s", getPageClassName(),
                                uiLoadedMarker));
            }
            break;
        default:
            throw new IllegalStateException("Page opening strategy was not applied properly");
        }
    }

    public String getPageURL() {
        return this.pageURL;
    }

    @Override
    public final WebDriver getDriver() {
        return this.driver;
    }

    public ExtendedWebElement getUiLoadedMarker() {
        return uiLoadedMarker;
    }

    public void setUiLoadedMarker(ExtendedWebElement uiLoadedMarker) {
        this.uiLoadedMarker = uiLoadedMarker;
    }

    public PageOpeningStrategy getPageOpeningStrategy() {
        return pageOpeningStrategy;
    }

    public void setPageOpeningStrategy(PageOpeningStrategy pageOpeningStrategy) {
        this.pageOpeningStrategy = pageOpeningStrategy;
    }

    /**
     * Save page as pdf<br>
     * If you want to set fileName as test name, use TestNamingService.getTestName()
     */
    public Path savePageAsPdf(boolean scaled, String fileName) {
        try {
            String pdfName = "";
            Path path = Screenshot.capture(getDriver(), getDriver(), new ExplicitFullSizeScreenshotRule(), "")
                    .orElseThrow();
            String fileID = fileName.replaceAll("\\W+", "_") + "-" + System.currentTimeMillis();
            pdfName = fileID + ".pdf";
            Path pdfPath = SessionContext.getArtifactsFolder().resolve(pdfName);

            Image image = Image.getInstance(path.toAbsolutePath().toString());
            Document document;
            if (scaled) {
                document = new Document(PageSize.A4, 10, 10, 10, 10);
                if (image.getHeight() > (document.getPageSize().getHeight() - 20)
                        || image.getScaledWidth() > (document.getPageSize().getWidth() - 20)) {
                    image.scaleToFit(document.getPageSize().getWidth() - 20, document.getPageSize().getHeight() - 20);
                }
            } else {
                document = new Document(new RectangleReadOnly(image.getScaledWidth(), image.getScaledHeight()));
            }
            PdfWriter.getInstance(document, Files.newOutputStream(pdfPath));
            document.open();
            document.add(image);
            document.close();
            return pdfPath;
        } catch (IOException | DocumentException e) {
            throw new RuntimeException(String.format("Cannot save page as pdf. Message: '%s'", e.getMessage()), e);
        }
    }

    /**
     * Save page as pdf<br>
     * If you want to set fileName as test name, use TestNamingService.getTestName()
     */
    public Path savePageAsPdf(String fileName) {
        return savePageAsPdf(true, fileName);
    }

    /**
     * Waits till JS and jQuery (if applicable for the page) are completely processed on the page
     */
    public void waitForJSToLoad() {
        waitForJSToLoad(getDefaultWaitTimeout().toSeconds());
    }

    /**
     * Waits till JS and jQuery (if applicable for the page) are completely processed on the page
     * 
     * @param timeout Completing of JS loading will be verified within specified timeout
     */
    public void waitForJSToLoad(long timeout) {
        // wait for jQuery to load
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        ExpectedCondition<Boolean> jQueryLoad = driver -> {
            try {
                return ((Long) executor.executeScript("return jQuery.active") == 0);
            } catch (Exception e) {
                return true;
            }
        };
        // wait for Javascript to load
        ExpectedCondition<Boolean> jsLoad = driver -> executor.executeScript("return document.readyState").toString().equals("complete");
        String errMsg = "JS was not loaded on page during expected time";
        if ((boolean) executor.executeScript("return window.jQuery != undefined")) {
            Assert.assertTrue(waitUntil(jQueryLoad, timeout) && waitUntil(jsLoad, timeout), errMsg);
        } else {
            Assert.assertTrue(waitUntil(jsLoad, timeout), errMsg);
        }
    }

    public boolean isPageOpened(final AbstractPage page) {
        return isPageOpened(page, getDefaultWaitTimeout());
    }

    public boolean isPageOpened(final AbstractPage page, Duration timeout) {
        boolean result;
        Duration retryInterval = getDefaultWaitInterval(timeout);
        Wait<WebDriver> wait = new WebDriverWait(getDriver(), timeout, retryInterval);
        try {
            wait.until((Function<WebDriver, Object>) dr -> LogicUtils.isURLEqual(page.getPageURL(), dr.getCurrentUrl()));
            result = true;
        } catch (Exception e) {
            result = false;
        }
        if (!result) {
            LOGGER.warn("Actual URL differs from expected one. Expected '{}' but found '{}'",
                    page.getPageURL(), getDriver().getCurrentUrl());
        }
        return result;
    }

    protected void setPageURL(String relURL) {
        String baseURL;
        if (Configuration.get(Configuration.Parameter.ENV).isPresent()) {
            baseURL = Configuration.get("base", StandardConfigurationOption.ENVIRONMENT)
                    .orElseGet(() -> Configuration.getRequired(WebDriverConfiguration.Parameter.URL));
        } else {
            baseURL = Configuration.getRequired(WebDriverConfiguration.Parameter.URL);
        }
        this.pageURL = baseURL + relURL;
    }

    protected void setPageAbsoluteURL(String url) {
        this.pageURL = url;
    }

    private String getPageClassName() {
        return String.join(" ", this.getClass().getSimpleName().split("(?=\\p{Upper})"));
    }

}
