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
package com.zebrunner.carina.webdriver.core.factory.impl;

import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.Point;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.Browser;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.http.ClientConfig;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.exception.InvalidConfigurationException;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.core.capability.AbstractCapabilities;
import com.zebrunner.carina.webdriver.core.capability.impl.desktop.ChromeCapabilities;
import com.zebrunner.carina.webdriver.core.capability.impl.desktop.EdgeCapabilities;
import com.zebrunner.carina.webdriver.core.capability.impl.desktop.FirefoxCapabilities;
import com.zebrunner.carina.webdriver.core.capability.impl.desktop.OperaCapabilities;
import com.zebrunner.carina.webdriver.core.capability.impl.desktop.SafariCapabilities;
import com.zebrunner.carina.webdriver.core.factory.AbstractFactory;
import com.zebrunner.carina.webdriver.listener.EventFiringSeleniumCommandExecutor;

public class DesktopFactory extends AbstractFactory {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static MutableCapabilities staticCapabilities = null;

    @Override
    public ImmutablePair<WebDriver, Capabilities> create(String name, Capabilities capabilities, String seleniumHost) {
        WebDriver driver = null;
        if (seleniumHost == null) {
            seleniumHost = Configuration.getRequired(WebDriverConfiguration.Parameter.SELENIUM_URL);
        }

        if (isCapabilitiesEmpty(capabilities)) {
            capabilities = getCapabilities(name);
        }

        if (staticCapabilities != null) {
            LOGGER.info("Static MutableCapabilities will be merged to basic driver capabilities");
            capabilities.merge(staticCapabilities);
        }

        LOGGER.debug("Capabilities: {}", capabilities);

        try {
            ClientConfig clientConfig = ClientConfig.defaultConfig()
                    .baseUrl(new URL(seleniumHost));
            Optional<Integer> readTimeout = Configuration.get(WebDriverConfiguration.Parameter.READ_TIMEOUT, Integer.class);
            if (readTimeout.isPresent()) {
                clientConfig = clientConfig.readTimeout(Duration.ofSeconds(readTimeout.get()));
            }
            EventFiringSeleniumCommandExecutor ce = new EventFiringSeleniumCommandExecutor(clientConfig);
            driver = new RemoteWebDriver(ce, capabilities);
        } catch (MalformedURLException e) {
            throw new UncheckedIOException("Malformed selenium URL!", e);
        }
        resizeBrowserWindow(driver, capabilities);
        return new ImmutablePair<>(driver, capabilities);
    }

    @SuppressWarnings("deprecation")
    public MutableCapabilities getCapabilities(String name) {
        String browser = WebDriverConfiguration.getBrowser().orElseThrow(
                () -> new InvalidConfigurationException(String.format("Cannot choose type of desktop browser. '%s' capability not specified.",
                        CapabilityType.BROWSER_NAME)));
        AbstractCapabilities<?> capabilities = null;
        if (Browser.FIREFOX.browserName().equalsIgnoreCase(browser)) {
            capabilities = new FirefoxCapabilities();
        } else if (Browser.SAFARI.browserName().equalsIgnoreCase(browser)) {
            capabilities = new SafariCapabilities();
        } else if (Browser.CHROME.browserName().equalsIgnoreCase(browser)) {
            capabilities = new ChromeCapabilities();
        } else if (Browser.OPERA.browserName().equalsIgnoreCase(browser)) {
            capabilities = new OperaCapabilities();
        } else if (Browser.EDGE.browserName().equalsIgnoreCase(browser) ||
                "edge".equalsIgnoreCase(browser)) {
            capabilities = new EdgeCapabilities();
        } else {
            throw new InvalidConfigurationException("Unsupported browser: " + browser);
        }
        return capabilities.getCapability(name);
    }

    public static void addStaticCapability(String name, Object value) {
        if (staticCapabilities == null) {
            staticCapabilities = new MutableCapabilities();
        }
        staticCapabilities.setCapability(name, value);
    }

    /**
     * Sets browser window according to capabilites.resolution value, otherwise
     * maximizes window.
     * 
     * @param driver - instance of desktop @WebDriver
     * @param capabilities - driver capabilities
     */
    private void resizeBrowserWindow(WebDriver driver, Capabilities capabilities) {
        try {
            Wait<WebDriver> wait = new FluentWait<WebDriver>(driver)
                    .pollingEvery(Duration.ofMillis(Configuration.getRequired(WebDriverConfiguration.Parameter.RETRY_INTERVAL, Integer.class)))
                    .withTimeout(Duration.ofSeconds(Configuration.getRequired(WebDriverConfiguration.Parameter.EXPLICIT_TIMEOUT, Integer.class)))
                    .ignoring(WebDriverException.class)
                    .ignoring(NoSuchSessionException.class)
                    .ignoring(TimeoutException.class);
            if (capabilities.getCapability("resolution") != null) {
                String resolution = (String) capabilities.getCapability("resolution");
                int expectedWidth = Integer.parseInt(resolution.split("x")[0]);
                int expectedHeight = Integer.parseInt(resolution.split("x")[1]);
                wait.until((Function<WebDriver, Boolean>) drv -> {
                    drv.manage().window().setPosition(new Point(0, 0));
                    drv.manage().window().setSize(new Dimension(expectedWidth, expectedHeight));
                    Dimension actualSize = drv.manage().window().getSize();
                    if (actualSize.getWidth() == expectedWidth && actualSize.getHeight() == expectedHeight) {
                        LOGGER.debug("Browser window size set to {}x{}", actualSize.getWidth(), actualSize.getHeight());
                    } else {
                        LOGGER.warn("Expected browser window {}x{}, but actual {}x{}",
                                expectedWidth, expectedHeight, actualSize.getWidth(), actualSize.getHeight());
                    }
                    return true;
                });
            } else {
                wait.until((Function<WebDriver, Boolean>) drv -> {
                    drv.manage().window().maximize();
                    LOGGER.debug("Browser window size was maximized!");
                    return true;
                });
            }
        } catch (Exception e) {
            LOGGER.error("Unable to resize browser window", e);
        }
    }
}
