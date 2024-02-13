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

import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.android.options.localization.SupportsLocaleScriptOption;
import org.apache.commons.lang3.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

public class UiAutomator2Capabilities extends AbstractCapabilities<UiAutomator2Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public UiAutomator2Options getCapability(String testName) {
        UiAutomator2Options options = new UiAutomator2Options();
        addAppiumProxy(options);
        // this step should be executed before initCapabilities() to be able to override this capabilities by default appium approach.
        setLocaleAndLanguage(options);
        addConfigurationCapabilities(options);
        return options;
    }

    /**
     * Add locale, language and localeScript capabilities<br>
     * <b>Locale capability could be:</b> <br>
     * US - USA<br>
     * JP - Japan<br>
     * <br>
     * <b>Language capability could be:</b><br>
     * en - English<br>
     * fr - French<br>
     * de - German<br>
     *
     * <b>LocaleScript capability could be :</b> Hans<br>
     *
     * @param options {@link UiAutomator2Options}
     */
    private void setLocaleAndLanguage(UiAutomator2Options options) {
        Locale locale = WebDriverConfiguration.getLocale();
        if (LocaleUtils.isAvailableLocale(locale)) {
            options.setLocale(locale.getCountry());
            options.setLanguage(locale.getLanguage());
            String script = locale.getScript();
            if (!script.isEmpty()) {
                // Example: language_tag = zh-Hans-HK, where {@code Hans} - script
                options.setLocaleScript(script);
            }
        } else {
            // locale could be used not only for changing language on the device, so we should not throw exception
            LOGGER.warn("Looks like there are no '{}' locale, so 'en_US' will be used instead for the device.", locale);
            options.setLocale("US");
            options.setLanguage("en");
        }
    }
}
