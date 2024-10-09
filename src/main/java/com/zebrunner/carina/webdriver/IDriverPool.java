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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apiguardian.api.API;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.support.decorators.Decorated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.common.CommonUtils;
import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.exception.DriverPoolException;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration.Parameter;
import com.zebrunner.carina.webdriver.core.factory.DriverFactory;
import com.zebrunner.carina.webdriver.device.Device;
import com.zebrunner.carina.webdriver.listener.DriverListener;

import io.appium.java_client.remote.MobileCapabilityType;

public interface IDriverPool {

    @API(status = API.Status.INTERNAL)
    Logger I_DRIVER_POOL_LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Store drivers by thread id and driver name
     */
    @SuppressWarnings("squid:S2386")
    @API(status = API.Status.INTERNAL)
    ConcurrentHashMap<Long, Map<String, CarinaDriver>> DRIVERS_POOL = new ConcurrentHashMap<>();

    /**
     * Background process for closing drivers. Scalable depends on requirements.
     * In carina-core in the shutdown logic we wait until all tasks will be completed
     */
    @API(status = API.Status.INTERNAL)
    ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(10, Integer.MAX_VALUE, 120L, TimeUnit.SECONDS,
            new SynchronousQueue<>());

    /**
     * Store device object for current thread
     */
    // todo remove this storage
    @API(status = API.Status.INTERNAL)
    ThreadLocal<Device> CURRENT_DEVICE = new ThreadLocal<>();

    // todo check if it is possible to remove usage of this object and reuse Optional for methods that return Device object
    @API(status = API.Status.INTERNAL)
    Device nullDevice = new Device();

    @API(status = API.Status.INTERNAL)
    ThreadLocal<Capabilities> CUSTOM_CAPABILITIES = new ThreadLocal<>();

    /**
     * Default driver name
     */
    @API(status = API.Status.STABLE)
    String DEFAULT = "default";

    /**
     * Get default driver. If no default driver discovered it will be created for current test (thread).
     *
     * @return {@link WebDriver}
     */
    @API(status = API.Status.STABLE)
    default WebDriver getDriver() {
        return getDriver(DEFAULT);
    }

    /**
     * Get driver by name. If no driver discovered it will be created for current test (thread).
     *
     * @param name driver name
     * @return {@link WebDriver}
     */
    @API(status = API.Status.STABLE)
    default WebDriver getDriver(String name) {
        // CUSTOM_CAPABILITIES.get() return registered custom capabilities or null as earlier
        return getDriver(name, CUSTOM_CAPABILITIES.get(), null);
    }

    /**
     * Get driver by name. If no driver discovered it will be created for current test (thread) with provided capabilities
     *
     * @param name driver name
     * @param capabilities {@link Capabilities}
     * @return {@link WebDriver}
     */
    @API(status = API.Status.STABLE)
    default WebDriver getDriver(String name, @Nullable Capabilities capabilities) {
        return getDriver(name, capabilities, null);
    }

    /**
     * Get driver by name. If no driver discovered it will be created using custom capabilities and custom selenium server host
     *
     * @param name driver name
     * @param capabilities {@link Capabilities}
     * @param seleniumHost selenium host URL
     * @return {@link WebDriver}
     */
    @API(status = API.Status.STABLE)
    default WebDriver getDriver(String name, @Nullable Capabilities capabilities, @Nullable String seleniumHost) {
        Optional<CarinaDriver> carinaDriver = getCarinaDriver(name);
        if (carinaDriver.isPresent()) {
            if (TestPhase.Phase.BEFORE_SUITE.equals(carinaDriver.get().getPhase())) {
                I_DRIVER_POOL_LOGGER.info("Before suite registered driver will be returned.");
            } else {
                I_DRIVER_POOL_LOGGER.debug("{} registered driver will be returned.", carinaDriver.get().getPhase());
            }
            return carinaDriver.get()
                    .getDriver();
        }
        I_DRIVER_POOL_LOGGER.debug("Starting new driver as nothing was found in the pool");
        return createDriver(name, capabilities, seleniumHost).getDriver();
    }

    /**
     * Get driver by sessionId.
     *
     * @param sessionId session id to be used for searching a desired driver
     * @return {@link WebDriver}
     */
    @API(status = API.Status.INTERNAL)
    public static WebDriver getDriver(SessionId sessionId) {
        for (CarinaDriver carinaDriver : getDrivers().values()) {
            WebDriver drv = carinaDriver.getDriver();
            if (Objects.requireNonNull(sessionId).equals(DriverListener.castDriver(drv, RemoteWebDriver.class).getSessionId())) {
                return drv;
            }
        }
        throw new DriverPoolException("Unable to find driver using sessionId artifacts. Returning default one!");
    }

