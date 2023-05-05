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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
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
import com.zebrunner.carina.utils.Configuration;
import com.zebrunner.carina.utils.Configuration.Parameter;
import com.zebrunner.carina.utils.CryptoUtils;
import com.zebrunner.carina.utils.LogicUtils;
import com.zebrunner.carina.utils.factory.ICustomTypePageFactory;
import com.zebrunner.carina.utils.messager.Messager;
import com.zebrunner.carina.utils.report.ReportContext;
import com.zebrunner.carina.webdriver.AbstractContext;
import com.zebrunner.carina.webdriver.Screenshot;
import com.zebrunner.carina.webdriver.decorator.PageOpeningStrategy;
import com.zebrunner.carina.webdriver.listener.DriverListener;
import com.zebrunner.carina.webdriver.screenshot.ExplicitFullSizeScreenshotRule;

/**
 * All page POJO objects should extend this abstract page to get extra logic.
 *
 * @author Alex Khursevich
 */
public abstract class AbstractPage extends AbstractContext implements ICustomTypePageFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    /**
     * @deprecated will be hided. Use {@link #getPageOpeningStrategy()}, {@link #setPageOpeningStrategy(PageOpeningStrategy)} instead
     */
    @Deprecated(forRemoval = true, since = "1.0.3")
    protected PageOpeningStrategy pageOpeningStrategy;
    /**
     * @deprecated will be hided. Use {@link #getPageURL()} / {@link #setPageURL(String)} / {@link #setPageAbsoluteURL(String)} instead
     */
    @Deprecated(forRemoval = true, since = "1.0.3")
    protected String pageURL;
    /**
     * @deprecated will be hided. Use {@link #getUiLoadedMarker()} / {@link #setUiLoadedMarker(AbstractUIObject)} instead
     */
    @Deprecated(forRemoval = true, since = "1.0.3")
    protected AbstractUIObject uiLoadedMarker;

    /**
     * Initializes page using {@link PageFactory}. Browser area for internal elements initialization is bordered by
     * current page.
     * <p>
     * Note: implement this constructor if you want your {@link AbstractUIObject} instances marked with {@link FindBy}
     * to be auto-initialized on {@link AbstractPage} inheritors
     *
     * @param driver {@link WebDriver}
     */
    protected AbstractPage(WebDriver driver) {
        super(driver, driver);
        pageOpeningStrategy = PageOpeningStrategy.valueOf(Configuration.get(Parameter.PAGE_OPENING_STRATEGY));
        pageURL = getUrl();
        // we should set as null explicitly because when we init page this field becomes element!
        uiLoadedMarker = null;
    }

    public final Optional<AbstractUIObject> getUiLoadedMarker() {
        return Optional.ofNullable(uiLoadedMarker);
    }

    /**
     * Set the marker that determines whether the page is loaded or not.<br>
     * Only will be used if {@link PageOpeningStrategy} is {@link PageOpeningStrategy#BY_ELEMENT} or {@link PageOpeningStrategy#BY_URL_AND_ELEMENT}
     * 
     * @param uiLoadedMarker see {@link AbstractUIObject}
     */
    public final void setUiLoadedMarker(@Nullable AbstractUIObject uiLoadedMarker) {
        this.uiLoadedMarker = uiLoadedMarker;
    }

    public final PageOpeningStrategy getPageOpeningStrategy() {
        return pageOpeningStrategy;
    }

    /**
     * Set page opening strategy
     * 
     * @param pageOpeningStrategy see {@link PageOpeningStrategy}
     */
    public final void setPageOpeningStrategy(@Nonnull PageOpeningStrategy pageOpeningStrategy) {
        Objects.requireNonNull(pageOpeningStrategy);
        this.pageOpeningStrategy = pageOpeningStrategy;
    }


    public boolean isPageOpened() {
        return isPageOpened(EXPLICIT_TIMEOUT);
    }

    public boolean isPageOpened(long timeout) {
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
                LOGGER.warn("Loaded page url is as expected but page loading marker element is not visible: {}", uiLoadedMarker.getNameWithLocator());
            }
            return isOpened;
        default:
            throw new RuntimeException("Page opening strategy was not applied properly");
        }
    }

    /**
     * @deprecated will be hided
     */
    @Deprecated(forRemoval = true, since = "1.0.3")
    public boolean isPageOpened(final AbstractPage page) {
        return isPageOpened(page, EXPLICIT_TIMEOUT);
    }

    /**
     * @deprecated will be hided
     */
    @Deprecated(forRemoval = true, since = "1.0.3")
    public boolean isPageOpened(final AbstractPage page, long timeout) {
        boolean result;
        long retryInterval = getRetryInterval(timeout);
        Wait<WebDriver> wait = new WebDriverWait(getDriver(), Duration.ofSeconds(timeout), Duration.ofMillis(retryInterval));
        try {
            wait.until((Function<WebDriver, Object>) dr -> LogicUtils.isURLEqual(page.getPageURL(), dr.getCurrentUrl()));
            result = true;
        } catch (Exception e) {
            result = false;
        }
        if (!result) {
            I_DRIVER_HELPER_LOGGER.warn("Actual URL differs from expected one. Expected '{}' but found '{}'",
                    page.getPageURL(), getDriver().getCurrentUrl());
        }
        return result;
    }

    /**
     * Asserts whether page is opened or not. Inside there is a check for expected url matches actual page url.
     * In addition if uiLoadedMarker is specified for the page it will check whether mentioned element presents on page or not.
     */
    public void assertPageOpened() {
        assertPageOpened(EXPLICIT_TIMEOUT);
    }

    /**
     * Asserts whether page is opened or not. Inside there is a check for expected url matches actual page url.
     * In addition if uiLoadedMarker is specified for the page it will check whether mentioned element presents on page or not.
     * 
     * @param timeout Completing of page loading conditions will be verified within specified timeout
     */
    public void assertPageOpened(long timeout) {
        switch (pageOpeningStrategy) {
        case BY_URL:
            Assert.assertTrue(isPageOpened(this, timeout), String.format("%s not loaded: url is not as expected", getPageClassName()));
            break;
        case BY_ELEMENT:
            if (uiLoadedMarker == null) {
                throw new IllegalStateException("Please specify uiLoadedMarker for the page/screen to validate page opened state");
            }
            Assert.assertTrue(uiLoadedMarker.isElementPresent(timeout), String.format("%s not loaded: page loading marker element is not visible: %s",
                    getPageClassName(), uiLoadedMarker.getNameWithLocator()));
            break;
        case BY_URL_AND_ELEMENT:
            if (!isPageOpened(this, timeout)) {
                Assert.fail(String.format("%s not loaded: url is not as expected", getPageClassName()));
            }

            if (uiLoadedMarker != null) {
                Assert.assertTrue(uiLoadedMarker.isElementPresent(timeout),
                        String.format("%s not loaded: url is correct but page loading marker element is not visible: %s",
                                getPageClassName(),
                                uiLoadedMarker.getNameWithLocator()));
            }
            break;
        default:
            throw new RuntimeException("Page opening strategy was not applied properly");
        }
    }

	private String getPageClassName() {
		return String.join(" ", this.getClass().getSimpleName().split("(?=\\p{Upper})"));
	}

    /**
     * Save page as pdf<br>
     * If you want to set fileName as test name, use TestNamingService.getTestName()
     */
    public String savePageAsPdf(boolean scaled, String fileName) throws IOException, DocumentException {
        String pdfName = "";
        File artifactsFolder = ReportContext.getArtifactsFolder();


        ExplicitFullSizeScreenshotRule screenshotRule = new ExplicitFullSizeScreenshotRule();
        Optional<String> screenshot = Screenshot.capture(getDriver(), getDriver(), screenshotRule, "");
        if (screenshot.isEmpty()) {
            return pdfName;
        }

        String fileID = fileName.replaceAll("\\W+", "_") + "-" + System.currentTimeMillis();
        pdfName = fileID + ".pdf";
        String fullPdfPath = artifactsFolder.getAbsolutePath() + "/" + pdfName;

        Image image = Image.getInstance(screenshotRule.getSaveFolder().toFile().getAbsolutePath() + "/" + screenshot.get());
        Document document = null;
        if (scaled) {
            document = new Document(PageSize.A4, 10, 10, 10, 10);
            if (image.getHeight() > (document.getPageSize().getHeight() - 20)
                    || image.getScaledWidth() > (document.getPageSize().getWidth() - 20)) {
                image.scaleToFit(document.getPageSize().getWidth() - 20, document.getPageSize().getHeight() - 20);
            }
        } else {
            document = new Document(new RectangleReadOnly(image.getScaledWidth(), image.getScaledHeight()));
        }
        PdfWriter.getInstance(document, new FileOutputStream(fullPdfPath));
        document.open();
        document.add(image);
        document.close();
        return fullPdfPath;
    }

    /**
     * Save page as pdf<br>
     * If you want to set fileName as test name, use TestNamingService.getTestName()
     */
    public String savePageAsPdf(String fileName) throws IOException, DocumentException {
        return savePageAsPdf(true, fileName);
    }

    /**
     * Waits till JS and jQuery (if applicable for the page) are completely processed on the page
     */
    public void waitForJSToLoad() {
        waitForJSToLoad(EXPLICIT_TIMEOUT);
    }

    /**
     * Waits till JS and jQuery (if applicable for the page) are completely processed on the page
     * 
     * @param timeout Completing of JS loading will be verified within specified timeout
     */
    public void waitForJSToLoad(long timeout) {
        // wait for jQuery to load
        JavascriptExecutor executor = (JavascriptExecutor) getDriver();
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

    private String getUrl() {
        String url = "";
        if (Configuration.isNull(Parameter.ENV) || Configuration.getEnvArg(Parameter.URL.getKey()).isEmpty()) {
            url = Configuration.get(Parameter.URL);
        } else {
            url = Configuration.getEnvArg(Parameter.URL.getKey());
        }
        return url;
    }

    /**
     * Opens page according to specified in constructor URL.
     */
    public void open() {
        openURL(this.pageURL);
    }

    /**
     * Open URL.
     *
     * @param url to open.
     */
    public void openURL(String url) {
        openURL(url, Configuration.getInt(Configuration.Parameter.EXPLICIT_TIMEOUT));
    }


    /**
     * Open URL.
     *
     * @param url to open.
     * @param timeout long
     */
    public void openURL(String url, long timeout) {
        openURL(url, Duration.ofSeconds(timeout));
    }

    /**
     * Open URL.
     *
     * @param url to open.
     * @param timeout timeout, see {@link Duration}
     */
    public void openURL(String url, Duration timeout) {
        final String decryptedURL = getEnvArgURL(CryptoUtils.INSTANCE.decryptIfEncrypted(url));
        this.pageURL = decryptedURL;
        WebDriver drv = getDriver();

        setPageLoadTimeout(drv, timeout.toSeconds());
        DriverListener.setMessages(Messager.OPENED_URL.getMessage(url), Messager.NOT_OPENED_URL.getMessage(url));

        // [VD] there is no sense to use fluent wait here as selenium just don't return something until page is ready!
        // explicitly limit time for the openURL operation
        try {
            Messager.OPENING_URL.info(url);
            drv.get(decryptedURL);
        } catch (UnhandledAlertException e) {
            drv.switchTo().alert().accept();
        } catch (TimeoutException e) {
            trigger("window.stop();"); // try to cancel page loading
            Assert.fail("Unable to open url during " + timeout + "sec!");
        } catch (Exception e) {
            Assert.fail("Undefined error on open url detected: " + e.getMessage(), e);
        } finally {
            // restore default pageLoadTimeout driver timeout
            setPageLoadTimeout(drv, getPageLoadTimeout());
            LOGGER.debug("finished driver.get call.");
        }
    }

    protected final void setPageURL(String relURL) {
        String baseURL;
        if (!Configuration.get(Parameter.ENV).isEmpty()) {
            baseURL = Configuration.getEnvArg("base");
        } else {
            baseURL = Configuration.get(Parameter.URL);
        }
        this.pageURL = baseURL + relURL;
    }

    protected final void setPageAbsoluteURL(String url) {
        this.pageURL = url;
    }

    public final String getPageURL() {
        return this.pageURL;
    }
}
