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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zebrunner.agent.core.config.ConfigurationHolder;
import com.zebrunner.agent.core.registrar.Label;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.agent.core.registrar.Artifact;
import com.zebrunner.carina.commons.artifact.IArtifactManager;
import com.zebrunner.carina.utils.Configuration;
import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.utils.exception.InvalidConfigurationException;
import com.zebrunner.carina.utils.mobile.ArtifactProvider;
import com.zebrunner.carina.webdriver.IDriverPool;
import com.zebrunner.carina.webdriver.core.capability.AbstractCapabilities;
import com.zebrunner.carina.webdriver.core.capability.impl.mobile.EspressoCapabilities;
import com.zebrunner.carina.webdriver.core.capability.impl.mobile.UiAutomator2Capabilities;
import com.zebrunner.carina.webdriver.core.capability.impl.mobile.XCUITestCapabilities;
import com.zebrunner.carina.webdriver.core.factory.AbstractFactory;
import com.zebrunner.carina.webdriver.device.Device;
import com.zebrunner.carina.webdriver.listener.EventFiringAppiumCommandExecutor;

import io.appium.java_client.AppiumClientConfig;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.internal.CapabilityHelpers;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.AutomationName;
import io.appium.java_client.remote.MobileCapabilityType;

/**
 * MobileFactory creates instance {@link WebDriver} for mobile testing.
 * 
 * @author Alex Khursevich (alex@qaprosoft.com)
 */