    /**
     * Get driver registered to device. If no device discovered null will be returned.
     *
     * @param device {@link Device}
     * @return {@link WebDriver}
     */
    @API(status = API.Status.INTERNAL)
    default WebDriver getDriver(Device device) {
        WebDriver drv = null;
        for (CarinaDriver carinaDriver : getDrivers().values()) {
            if (carinaDriver.getDevice().equals(device)) {
                drv = carinaDriver.getDriver();
            }
        }
        return drv;
    }

    /**
     * Restart default driver
     *
     * @return {@link WebDriver}
     */
    @API(status = API.Status.STABLE)
    default WebDriver restartDriver() {
        return restartDriver(false);
    }

    /**
     * Restart default driver on the same device
     *
     * @param isSameDevice restart driver on the same device or not
     * @return {@link WebDriver}
     */
    @API(status = API.Status.STABLE)
    default WebDriver restartDriver(boolean isSameDevice) {
        return restartDriver(isSameDevice, null);
    }

    /**
     * Restart default driver on the same device with additional capabilities
     *
     * @param isSameDevice restart driver on the same device or not
     * @param additionalOptions {@link Capabilities}
     * @return {@link WebDriver}
     */
    @API(status = API.Status.STABLE)
    default WebDriver restartDriver(boolean isSameDevice, @Nullable Capabilities additionalOptions) {
        CarinaDriver driver = getCarinaDriver(DEFAULT)
                .orElseThrow(() -> new DriverPoolException(String.format("Could not find '%s' driver. "
                        + "Please check that driver exists before 'restartDriver' method call.", DEFAULT)));
        quitDriver(DEFAULT);

        Capabilities capabilities = driver.getOriginalCapabilities()
                .merge(additionalOptions);
        if (isSameDevice) {
            MutableCapabilities udidCaps = new MutableCapabilities();
            udidCaps.setCapability(MobileCapabilityType.UDID, driver.getDevice().getUdid());
            capabilities = capabilities.merge(udidCaps);
        }
        return createDriver(DEFAULT, capabilities, null)
                .getDriver();
    }

    /**
     * Quit default driver
     */
    @API(status = API.Status.STABLE)
    default void quitDriver() {
        quitDriver(DEFAULT);
    }

    /**
     * Quit driver by name
     *
     * @param name driver name
     */
    @API(status = API.Status.STABLE)
    @SuppressWarnings("squid:S1181")
    default void quitDriver(String name) {
        quitDriver(name, Thread.currentThread().getId());
    }

    @API(status = API.Status.INTERNAL)
    @SuppressWarnings("squid:S1181")
    static void quitDriver(String name, Long threadId) {
        DRIVERS_POOL.computeIfAbsent(threadId, k -> new ConcurrentHashMap<>(0))
                .computeIfPresent(name, (k, carinaDriver) -> {
                    try {
                        EXECUTOR_SERVICE.submit(new CloseDriverTask(carinaDriver) {
                            @Override
                            public void run() {
                                I_DRIVER_POOL_LOGGER.debug("Starting driver quit process for {}-{}", getCarinaDriver().getThreadId(),
                                        getCarinaDriver().getName());
                                try {
                                    getCarinaDriver().getDevice().disconnectRemote();
                                } catch (Throwable e) {
                                    I_DRIVER_POOL_LOGGER.warn("Unsuccessful remote disconnect", e);
                                }
                                // castDriver to disable DriverListener operations on quit
                                WebDriver drv = castDriver(getCarinaDriver().getDriver());

                                if (Configuration.get(WebDriverConfiguration.Parameter.CHROME_CLOSURE, Boolean.class).orElse(false)) {
                                    try {
                                        // workaround to not cleaned chrome profiles on hard drive
                                        drv.close();
                                    } catch (Throwable e) {
                                        I_DRIVER_POOL_LOGGER.warn("Unsuccessful driver close process");
                                    }
                                }
                                try {
                                    drv.quit();
                                } catch (Throwable e) {
                                    I_DRIVER_POOL_LOGGER.error("Unable to quit driver! Cause: {}", e.getMessage(), e);
                                }
                                I_DRIVER_POOL_LOGGER.debug("Finished driver quit process for {}-{}", getCarinaDriver().getThreadId(),
                                        getCarinaDriver().getName());
                            }
                        });
                    } catch (Throwable e) {
                        I_DRIVER_POOL_LOGGER.warn("Unsuccessful submit driver quit task");
                    }
                    return null;
                });
    }

