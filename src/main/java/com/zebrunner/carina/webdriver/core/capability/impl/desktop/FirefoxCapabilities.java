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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.net.PortProber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.report.SessionContext;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.core.capability.AbstractCapabilities;

public class FirefoxCapabilities extends AbstractCapabilities<FirefoxOptions> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final List<Integer> firefoxPorts = new ArrayList<>();

    /**
     * Generate FirefoxOptions for Firefox with default Carina FirefoxProfile.
     * 
     * @return Firefox capabilities.
     */
    @Override
    public FirefoxOptions getCapability(String testName) {
        FirefoxOptions options = new FirefoxOptions();
        addProxy(options);
        addConfigurationCapabilities(options);
        addFirefoxOptions(options);
        FirefoxProfile profile = options.getProfile();
        profile.setPreference("media.eme.enabled", true);
        profile.setPreference("media.gmp-manager.updateEnabled", true);
        options.setProfile(profile);
        return options;
    }

    /**
     * Generate FirefoxOptions for Firefox with custom FirefoxProfile.
     *
     * @param testName - String.
     * @param profile - FirefoxProfile.
     * @return FirefoxOptions
     */
    public FirefoxOptions getCapability(String testName, FirefoxProfile profile) {
        FirefoxOptions options = new FirefoxOptions();
        addProxy(options);
        addConfigurationCapabilities(options);
        addFirefoxOptions(options);
        options.setProfile(profile);
        return options;
    }

    private void addFirefoxOptions(FirefoxOptions options) {
        FirefoxProfile profile = getDefaultFirefoxProfile();
        options.setProfile(profile);
        // add custom firefox args
        Configuration.get(WebDriverConfiguration.Parameter.FIREFOX_ARGS)
                .ifPresent(args -> Arrays.stream(args.split(","))
                        .filter(arg -> !arg.isEmpty())
                        .map(String::trim)
                        .forEach(options::addArguments));

        if(Configuration.getRequired(WebDriverConfiguration.Parameter.HEADLESS, Boolean.class)) {
            options.addArguments("--headless=new");
        }

        // add all custom firefox preferences
        Configuration.get(WebDriverConfiguration.Parameter.FIREFOX_PREFERENCES).ifPresent(preferences -> {
            Arrays.stream(preferences.split(","))
                    .filter(p -> !p.isEmpty())
                    // TODO: think about equal sign inside name or value later
                    .map(String::trim)
                    .forEach(preference -> {
                        String name = preference.split("=")[0].trim();
                        String value = preference.split("=")[1].trim();
                        // TODO: test approach with numbers
                        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                            options.addPreference(name, Boolean.valueOf(value));
                        } else {
                            options.addPreference(name, value);
                        }
                    });
        });
    }

    /**
     * Generate default default Carina FirefoxProfile.
     *
     * @return Firefox profile.
     */
    // keep it public to be able to get default and override on client layerI
    public FirefoxProfile getDefaultFirefoxProfile() {
        FirefoxProfile profile = new FirefoxProfile();

        // update browser language
        Configuration.get(WebDriverConfiguration.Parameter.BROWSER_LANGUAGE)
                .ifPresent(language -> {
                    LOGGER.info("Set Firefox language to: {}, ", language);
                    profile.setPreference("intl.accept_languages", language);
                });

        boolean generated = false;
        int newPort = 7055;
        int i = 100;
        while (!generated && (--i > 0)) {
            newPort = PortProber.findFreePort();
            generated = firefoxPorts.add(newPort);
        }
        if (!generated) {
            newPort = 7055;
        }
        if (firefoxPorts.size() > 20) {
            firefoxPorts.remove(0);
        }

        profile.setPreference("webdriver_firefox_port", newPort);
        LOGGER.debug("FireFox profile will use '{}' port number.", newPort);

        profile.setPreference("dom.max_chrome_script_run_time", 0);
        profile.setPreference("dom.max_script_run_time", 0);

        Configuration.get(WebDriverConfiguration.Parameter.AUTO_DOWNLOAD, Boolean.class).ifPresent(isAutoDownload -> {
            if (!isAutoDownload) {
                return;
            }
            Optional<String> autoDownloadApps = Configuration.get(WebDriverConfiguration.Parameter.AUTO_DOWNLOAD_APPS);
            if (autoDownloadApps.isEmpty()) {
                LOGGER.warn(
                        "If you want to enable auto-download for FF please specify '{}' param",
                        WebDriverConfiguration.Parameter.AUTO_DOWNLOAD_APPS.getKey());
                return;
            }

            profile.setPreference("browser.download.folderList", 2);
            // don't override auto download dir for Zebrunner Selenium Grid (Selenoid)
            profile.setPreference("browser.download.dir", SessionContext.getArtifactsFolder().toString());

            profile.setPreference("browser.helperApps.neverAsk.saveToDisk", autoDownloadApps.get());
            profile.setPreference("browser.download.manager.showWhenStarting", false);
            profile.setPreference("browser.download.saveLinkAsFilenameTimeout", 1);
            profile.setPreference("pdfjs.disabled", true);
            profile.setPreference("plugin.scan.plid.all", false);
            profile.setPreference("plugin.scan.Acrobat", "99.0");

        });
        profile.setAcceptUntrustedCertificates(true);
        profile.setAssumeUntrustedCertificateIssuer(true);

        // TODO: implement support of custom args if any
        return profile;
    }
}
