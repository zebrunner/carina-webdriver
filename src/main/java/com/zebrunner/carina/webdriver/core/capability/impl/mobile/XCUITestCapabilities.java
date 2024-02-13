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
package com.zebrunner.carina.webdriver.core.capability.impl.mobile;

import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.core.capability.AbstractCapabilities;

import io.appium.java_client.ios.options.XCUITestOptions;
import org.apache.commons.lang3.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

public class XCUITestCapabilities extends AbstractCapabilities<XCUITestOptions> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public XCUITestOptions getCapability(String testName) {
        XCUITestOptions options = new XCUITestOptions();
        addAppiumProxy(options);
        // this step should be executed before initCapabilities() to be able to override this capabilities by default appium approach.
        setLocaleAndLanguage(options);
        addConfigurationCapabilities(options);
        return options;
    }

    /**
     * Add locale and language capabilities<br>
     * <b>Locale capability could be:</b> <br>
     * [language designator] {@code en} - an unspecified region where the language is used. (<b>Unsupported by this
     * method</b>)<br>
     * [language designator]_[region designator] {@code en_GB} - the language used by and regional preference of the user.<br>
     * [language designator]-[script designator] {@code az-Arab} - an unspecified region where the script is used. (<b>Unsupported by this
     * method</b>)<br>
     * [language designator]-[script designator]_[region designator] {@code zh-Hans_HK} - the script used by and regional preference of the user.<br>
     *
     * <br>
     * <b>Language capability could be:</b><br>
     * en [eng] - English<br>
     * fr [fre] - French<br>
     * de [ger] - German<br>
     *
     * @param options {@link XCUITestOptions}
     */
    private void setLocaleAndLanguage(XCUITestOptions options) {
        Locale locale = WebDriverConfiguration.getLocale();
        if (LocaleUtils.isAvailableLocale(locale)) {
            String country = locale.getCountry();
            String language = locale.getLanguage();
            StringBuilder sb = new StringBuilder().append(language);
            if (!locale.getScript().isEmpty()) {
                sb.append("-")
                        .append(locale.getScript());
            }
            sb.append("_")
                    .append(country);
            options.setLocale(sb.toString());
            options.setLanguage(language);
        } else {
            // locale could be used not only for changing language on the device, so we should not throw exception
            LOGGER.warn("Looks like there are no '{}' locale, so 'en_US' will be used instead for the device.", locale);
            options.setLocale("en_US");
            options.setLanguage("en");
        }
    }
}
