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
package com.zebrunner.carina.webdriver.core.capability;

import java.io.UncheckedIOException;
import java.util.HashMap;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.Browser;
import org.openqa.selenium.remote.CapabilityType;
import org.testng.Assert;
import org.testng.annotations.Test;

import io.appium.java_client.remote.MobileCapabilityType;

public class CapabilitiesLoaderTest {

    private final static String customCapabilities = "custom_capabilities.properties";

    /*
     * Test that loadCapabilities() raise exception if no properties file detected on classpath
     */
    @Test(expectedExceptions = {
            UncheckedIOException.class }, expectedExceptionsMessageRegExp = "java.io.FileNotFoundException: Unable to find custom capabilities file 'unexisting_file'.")
    public void loadCapabilitiesFromNonExistingFileTest() {
        new CapabilitiesLoader().loadCapabilities("unexisting_file");
    }

    @Test
    public void getW3CCapabilitiesTest() {
        MutableCapabilities capabilities = new CapabilitiesLoader().getCapabilities(customCapabilities);
        Assert.assertNull(capabilities.getCapability("coreParam"), "Capabilities should not contains parameter from configuration"
                + "that has no 'capabilities.' prefix.");

        Assert.assertEquals(capabilities.getCapability(CapabilityType.PLATFORM_NAME), Platform.ANDROID,
                "Capabilities should contains android platform.");
        Assert.assertEquals(capabilities.getCapability(CapabilityType.BROWSER_NAME), Browser.CHROME.browserName(),
                "Capabilities should contains chrome browser name.");

        Assert.assertEquals(capabilities.getCapability("appium:" + MobileCapabilityType.PLATFORM_VERSION), 11,
                "Capabilities should contains 11 platfrom version as integer.");

        Assert.assertEquals(capabilities.getCapability("zebrunner:enableLog"), true);
        Assert.assertEquals(capabilities.getCapability("zebrunner:name"), "Default");
        Assert.assertEquals(capabilities.getCapability("zebrunner:idleTimeout"), "100");

        Assert.assertNotNull(capabilities.getCapability("zebrunner:options"));
        HashMap<String, Object> options = (HashMap<String, Object>) capabilities.getCapability("zebrunner:options");
        Assert.assertEquals(options.get("idleTimeout"), "200");
        Assert.assertEquals(options.get("waitForIdleTimeout"), 200);
        Assert.assertEquals(options.get("enableVideo"), true);
    }

}
