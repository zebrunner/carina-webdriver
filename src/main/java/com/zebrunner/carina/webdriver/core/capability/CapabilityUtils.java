package com.zebrunner.carina.webdriver.core.capability;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Optional;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.CapabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.appium.java_client.internal.CapabilityHelpers;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.remote.MobilePlatform;

public final class CapabilityUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private CapabilityUtils() {
        // hide
    }

    public static DriverType getDriverType(Capabilities capabilities) {
        Optional<String> platform = Optional.ofNullable(capabilities.getCapability(CapabilityType.PLATFORM_NAME) instanceof Platform
                ? ((Platform) capabilities.getCapability(CapabilityType.PLATFORM_NAME)).name()
                : (String) capabilities.getCapability(CapabilityType.PLATFORM_NAME));
        Optional<String> browserName = Optional.ofNullable((String) (capabilities.getCapability(CapabilityType.BROWSER_NAME)));
        Optional<String> udid = Optional.ofNullable(CapabilityHelpers.getCapability(capabilities, MobileCapabilityType.UDID, String.class));

        if (platform.isPresent() && (MobilePlatform.ANDROID.equalsIgnoreCase(platform.get()) ||
                MobilePlatform.IOS.equalsIgnoreCase(platform.get()) ||
                MobilePlatform.TVOS.equalsIgnoreCase(platform.get()))) {
            return DriverType.MOBILE;
        }

        if (browserName.isPresent()) {
            return DriverType.DESKTOP;
        }

        if (platform.isEmpty()) {
            // handle use-case when we provide only uuid object among desired capabilities
            if (udid.isPresent()) {
                LOGGER.debug("Detected MOBILE driver_type by uuid inside capabilities.");
                return DriverType.MOBILE;
            }
            throw new IllegalArgumentException(String.format("Cannot detect driver type. Capabilities '%s', '%s', '%s' are empty.",
                    CapabilityType.BROWSER_NAME, CapabilityType.PLATFORM_NAME, MobileCapabilityType.UDID));
        }

        if (MobilePlatform.WINDOWS.equalsIgnoreCase(platform.get())) {
            return DriverType.WINDOWS;
        }

        if (MobilePlatform.MAC.equalsIgnoreCase(platform.get())) {
            return DriverType.MAC;
        }

        throw new IllegalArgumentException(String.format("Cannot detect driver type. Unsupported platform: '%s'", platform.get()));
    }

    public static <T> Optional<T> getZebrunnerCapability(Capabilities capabilities, String capabilityName, Class<T> clazz) {
        Object value = capabilities.getCapability("zebrunner:" + capabilityName);
        if (value == null) {
            value = capabilities.getCapability("zebrunner:options");
            if (value != null) {
                value = ((HashMap<String, Object>) value).get(capabilityName);
            } else {
                value = CapabilityHelpers.getCapability(capabilities, capabilityName, Object.class);
            }
        }
        if (clazz == String.class) {
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(clazz.cast(String.valueOf(value)));
        }
        return Optional.ofNullable(clazz.cast(value));
    }
}
