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
package com.zebrunner.carina.webdriver.core.factory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.WebDriverListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.agent.core.webdriver.RemoteWebDriverFactory;
import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.core.capability.CapabilityUtils;
import com.zebrunner.carina.webdriver.core.capability.DriverType;
import com.zebrunner.carina.webdriver.core.factory.impl.DesktopFactory;
import com.zebrunner.carina.webdriver.core.factory.impl.MacFactory;
import com.zebrunner.carina.webdriver.core.factory.impl.MobileFactory;
import com.zebrunner.carina.webdriver.core.factory.impl.WindowsFactory;
import com.zebrunner.carina.webdriver.listener.DriverListener;

/**
 * DriverFactory produces driver instance with capabilities according to configuration.
 * <b>For internal usage only</b>
 *
 * @author Alexey Khursevich (hursevich@gmail.com)
 */
public class DriverFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // class have only static methods, so constructor should be hidden
    private DriverFactory() {
        // hide
    }

    public static ImmutablePair<WebDriver, Capabilities> create(String testName, Capabilities capabilities, String seleniumHost) {
		LOGGER.debug("DriverFactory start...");
        AbstractFactory factory = null;

        URL seleniumUrl = RemoteWebDriverFactory.getSeleniumHubUrl();
        if (seleniumUrl != null) {
            // override existing selenium_url in config
            R.CONFIG.put(WebDriverConfiguration.Parameter.SELENIUM_URL.getKey(), seleniumUrl.toString());
        }

        DriverType driverType = capabilities == null ? WebDriverConfiguration.getDriverType() : CapabilityUtils.getDriverType(capabilities);
		switch (driverType) {
        case DESKTOP:
			factory = new DesktopFactory();
			break;
        case MOBILE:
			factory = new MobileFactory();
			break;
        case WINDOWS:
            factory = new WindowsFactory();
            break;
        case MAC:
            factory = new MacFactory();
            break;
		default:
            throw new IllegalArgumentException(String.format("Unsupported driver type: '%s'", driverType));
        }

        LOGGER.info("Starting driver session...");
        ImmutablePair<WebDriver, Capabilities> pair = factory.create(testName, capabilities, seleniumHost);
        LOGGER.info("Driver session started.");
        LOGGER.debug("DriverFactory finish...");

        return new ImmutablePair<>(new CarinaEventFiringDecorator<>(getEventListeners(pair.getLeft()))
                .decorate(pair.getLeft()), pair.getRight());
    }

    /**
     * Reads 'driver_event_listeners' configuration property and initializes appropriate array of driver event listeners.
     *
     * @return list of driver listeners (default listener plus custom listeners)
     */
    private static WebDriverListener[] getEventListeners(WebDriver driver) {
        List<WebDriverListener> listeners = new ArrayList<>();

        // explicitly add default carina com.qaprosoft.carina.core.foundation.webdriver.listener.DriverListener
        DriverListener driverListener = new DriverListener(driver);
        listeners.add(driverListener);

        Configuration.get(WebDriverConfiguration.Parameter.DRIVER_EVENT_LISTENERS).ifPresent(listenerClasses -> {
            for (String listenerClass : listenerClasses.split(",")) {
                try {
                    Class<?> clazz = Class.forName(listenerClass);
                    if (WebDriverListener.class.isAssignableFrom(clazz)) {
                        WebDriverListener listener = null;

                        Constructor<?> constructor = ConstructorUtils.getMatchingAccessibleConstructor(clazz, WebDriver.class);
                        if (constructor != null) {
                            listener = (WebDriverListener) constructor.newInstance(driver);
                        } else {
                            constructor = ConstructorUtils.getAccessibleConstructor(clazz);
                            if (constructor == null) {
                                LOGGER.error("Unable to register '{}' webdriver event listener! No default constructor found", listenerClass);
                                continue;
                            }
                            listener = (WebDriverListener) constructor.newInstance();
                        }
                        listeners.add(listener);
                        LOGGER.debug("WebDriver event listener registered: {}", clazz.getName());
                    }
                } catch (ClassNotFoundException e) {
                    LOGGER.error("Unable to register '{}' webdriver event listener! Class was not found", listenerClass, e);

                } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    LOGGER.error("Unable to register '{}' webdriver event listener! Please, investigate stacktrace!", listenerClass, e);
                }
            }
        });

        return listeners.toArray(new WebDriverListener[0]);
    }

}
