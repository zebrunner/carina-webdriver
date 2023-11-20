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
package com.zebrunner.carina.webdriver.core.capability.impl.desktop;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.report.SessionContext;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.core.capability.AbstractCapabilities;

public class ChromeCapabilities extends AbstractCapabilities<ChromeOptions> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Generate ChromeOptions depends on capabilities defines in configuration file
     * Also adds chrome-specific arguments, prefs and so on
     */
    @Override
    public ChromeOptions getCapability(String testName) {
        ChromeOptions options = new ChromeOptions();
        addProxy(options);
        addConfigurationCapabilities(options);
        addChromeOptions(options);
        options.addArguments("--start-maximized", "--ignore-ssl-errors");
        options.setAcceptInsecureCerts(true);
        return options;
    }

    /**
     * Add chrome-specific arguments, prefs and so on
     * 
     * @param options ChromeOptions to which will be added
     */
    private void addChromeOptions(ChromeOptions options) {
        // add default carina options and arguments
        // disable the "unsupported flag" prompt
        options.addArguments("--test-type");
        // prefs
        HashMap<String, Object> chromePrefs = new HashMap<>();
        AtomicBoolean needsPrefs = new AtomicBoolean(false);

        // update browser language
        Configuration.get(WebDriverConfiguration.Parameter.BROWSER_LANGUAGE).ifPresent(language -> {
            LOGGER.info("Set Chrome language to: {}", language);
            options.addArguments("--lang=" + language);
            chromePrefs.put("intl.accept_languages", language);
            needsPrefs.set(true);
        });

        if(Configuration.getRequired(WebDriverConfiguration.Parameter.HEADLESS, Boolean.class)) {
            options.addArguments("--headless=new");
        }

        if (Configuration.get(WebDriverConfiguration.Parameter.AUTO_DOWNLOAD, Boolean.class).orElse(false)) {
            options.addArguments("--disable-features=DownloadBubble");
            options.addArguments("--disable-features=DownloadBubbleV2");
            chromePrefs.put("download.prompt_for_download", false);
            // don't override auto download dir for Zebrunner Selenium Grid (Selenoid)
            chromePrefs.put("download.default_directory", SessionContext.getArtifactsFolder().toString());
            chromePrefs.put("plugins.always_open_pdf_externally", true);
            needsPrefs.set(true);
        }

        Configuration.get(WebDriverConfiguration.Parameter.CHROME_ARGS).ifPresent(args -> {
            // add all custom chrome args
            for (String arg : args.split(",")) {
                if (arg.isEmpty()) {
                    continue;
                }
                options.addArguments(arg.trim());
            }
        });

        // add all custom chrome experimental options, w3c=false
        Configuration.get(WebDriverConfiguration.Parameter.CHROME_EXPERIMENTAL_OPTS).ifPresent(opts -> {
            needsPrefs.set(true);
            for (String option : opts.split(",")) {
                if (option.isEmpty()) {
                    continue;
                }

                // TODO: think about equal sign inside name or value later
                option = option.trim();
                String name = option.split("=")[0].trim();
                String value = option.split("=")[1].trim();
                if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                    chromePrefs.put(name, Boolean.valueOf(value));
                } else if (isNumber(value)) {
                    chromePrefs.put(name, Long.valueOf(value));
                } else {
                    chromePrefs.put(name, value);
                }
            }
        });

        if (needsPrefs.get()) {
            options.setExperimentalOption("prefs", chromePrefs);
        }

        // add all custom chrome mobileEmulation options, deviceName=Nexus 5
        Map<String, String> mobileEmulation = new HashMap<>();
        Configuration.get(WebDriverConfiguration.Parameter.CHROME_MOBILE_EMULATION_OPTS).ifPresent(opts -> {
            for (String option : opts.split(",")) {
                if (option.isEmpty()) {
                    continue;
                }

                option = option.trim();
                String name = option.split("=")[0].trim();
                String value = option.split("=")[1].trim();
                mobileEmulation.put(name, value);
            }
        });

        if (!mobileEmulation.isEmpty()) {
            options.setExperimentalOption("mobileEmulation", mobileEmulation);
        }
    }
}