    /**
     * Quit drivers in current thread by phase(s). "Current" means assigned to the current test/thread.
     *
     * @param phase comma separated driver phases to quit
     */
    @API(status = API.Status.INTERNAL)
    default void quitDrivers(TestPhase.Phase... phase) {
        List<TestPhase.Phase> phases = Arrays.asList(phase);
        Set<String> drivers4Remove = new HashSet<>(1);
        Long threadId = Thread.currentThread().getId();
        for (CarinaDriver carinaDriver : getDrivers().values()) {
            if ((phases.contains(carinaDriver.getPhase()) && threadId.equals(carinaDriver.getThreadId())) || phases.contains(TestPhase.Phase.ALL)) {
                drivers4Remove.add(carinaDriver.getName());
            }
        }
        drivers4Remove.forEach(this::quitDriver);
        removeCapabilities();
    }

    /**
     * Set custom capabilities that will be used for current thread (test).
     *
     * @param capabilities {@link Capabilities}
     */
    @API(status = API.Status.STABLE)
    default void setCapabilities(Capabilities capabilities) {
        CUSTOM_CAPABILITIES.set(capabilities);
    }

    /**
     * Clear custom capabilities.
     */
    @API(status = API.Status.STABLE)
    default void removeCapabilities() {
        CUSTOM_CAPABILITIES.remove();
    }

    @API(status = API.Status.INTERNAL)
    abstract class CloseDriverTask implements Runnable {
        private final CarinaDriver driver;

        CloseDriverTask(CarinaDriver driver) {
            this.driver = driver;
        }

        public CarinaDriver getCarinaDriver() {
            return driver;
        }
    }

    /**
     * Get {@link CarinaDriver} registered for current thread (test) (if it exists)
     * 
     * @param name driver name
     * @return {@link Optional} of {@link CarinaDriver}
     */
    @API(status = API.Status.INTERNAL)
    private static Optional<CarinaDriver> getCarinaDriver(String name) {
        long threadId = Thread.currentThread().getId();
        return Optional.ofNullable(DRIVERS_POOL.computeIfAbsent(threadId, k -> new ConcurrentHashMap<>(0))
                .getOrDefault(name, null));
    }

    /**
     * Get driver without listeners, for example without {@link DriverListener}
     * 
     * @param drv {@link WebDriver}
     * @return {@link WebDriver}
     */
    @API(status = API.Status.INTERNAL)
    private static WebDriver castDriver(WebDriver drv) {
        if (drv instanceof Decorated<?>) {
            drv = (WebDriver) ((Decorated<?>) drv).getOriginal();
        }
        return drv;
    }

    /**
     * Create driver
     *
     * @param name driver name
     * @param capabilities {@link Capabilities}
     * @param seleniumHost selenium host url
     * @return {@link ImmutablePair} with {@link WebDriver} and original {@link Capabilities}
     */
    @API(status = API.Status.INTERNAL)
    private static CarinaDriver createDriver(String name, @Nullable Capabilities capabilities, @Nullable String seleniumHost) {
        int count = 0;
        CarinaDriver drv = null;
        Device device = nullDevice;

        // 1 - is default run without retry
        int maxCount = Configuration.getRequired(Parameter.INIT_RETRY_COUNT, Integer.class) + 1;
        int maxDriverCount = Configuration.getRequired(Parameter.MAX_DRIVER_COUNT, Integer.class);
        long threadId = Thread.currentThread().getId();
        while (drv == null && count++ < maxCount) {
            try {
                Map<String, CarinaDriver> currentDrivers = getDrivers();
                if (currentDrivers.size() == maxDriverCount) {
                    throw new DriverPoolException(String.format("Unable to create new driver as you reached max number of drivers per thread: %s !" +
                            " Override max_driver_count to allow more drivers per test!", maxDriverCount));
                }
                // [VD] pay attention that similar piece of code is copied into the DriverPoolTest as registerDriver method!
                if (currentDrivers.containsKey(name)) {
                    // [VD] moved containsKey verification before the driver start
                    throw new DriverPoolException(String.format("Driver '%s' is already registered for thread: %s", name, threadId));
                }
                ImmutablePair<WebDriver, Capabilities> pair = DriverFactory.create(name, capabilities, seleniumHost);
                if (CURRENT_DEVICE.get() != null) {
                    device = CURRENT_DEVICE.get();
                }
                drv = new CarinaDriver(name, pair.getLeft(), device, TestPhase.getActivePhase(), threadId, pair.getRight());
                DRIVERS_POOL.computeIfAbsent(threadId, k -> new ConcurrentHashMap<>(1))
                        .put(name, drv);
            } catch (Throwable e) {
                device.disconnectRemote();
                // TODO: [VD] think about excluding device from pool for explicit reasons like out of space etc
                // but initially try to implement it on selenium-hub level
                if (count == maxCount) {
                    throw e;
                } else {
                    // do not provide huge stacktrace as more retries exists. Only latest will generate full error + stacktrace
                    I_DRIVER_POOL_LOGGER.error(String.format("Driver initialization '%s' FAILED! Retry %d of %d time - %s",
                            name, count, maxCount, e.getMessage()));
                    CommonUtils.pause(Configuration.getRequired(Parameter.INIT_RETRY_INTERVAL, Integer.class));
                }
            }
        }
        if (drv == null) {
            throw new RuntimeException("Undefined exception detected! Analyze above logs for details.");
        }
        return drv;
    }

