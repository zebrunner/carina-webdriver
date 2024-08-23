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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.apiguardian.api.API;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.support.decorators.Decorated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.R;
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
    Logger POOL_LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @API(status = API.Status.INTERNAL)
    String DEFAULT = "default";

    @API(status = API.Status.INTERNAL)
    ExecutorService EXECUTORS_POOL = Executors.newCachedThreadPool();

    @API(status = API.Status.INTERNAL)
    Map<Long, Map<String, CarinaDriver>> DRIVERS = new ConcurrentHashMap<>();

    @API(status = API.Status.INTERNAL)
    ThreadLocal<Capabilities> CUSTOM_CAPABILITIES = new ThreadLocal<>();

    @API(status = API.Status.INTERNAL)
    AtomicBoolean BEFORE_SUITE_DRIVER_REGISTERED = new AtomicBoolean(false);

    Device nullDevice = new Device();

    @API(status = API.Status.INTERNAL)
    ThreadLocal<Device> CURRENT_DEVICE = ThreadLocal.withInitial(() -> nullDevice);

    @API(status = API.Status.INTERNAL)
    Integer MAX_DRIVER_COUNT = Configuration.getRequired(Parameter.MAX_DRIVER_COUNT, Integer.class);

    /**
     * Get driver (with <b>default</b> name). If no <b>default</b> driver discovered in current thread it will be created. <br>
     * Capabilities for this driver will be loaded from {@link R#CONFIG} storage.
     *
     * @return {@link WebDriver}
     */
    @API(status = API.Status.STABLE)
    default WebDriver getDriver() {
        return getDriver(DEFAULT);
    }

    /**
     * Get driver by name. If no driver discovered with such name in current thread it will be created. <br>
     * Capabilities for this driver will be loaded from {@link R#CONFIG} storage.
     *
     * @param name driver name, for example 'chrome-driver'
     * @return {@link WebDriver}
     */
    @API(status = API.Status.STABLE)
    default WebDriver getDriver(String name) {
        // customCapabilities.get() return registered custom capabilities or null as earlier
        return getDriver(name, CUSTOM_CAPABILITIES.get(), null);
    }

    /**
     * Get driver by name. If no driver discovered with such name in current thread
     * it will be created using provided capabilities.<br>
     *
     * @param name driver name, for example 'chrome-driver'
     * @param capabilities capabilities, that will be used instead of {@link R#CONFIG} storage capabilities.
     * @return {@link WebDriver}
     */
    @API(status = API.Status.STABLE)
    default WebDriver getDriver(String name, @Nullable Capabilities capabilities) {
        return getDriver(name, capabilities, null);
    }

    /**
     * Get driver by name. If no driver discovered with such name in current thread it will be created using
     * provided capabilities and selenium server url<br>
     *
     * @param name driver name, for example 'chrome-driver'
     * @param capabilities capabilities, that will be used instead of {@link R#CONFIG} storage capabilities.
     * @param seleniumHost selenium server URL, for example {@code https://localhost:4444/wd/hub}
     * @return {@link WebDriver}
     */
    @API(status = API.Status.STABLE)
    default WebDriver getDriver(String name, @Nullable Capabilities capabilities, @Nullable String seleniumHost) {
        return getCarinaDriver(name, Thread.currentThread().getId())
                .orElseGet(() -> {
                    if (!(TestPhase.getActivePhase() == TestPhase.Phase.BEFORE_METHOD ||
                            TestPhase.getActivePhase() == TestPhase.Phase.BEFORE_SUITE ||
                            TestPhase.getActivePhase() == TestPhase.Phase.METHOD)) {
                        throw new DriverPoolException("Driver can be created only in 'BeforeMethod', 'BeforeSuite' or 'Method' phases. Maybe driver "
                                + " already closed.");
                    }
                    if (getDriversMapByThread(Thread.currentThread().getId()).size() == MAX_DRIVER_COUNT) {
                        throw new DriverPoolException(
                                String.format("Unable to create new driver as you reached max number of drivers per thread: %s !" +
                                        " Override max_driver_count to allow more drivers per test!", MAX_DRIVER_COUNT));
                    }

                    return createCarinaDriver(name, capabilities, seleniumHost);
                }).getDriver();
    }

    /**
     * Get driver by sessionId.
     *
     * @param sessionId session id to be used for searching a desired driver
     * @return default WebDriver
     * @deprecated use {@link #getDriver()} or {@link #getDriver(String)} instead
     */
    @Deprecated(forRemoval = true)
    public static WebDriver getDriver(SessionId sessionId) {
        for (CarinaDriver carinaDriver : getDriversMapByThread(Thread.currentThread().getId()).values()) {
            WebDriver drv = carinaDriver.getDriver();
            SessionId drvSessionId = DriverListener.castDriver(drv, RemoteWebDriver.class)
                    .getSessionId();
            if (sessionId.equals(drvSessionId)) {
                return drv;
            }
        }
        throw new DriverPoolException("Unable to find driver using sessionId artifacts. Returning default one!");
    }

    /**
     * Get driver registered to device. If no device discovered null will be returned.
     *
     * @param device Device
     * @return WebDriver
     * @deprecated use {@link #getDriver()} or {@link #getDriver(String)} instead
     */
    @Deprecated(forRemoval = true)
    default WebDriver getDriver(Device device) {
        WebDriver drv = null;
        for (CarinaDriver carinaDriver : getDriversMapByThread(Thread.currentThread().getId()).values()) {
            if (carinaDriver.getDevice().equals(device)) {
                drv = carinaDriver.getDriver();
            }
        }
        return drv;
    }

    /**
     * Restart <b>default</b> driver
     *
     * @return {@link WebDriver}
     */
    @API(status = API.Status.STABLE)
    default WebDriver restartDriver() {
        return restartDriver(DEFAULT, false, null);
    }

    /**
     * Restart <b>default</b> driver
     *
     * @param isSameDevice should we restart driver on the same device or not
     * @return {@link WebDriver}
     */
    @API(status = API.Status.STABLE)
    default WebDriver restartDriver(boolean isSameDevice) {
        return restartDriver(DEFAULT, isSameDevice, null);
    }

    /**
     * Restart <b>default</b> driver on the same device with additional capabilities
     *
     * @param isSameDevice should we restart driver on the same device or not
     * @param additionalOptions additional capabilities that should be used in driver session
     * @return {@link WebDriver}
     */
    @API(status = API.Status.STABLE)
    default WebDriver restartDriver(boolean isSameDevice, @Nullable Capabilities additionalOptions) {
        return restartDriver(DEFAULT, isSameDevice, additionalOptions);
    }

    /**
     * Restart driver by name
     *
     * @param name driver name, for example 'chrome-driver'
     * @return {@link WebDriver}
     */
    @API(status = API.Status.MAINTAINED)
    default WebDriver restartDriver(String name) {
        return restartDriver(name, false, null);
    }

    /**
     * Restart driver by name
     *
     * @param name driver name, for example 'chrome-driver'
     * @param isSameDevice should we restart driver on the same device or not
     * @return {@link WebDriver}
     */
    @API(status = API.Status.MAINTAINED)
    default WebDriver restartDriver(String name, boolean isSameDevice) {
        return restartDriver(name, isSameDevice, null);
    }

    /**
     * Restart driver by name on the same device with additional capabilities
     *
     * @param name driver name, for example 'chrome-driver'
     * @param isSameDevice should we restart driver on the same device or not
     * @param additionalOptions additional capabilities that should be used in driver session
     * @return {@link WebDriver}
     */
    @API(status = API.Status.MAINTAINED)
    default WebDriver restartDriver(String name, boolean isSameDevice, @Nullable Capabilities additionalOptions) {
        CarinaDriver drv = getCarinaDriver(name, Thread.currentThread().getId())
                .orElseThrow(() -> new DriverPoolException(
                        String.format("Could not restart '%s' driver due to there are no such driver in current thread.", name)));
        MutableCapabilities udidCaps = new MutableCapabilities();
        Device device = drv.getDevice();
        if (isSameDevice) {
            if (device == nullDevice) {
                POOL_LOGGER.warn("Could not restart driver on the same device, because there are no registered device for driver '{}'.", name);
            } else {
                POOL_LOGGER.debug("Added udid: {} to capabilities for restartDriver on the same device.", device
                        .getUdid());
                udidCaps.setCapability(MobileCapabilityType.UDID, device
                        .getUdid());
            }
        }
        udidCaps = udidCaps.merge(additionalOptions);

        quitDriver(name);
        return getDriver(name, drv.getOriginalCapabilities()
                .merge(udidCaps));
    }

    /**
     * Quit <b>default</b> driver. Driver will be removed from the pool
     */
    @API(status = API.Status.STABLE)
    default void quitDriver() {
        quitDriver(DEFAULT);
    }

    /**
     * Quit driver by name. Driver will be removed from the pool
     *
     * @param name driver name, for example 'chrome-driver'
     * @throws DriverPoolException if there are no such driver in pool
     */
    @API(status = API.Status.STABLE)
    default void quitDriver(String name) {
        Long threadId = Thread.currentThread().getId();
        Optional<CarinaDriver> driver = getCarinaDriver(name, threadId);
        if (driver.isEmpty()) {
            throw new DriverPoolException(String.format("Unable to find driver '%s' in pool!", name));
        } else {
            quitCarinaDriver(name, threadId);
        }
    }

    /**
     * Quit current drivers by phase(s). "Current" means assigned to the current test/thread.
     *
     * @param phase Comma separated driver phases to quit
     */
    default void quitDrivers(TestPhase.Phase... phase) {
        List<TestPhase.Phase> phases = Arrays.asList(phase);
        List<String> driversForRemove = new ArrayList<>(1);
        getDriversMapByThread(Thread.currentThread().getId())
                .values()
                .forEach(driver -> {
                    if (phases.contains(driver.getPhase()) || phases.contains(TestPhase.Phase.ALL)) {
                        driversForRemove.add(driver.getName());
                    }
                });
        driversForRemove.stream()
                .forEach(this::quitDriver);
    }

    /**
     * Set custom capabilities.
     *
     * @param caps capabilities
     */
    default void setCapabilities(Capabilities caps) {
        CUSTOM_CAPABILITIES.set(caps);
    }

    /**
     * Remove custom capabilities.
     */
    default void removeCapabilities() {
        CUSTOM_CAPABILITIES.remove();
    }

    /**
     * Verify if driver is registered in the DriverPool
     *
     * @param name String driver name
     * @return boolean
     */
    @API(status = API.Status.STABLE)
    default boolean isDriverRegistered(String name) {
        return getCarinaDriver(name, Thread.currentThread().getId())
                .isPresent();
    }

    /**
     * Return all drivers registered in the DriverPool for this thread including
     * on Before Suite/Class/Method stages
     *
     * @return ConcurrentHashMap of driver names and Carina WebDrivers
     * @deprecated use {@link #getDriver()} instead
     */
    @API(status = API.Status.INTERNAL)
    default Map<String, CarinaDriver> getDrivers() {
        return getDriversMapByThread(Thread.currentThread().getId());
    }

    // ------------------------ DEVICE POOL METHODS -----------------------

    /**
     * Get device registered to default driver. If no default driver discovered nullDevice will be returned.
     *
     * @return default Device
     */
    @API(status = API.Status.STABLE)
    default Device getDevice() {
        return getDevice(DEFAULT);
    }

    /**
     * Get device registered to named driver.
     *
     * @param name String driver name
     * @return Device
     */
    @API(status = API.Status.STABLE)
    default Device getDevice(String name) {
        Optional<CarinaDriver> driver = getCarinaDriver(name, Thread.currentThread().getId());
        if (driver.isEmpty()) {
            return nullDevice;
        }
        return driver.get()
                .getDevice();
    }

    /**
     * Get device registered to driver. If no driver discovered nullDevice will be returned.
     *
     * @param drv WebDriver
     * @return Device
     */
    @API(status = API.Status.INTERNAL)
    default Device getDevice(WebDriver drv) {
        Device device = nullDevice;

        for (CarinaDriver carinaDriver : getDriversMapByThread(Thread.currentThread().getId()).values()) {
            if (carinaDriver.getDriver().equals(drv)) {
                device = carinaDriver.getDevice();
                break;
            }
        }
        return device;
    }

    /**
     * Register device information for current thread by MobileFactory and clear SysLog for Android only
     *
     * @param device String Device device
     * @return Device device
     */
    @API(status = API.Status.INTERNAL)
    static Device registerDevice(Device device) {
        // register current device to be able to transfer it into Zafira at the end of the test
        long threadId = Thread.currentThread().getId();
        POOL_LOGGER.debug("Set current device '{}' to thread: {}", device.getName(), threadId);
        CURRENT_DEVICE.set(Objects.requireNonNull(device));
        POOL_LOGGER.debug("register device for current thread id: {}; device: '{}'", threadId, device.getName());
        boolean enableAdb = R.CONFIG.getBoolean(SpecialKeywords.ENABLE_ADB);
        if (enableAdb) {
            device.connectRemote();
        }
        return device;
    }

    /**
     * Return last registered device information for current thread.
     *
     * @return Device device
     */
    @Deprecated
    static Device getDefaultDevice() {
        long threadId = Thread.currentThread().getId();
        Device device = CURRENT_DEVICE.get();
        if (device.getName().isEmpty()) {
            POOL_LOGGER.debug("Current device name is empty! nullDevice was used for thread: {}", threadId);
        } else {
            POOL_LOGGER.debug("Current device name is '{}' for thread: {}", device.getName(), threadId);
        }
        return device;
    }

    /**
     * Return nullDevice object to avoid NullPointerException and tons of verification across carina-core modules.
     *
     * @return Device device
     */
    static Device getNullDevice() {
        return nullDevice;
    }

    /**
     * Verify if device is registered in the Pool
     *
     * @return boolean
     */
    default boolean isDeviceRegistered() {
        return CURRENT_DEVICE.get() != nullDevice;
    }

    @API(status = API.Status.INTERNAL)
    public static Optional<CarinaDriver> getCarinaDriver(String name, Long threadId) {
        if (BEFORE_SUITE_DRIVER_REGISTERED.get()) {
            return Optional.of(DRIVERS.values()
                    .stream()
                    .findAny()
                    .orElseThrow(() -> new DriverPoolException("Cannot find any map with drivers."))
                    .values()
                    .stream()
                    .findAny()
                    .orElseThrow(() -> new DriverPoolException("Cannot find before suite driver! But looks like we registered driver previously.")));
        }
        return Optional.ofNullable(
                getDriversMapByThread(threadId)
                        .get(name));
    }

    @API(status = API.Status.INTERNAL)
    public static void quitCarinaDriver(String name, Long threadId) {
        getCarinaDriver(name, threadId).ifPresent(drv -> {
            // default timeout for driver quit 1/2 of explicit
            long timeout = Configuration.getRequired(Parameter.DRIVER_QUIT_TIMEOUT, Integer.class);
            drv.getDevice().disconnectRemote();
            // castDriver to disable DriverListener operations on quit
            Future<?> future = EXECUTORS_POOL.submit(new DriverCloseTask(castDriver(drv.getDriver())));
            try {
                future.get(timeout, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                POOL_LOGGER.error("InterruptedException: Unable to quit driver!", e);
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                POOL_LOGGER.error("ExecutionException: Unable to quit driver!", e);
            } catch (java.util.concurrent.TimeoutException e) {
                POOL_LOGGER.error("Unable to quit driver for {} sec!", timeout, e);
            } catch (Throwable e) {
                POOL_LOGGER.error("Unable to quit driver properly because of unknown error: {}!", e.getMessage(), e);
            } finally {
                if (BEFORE_SUITE_DRIVER_REGISTERED.getAndSet(false)) {
                    DRIVERS.clear();
                    POOL_LOGGER.warn("Removed driver created in the before suite.");
                } else {
                    Map<String, CarinaDriver> drvs = DRIVERS.get(threadId);
                    if (drvs.size() > 1) {
                        drvs.remove(name);
                    } else {
                        drvs.remove(name);
                        DRIVERS.remove(threadId);
                        CURRENT_DEVICE.remove();
                    }
                }
            }
        });
    }

    @API(status = API.Status.INTERNAL)
    private static CarinaDriver createCarinaDriver(String name, @Nullable Capabilities capabilities, @Nullable String seleniumHost) {
        // 1 - is default run without retry
        int maxCount = Configuration.getRequired(Parameter.INIT_RETRY_COUNT, Integer.class) + 1;
        long threadId = Thread.currentThread()
                .getId();

        int count = 0;
        CarinaDriver driver = null;
        while (driver == null && count++ < maxCount) {
            try {
                Pair<WebDriver, Capabilities> pair = DriverFactory.create(name, capabilities, seleniumHost);
                driver = new CarinaDriver(name,
                        pair.getLeft(),
                        CURRENT_DEVICE.get(),
                        TestPhase.getActivePhase(),
                        threadId,
                        pair.getRight());
            } catch (Throwable e) {
                Optional.ofNullable(CURRENT_DEVICE.get())
                        .ifPresent(d -> {
                            d.disconnectRemote();
                            CURRENT_DEVICE.remove();
                        });
                // TODO: [VD] think about excluding device from pool for explicit reasons like out of space etc
                // but initially try to implement it on selenium-hub level
                String msg = String.format("Driver initialization '%s' FAILED! Retry %d of %d time - %s", name, count, maxCount, e.getMessage());
                if (count == maxCount) {
                    throw e;
                } else {
                    // do not provide huge stacktrace as more retries exists. Only latest will generate full error + stacktrace
                    POOL_LOGGER.error(msg);
                }
                CommonUtils.pause(Configuration.getRequired(Parameter.INIT_RETRY_INTERVAL, Integer.class));
            }
        }
        if (TestPhase.getActivePhase() == TestPhase.Phase.BEFORE_SUITE) {
            BEFORE_SUITE_DRIVER_REGISTERED.set(true);
        }
        getDriversMapByThread(threadId)
                .put(name, driver);
        return driver;
    }

    @API(status = API.Status.INTERNAL)
    private static WebDriver castDriver(WebDriver drv) {
        if (drv instanceof Decorated<?>) {
            drv = (WebDriver) ((Decorated<?>) drv).getOriginal();
        }
        return drv;
    }

    @API(status = API.Status.INTERNAL)
    private static Map<String, CarinaDriver> getDriversMapByThread(long threadId) {
        return DRIVERS.computeIfAbsent(threadId,
                id -> new HashMap<>(1));
    }

    @API(status = API.Status.INTERNAL)
    static final class DriverCloseTask implements Callable<Void> {
        private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

        final WebDriver driver;

        DriverCloseTask(WebDriver driver) {
            this.driver = driver;
        }

        public Void call() {
            if (Configuration.get(WebDriverConfiguration.Parameter.CHROME_CLOSURE, Boolean.class).orElse(false)) {
                // workaround to not cleaned chrome profiles on hard drive
                LOGGER.debug("Starting drv.close()");
                try {
                    driver.close();
                } catch (Throwable e) {
                    LOGGER.error("Cannot successfully call drv.close().", e);
                }
                LOGGER.debug("Finished drv.close()");
            }
            LOGGER.debug("Starting drv.quit()");
            try {
                driver.quit();
            } catch (Throwable e) {
                LOGGER.error("Unable to quit driver! Exception: {}", e.getMessage(), e);

            }
            LOGGER.debug("Finished drv.quit()");
            return null;
        }
    }
}
