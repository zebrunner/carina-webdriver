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
package com.zebrunner.carina.webdriver;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import com.zebrunner.carina.webdriver.helper.IClipboardHelper;
import com.zebrunner.carina.webdriver.helper.ICommonsHelper;
import com.zebrunner.carina.webdriver.helper.IExtendedWebElementHelper;
import com.zebrunner.carina.webdriver.helper.IPageActionsHelper;
import com.zebrunner.carina.webdriver.helper.IPageDataHelper;
import com.zebrunner.carina.webdriver.helper.IPageStorageHelper;
import com.zebrunner.carina.webdriver.helper.IWaitHelper;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.config.StandardConfigurationOption;
import com.zebrunner.carina.utils.encryptor.EncryptorUtils;
import com.zebrunner.carina.utils.messager.Messager;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration.Parameter;
import com.zebrunner.carina.webdriver.helper.IChromeDevToolsHelper;
import com.zebrunner.carina.webdriver.listener.DriverListener;

/**
 * DriverHelper - WebDriver wrapper for logging and reporting features. Also it
 * contains some complex operations with UI.
 *
 * @author Alex Khursevich
 */
public class DriverHelper implements IChromeDevToolsHelper, IExtendedWebElementHelper, IClipboardHelper, ICommonsHelper, IPageStorageHelper,
        IPageDataHelper, IPageActionsHelper, IWaitHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final long EXPLICIT_TIMEOUT = Configuration.getRequired(Parameter.EXPLICIT_TIMEOUT, Long.class);
    public static final long SHORT_TIMEOUT = Configuration.getRequired(Parameter.EXPLICIT_TIMEOUT, Long.class) / 3;
    public static final long RETRY_TIME = Configuration.getRequired(Parameter.RETRY_INTERVAL, Long.class);
    protected WebDriver driver;
    @Deprecated(forRemoval = true, since = "1.2.7")
    protected String pageURL = Configuration.get(Parameter.URL).orElse("");

    public DriverHelper() {
    }

    public DriverHelper(WebDriver driver) {
        this();
        Objects.requireNonNull(driver, "WebDriver not initialized, check log files for details!");
        this.driver = driver;
    }

    /**
     * Opens page according to specified in constructor URL.
     */
    @Deprecated(forRemoval = true, since = "1.2.7")
    public void open() {
        openURL(this.pageURL);
    }

    /**
     * Open URL.
     *
     * @param url to open.
     */
    @Deprecated(forRemoval = true, since = "1.2.7")
    public void openURL(String url) {
        openURL(url, Configuration.getRequired(WebDriverConfiguration.Parameter.EXPLICIT_TIMEOUT, Integer.class));
    }

    /**
     * Open URL.
     *
     * @param url to open.
     * @param timeout long
     */
    @Deprecated(forRemoval = true, since = "1.2.7")
    public void openURL(String url, long timeout) {
        String decryptedURL = EncryptorUtils.decrypt(url);
        if (!(decryptedURL.startsWith("http:") || decryptedURL.startsWith("https:"))) {
            decryptedURL = Configuration.getRequired(WebDriverConfiguration.Parameter.URL, StandardConfigurationOption.DECRYPT) + decryptedURL;
        }
        this.pageURL = decryptedURL;
        WebDriver drv = getDriver();

        setPageLoadTimeout(drv, timeout);
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

    @Deprecated(forRemoval = true, since = "1.2.7")
    protected void setPageURL(String relURL) {
        String baseURL;
        if (Configuration.get(Configuration.Parameter.ENV).isPresent()) {
            baseURL = Configuration.get("base", StandardConfigurationOption.ENVIRONMENT)
                    .orElseGet(() -> Configuration.getRequired(Parameter.URL));
        } else {
            baseURL = Configuration.getRequired(Parameter.URL);
        }
        this.pageURL = baseURL + relURL;
    }

    @Deprecated(forRemoval = true, since = "1.2.7")
    protected void setPageAbsoluteURL(String url) {
        this.pageURL = url;
    }

    @Deprecated(forRemoval = true, since = "1.2.7")
    public String getPageURL() {
        return this.pageURL;
    }

    protected void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    public WebDriver getDriver() {
        if (driver == null) {
            long currentThreadId = Thread.currentThread().getId();
            LOGGER.error("There is no any initialized driver for thread: {}", currentThreadId);
            throw new RuntimeException("Driver isn't initialized.");
        }
        return driver;
    }
}
