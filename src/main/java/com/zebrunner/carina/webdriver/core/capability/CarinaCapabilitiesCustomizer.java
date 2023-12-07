package com.zebrunner.carina.webdriver.core.capability;

import com.zebrunner.agent.core.webdriver.CapabilitiesCustomizer;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hotfix for 'legacy' capabilities that fail session
 * <b>For internal usage only</b>
 */
public class CarinaCapabilitiesCustomizer implements CapabilitiesCustomizer {

    @Override
    public Capabilities customize(Capabilities capabilities) {
        Set<String> capabilitiesMap = capabilities.asMap().keySet()
                .stream()
                .filter(key -> StringUtils.equalsAnyIgnoreCase(key, "provider", "idleTimeout", "enableVNC", "enableLog", "enableVideo", "cpu",
                        "memory"))
                .collect(Collectors.toSet());

        MutableCapabilities mutableCapabilities = (MutableCapabilities) capabilities;
        for (String key : capabilitiesMap) {
            Object value = mutableCapabilities.getCapability(key);
            mutableCapabilities.setCapability(key, (String) null);
            mutableCapabilities.setCapability("zebrunner:" + key, value);
        }
        return mutableCapabilities;
    }
}
