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

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.Configuration;
import com.zebrunner.carina.utils.Configuration.Parameter;

/**
 * DriverHelper - WebDriver wrapper for logging and reporting features. Also it
 * contains some complex operations with UI.
 * 
 * @author Alex Khursevich
 */
public class DriverHelper implements IDriverHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Deprecated(forRemoval = true, since = "1.0.1")
    protected static final long EXPLICIT_TIMEOUT = Configuration.getLong(Parameter.EXPLICIT_TIMEOUT);
    /**
     * @deprecated will be hided
     */
    @Deprecated
    protected WebDriver driver;

    /**
     * @deprecated unused field
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    protected long timer;

    public DriverHelper() {
    }

    public DriverHelper(WebDriver driver) {
        this();
        Objects.requireNonNull(driver, "WebDriver not initialized, check log files for details!");
        this.driver = driver;
    }

    // --------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------


    protected void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    public WebDriver getDriver() {
        if (this.driver == null) {
            long currentThreadId = Thread.currentThread().getId();
            LOGGER.error("There is no any initialized driver for thread: {}", currentThreadId);
            throw new RuntimeException("Driver isn't initialized.");
        }
        return this.driver;
    }

}
