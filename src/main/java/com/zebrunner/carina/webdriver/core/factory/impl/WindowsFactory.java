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
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.core.capability.impl.windows.WindowsCapabilities;
import com.zebrunner.carina.webdriver.core.factory.AbstractFactory;

import io.appium.java_client.AppiumClientConfig;
import io.appium.java_client.windows.WindowsDriver;

/**
 * WindowsFactory creates instance {@link WebDriver} for Windows native application testing.
 * 
 * @author Sergei Zagriychuk (sergeizagriychuk@gmail.com)
 */
public class WindowsFactory extends AbstractFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public ImmutablePair<WebDriver, Capabilities> create(String name, Capabilities capabilities, String seleniumHost) {
        if (seleniumHost == null) {
            seleniumHost = Configuration.getRequired(WebDriverConfiguration.Parameter.SELENIUM_URL);
        }
        LOGGER.debug("Selenium URL: {}", seleniumHost);

        if (isCapabilitiesEmpty(capabilities)) {
            capabilities = new WindowsCapabilities().getCapability(name);
        }
        LOGGER.debug("Capabilities: {}", capabilities);

        try {
            AppiumClientConfig config = AppiumClientConfig.defaultConfig()
                    .baseUrl(new URL(seleniumHost));
            Optional<Integer> readTimeout = Configuration.get(WebDriverConfiguration.Parameter.READ_TIMEOUT, Integer.class);
            if (readTimeout.isPresent()) {
                config = config.readTimeout(Duration.ofSeconds(readTimeout.get()));
            }
            return new ImmutablePair<>(new WindowsDriver(config, capabilities), capabilities);
        } catch (MalformedURLException e) {
            throw new UncheckedIOException("Malformed appium URL!", e);
        }
    }
}