    /**
     * Verify if driver with provided name is registered in current thread (test)
     *
     * @param name driver name
     * @return true if registered, false otherwise
     */
    @API(status = API.Status.STABLE)
    default boolean isDriverRegistered(String name) {
        return getDrivers().containsKey(name);
    }

    /**
     * Get drivers registered for the current thread (test)
     *
     * @return {@link Map} of driver names and {@link CarinaDriver}
     */
    @API(status = API.Status.INTERNAL)
    static Map<String, CarinaDriver> getDrivers() {
        long threadId = Thread.currentThread().getId();
        return DRIVERS_POOL.computeIfAbsent(threadId, k -> new ConcurrentHashMap<>(0));
    }

    // ------------------------ DEVICE POOL METHODS -----------------------

    /**
     * Get device registered to default driver. If no default driver discovered {@link #nullDevice} will be returned
     *
     * @return {@link Device}
     */
    @API(status = API.Status.STABLE)
    default Device getDevice() {
        return getDevice(DEFAULT);
    }

    /**
     * Get {@link Device} registered to the driver with provided name. If no driver discovered {@link #nullDevice} will be returned
     *
     * @param name driver name
     * @return {@link Device}
     */
    @API(status = API.Status.STABLE)
    default Device getDevice(String name) {
        Optional<CarinaDriver> drv = getCarinaDriver(name);
        if (drv.isPresent()) {
            return drv.get()
                    .getDevice();
        }
        return nullDevice;
    }

    /**
     * Get device registered for the provided driver. If no driver discovered nullDevice will be returned
     *
     * @param driver {@link WebDriver}
     * @return {@link Device}
     */
    @API(status = API.Status.INTERNAL)
    default Device getDevice(WebDriver driver) {
        Device device = nullDevice;
        for (CarinaDriver drv : getDrivers().values()) {
            if (drv.getDriver().equals(driver)) {
                device = drv.getDevice();
                break;
            }
        }
        return device;
    }

    /**
     * Register device information for current thread (test)
     *
     * @param device {@link Device}
     * @return {@link Device}
     */
    @API(status = API.Status.INTERNAL)
    static Device registerDevice(Device device) {
        // register current device to be able to transfer it into Zafira at the end of the test
        long threadId = Thread.currentThread().getId();
        I_DRIVER_POOL_LOGGER.debug("Set current device '{}' to thread: {}", device.getName(), threadId);
        CURRENT_DEVICE.set(device);
        I_DRIVER_POOL_LOGGER.debug("register device for current thread id: {}; device: '{}'", threadId, device.getName());
        Configuration.get(SpecialKeywords.ENABLE_ADB, Boolean.class).ifPresent(enableAdb -> {
            if (enableAdb) {
                device.connectRemote();
            }
        });
        return device;
    }

    /**
     * Return last registered device information for current thread
     *
     * @return {@link Device} if device information registered, {@link #nullDevice} otherwise
     * @deprecated use {@link #getDevice(String)} instead
     */
    @Deprecated(forRemoval = true)
    static Device getDefaultDevice() {
        long threadId = Thread.currentThread().getId();
        Device device = CURRENT_DEVICE.get();
        if (device == null) {
            device = nullDevice;
        } else if (device.getName().isEmpty()) {
            I_DRIVER_POOL_LOGGER.debug("Current device name is empty! nullDevice was used for thread: {}", threadId);
        } else {
            I_DRIVER_POOL_LOGGER.debug("Current device name is '{}' for thread: {}", device.getName(), threadId);
        }
        return device;
    }

    /**
     * Get {@link #nullDevice} device. It is not recommended to use such method,
     * because nullDevice can be removed in future releases
     * 
     * @return {@link Device}
     */
    @API(status = API.Status.DEPRECATED)
    static Device getNullDevice() {
        return nullDevice;
    }

    /**
     * Check if device is registered for current thread (test)
     * 
     * @return true if registered, false otherwise
     * @deprecated should not be used on client / module side
     */
    @Deprecated(forRemoval = true)
    default boolean isDeviceRegistered() {
        Device device = CURRENT_DEVICE.get();
        return device != null && device != nullDevice;
    }
}