public class MobileFactory extends AbstractFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Map<String, LazyInitializer<String>> CACHE_MOBILE_APP_LAZY_INITIALIZERS = new ConcurrentHashMap<>();

    /**
     * Get a direct (pre-sign) link to the application.
     * If the passed link was previously accessed to get a direct link,
     * it will be taken from the cache, otherwise it will be generated and cached.
     *
     * @param originalAppLink link of which a direct link will be made
     * @return direct link to the mobile application
     */
    public static String getAppLink(String originalAppLink) {
        try {
            String directLink = CACHE_MOBILE_APP_LAZY_INITIALIZERS.computeIfAbsent(originalAppLink,
                            link -> new LazyInitializer<>() {
                                @Override
                                protected String initialize() throws ConcurrentException {
                                    String generatedLink = ArtifactProvider.getInstance()
                                            .getDirectLink(link);
                                    LOGGER.debug("For the 'app' capability with current value '{}', will be cached link: {}", link, generatedLink);
                                    if (ConfigurationHolder.isReportingEnabled()) {
                                        Artifact.attachReferenceToTestRun("app", generatedLink);
                                    }
                                    return generatedLink;
                                }
                            })
                    .get();
            LOGGER.debug("Original value of capability 'app': '{}' will be replaced by cached link: {}", originalAppLink, directLink);
            return directLink;
        } catch (ConcurrentException e) {
            throw new RuntimeException("Cannot get direct link to the application. Message: " + e.getMessage(), e);
        }
    }

    @Override
    public WebDriver create(String name, MutableCapabilities capabilities, String seleniumHost) {
        if (seleniumHost == null) {
            seleniumHost = Configuration.getSeleniumUrl();
        }
        LOGGER.debug("Selenium URL: {}", seleniumHost);

        WebDriver driver = null;
        // if inside capabilities only singly "udid" capability then generate default one and append udid
        if (isCapabilitiesEmpty(capabilities)) {
            capabilities = getCapabilities(name);
        } else if (capabilities.asMap().size() == 1 && capabilities.getCapability("udid") != null) {
            String udid = capabilities.getCapability("udid").toString();
            capabilities = getCapabilities(name);
            capabilities.setCapability("udid", udid);
            LOGGER.debug("Appended udid to capabilities: {}", capabilities);
        }

        Object mobileAppCapability = capabilities.getCapability(MobileCapabilityType.APP);
        if (mobileAppCapability != null) {
            capabilities.setCapability(MobileCapabilityType.APP, getAppLink(String.valueOf(mobileAppCapability)));
        }

        LOGGER.debug("capabilities: {}", capabilities);

        try {
            String mobilePlatformName = CapabilityHelpers.getCapability(capabilities, CapabilityType.PLATFORM_NAME, String.class);
            AppiumClientConfig config = AppiumClientConfig.defaultConfig()
                    .baseUrl(new URL(seleniumHost))
                    .readTimeout(Duration.ofSeconds(R.CONFIG.getLong("read_timeout")));

            if (SpecialKeywords.ANDROID.equalsIgnoreCase(mobilePlatformName)) {
                driver = new AndroidDriver(config, capabilities);
                // todo do not create TVOSDriver for now
                // }
                // else if (SpecialKeywords.IOS.equalsIgnoreCase(mobilePlatformName) &&
                // SpecialKeywords.TVOS.equalsIgnoreCase(CapabilityHelpers.getCapability(capabilities, "deviceType", String.class))) {
                // driver = new TVOSDriver(ce, capabilities);
            } else if (SpecialKeywords.IOS.equalsIgnoreCase(mobilePlatformName) ||
                    SpecialKeywords.TVOS.equalsIgnoreCase(mobilePlatformName)) {
                // can't create a SafariDriver as it has no advantages over IOSDriver, but needs revision in the future
                // SafariDriver only limits functionality
                driver = new IOSDriver(config, capabilities);
            } else {
                throw new InvalidConfigurationException("Unsupported mobile platform: " + mobilePlatformName);
            }
        } catch (MalformedURLException e) {
            throw new UncheckedIOException("Malformed selenium URL!", e);
        } catch (Exception e) {
            LOGGER.debug("STF is enabled. Debug info will be extracted from the exception.");
            String debugInfo = getDebugInfo(e.getMessage());
            if (!debugInfo.isEmpty()) {
                Label.attachToTest("device", getParamFromDebugInfo(debugInfo, "name"));
            }
            // there is no sense to register device in the pool as driver is not started and we don't have custom exception from MCloud
            throw e;
        }

        try {
            Device device = new Device(((HasCapabilities) driver).getCapabilities());
            Label.attachToTest("device", device.getName());
            IDriverPool.registerDevice(device);
            // will be performed just in case uninstall_related_apps flag marked as true
            device.uninstallRelatedApps();
        } catch (Exception e) {
            // use-case when something wrong happen during initialization and registration device information.
            // the most common problem might be due to the adb connection problem
            
            // make sure to initiate driver quit
            LOGGER.error("Unable to register device!", e);
            //TODO: try to handle use-case if quit in this place can hangs for minutes!
            LOGGER.error("starting driver quit...");
            driver.quit();
            LOGGER.error("finished driver quit...");
            throw e;
        }



        return driver;
    }

    private MutableCapabilities getCapabilities(String name) {
        String platform = R.CONFIG.get("capabilities." + CapabilityType.PLATFORM_NAME);
        String automationName = R.CONFIG.get("capabilities." + MobileCapabilityType.AUTOMATION_NAME);

        AbstractCapabilities<?> capabilities = null;
        if (AutomationName.ESPRESSO.equalsIgnoreCase(automationName)) {
            capabilities = new EspressoCapabilities();
        } else if (SpecialKeywords.ANDROID.equalsIgnoreCase(platform)) {
            capabilities = new UiAutomator2Capabilities();
        } else if (platform.equalsIgnoreCase(SpecialKeywords.IOS)
                || platform.equalsIgnoreCase(SpecialKeywords.TVOS)) {
            capabilities = new XCUITestCapabilities();
        } else {
            throw new InvalidConfigurationException("Unsupported platform: " + platform);
        }
        return capabilities.getCapability(name);
    }

    /**
     * Method to extract debug info in case exception has been thrown during app installation
     * 
     * @param exceptionMsg List&lt;WebElement&gt;
     * @return debug info
     */
    private String getDebugInfo(String exceptionMsg) {
        String debugInfoPattern = "\\[\\[\\[(.*)\\]\\]\\]";

        Pattern p = Pattern.compile(debugInfoPattern);
        Matcher m = p.matcher(exceptionMsg);
        String debugInfo = "";
        if (m.find()) {
            debugInfo = m.group(1);
            LOGGER.debug("Extracted debug info: {}", debugInfo);
        } else {
            LOGGER.debug("Debug info hasn't been found");
        }
        return debugInfo;
    }

    private String getUdidFromDebugInfo(String debugInfo) {
        return getParamFromDebugInfo(debugInfo, "udid");
    }

    /**
     * Method to extract specific parameter from debug info in case STF enabled
     * Debug info example: [[[DEBUG info: /opt/android-sdk-linux/platform-tools/adb -P 5037 -s 4d002c7f5b328095 shell pm install -r
     * /data/local/tmp/appium_cache/642637a49a85a430df0f3c4c1b2dd36022c83df4.apk --udid 4d002c7f5b328095 --name Samsung_Galaxy_Note3]]]
     * Example: --{paramName} {paramValue}
     * 
     * @param debugInfo String
     * @param paramName String
     * @return paramValue
     */
    private String getParamFromDebugInfo(String debugInfo, String paramName) {
        String paramPattern = String.format("-%s ([^\\s]*)", paramName);

        Pattern p = Pattern.compile(paramPattern);
        Matcher m = p.matcher(debugInfo);
        String paramValue = "";
        if (m.find()) {
            paramValue = m.group(1);
            LOGGER.debug("Found parameter: {} -> {}", paramName, paramValue);
        } else {
            LOGGER.debug("Param '{}' hasn't been found in debug info: [{}]", paramName, debugInfo);
        }

        return paramValue;
    }
    
}
