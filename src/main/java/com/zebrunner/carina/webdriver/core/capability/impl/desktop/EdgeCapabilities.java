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

import org.openqa.selenium.edge.EdgeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.report.SessionContext;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.core.capability.AbstractCapabilities;

public class EdgeCapabilities extends AbstractCapabilities<EdgeOptions> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public EdgeOptions getCapability(String testName) {
        EdgeOptions options = new EdgeOptions();
        addProxy(options);
        addConfigurationCapabilities(options);
        addEdgeOptions(options);
        options.addArguments("--start-maximized", "--ignore-ssl-errors");
        options.setAcceptInsecureCerts(true);
        return options;
    }

    private void addEdgeOptions(EdgeOptions options) {
        Map<String, Object> prefs = new HashMap<>();
        AtomicBoolean needsPrefs = new AtomicBoolean(false);
        // disable the "unsupported flag" prompt
        options.addArguments("--test-type");
        // update browser language
        Configuration.get(WebDriverConfiguration.Parameter.BROWSER_LANGUAGE).ifPresent(language -> {
            LOGGER.info("Set Edge language to: {}", language);
            options.addArguments("--lang=" + language);
            prefs.put("intl.accept_languages", language);
            needsPrefs.set(true);
        });

        if(Configuration.getRequired(WebDriverConfiguration.Parameter.HEADLESS, Boolean.class)) {
            options.addArguments("--headless=new");
        }

        if (Configuration.get(WebDriverConfiguration.Parameter.AUTO_DOWNLOAD, Boolean.class).orElse(false)) {
            prefs.put("download.prompt_for_download", false);
            prefs.put("download.default_directory", SessionContext.getArtifactsFolder().toString());
            needsPrefs.set(true);
        }

        if (needsPrefs.get()) {
            options.setExperimentalOption("prefs", prefs);
        }
        options.setCapability("ms:edgeChrominum", true);
    }
}
