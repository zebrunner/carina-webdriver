package com.zebrunner.carina.webdriver.core.capability;

import com.zebrunner.agent.core.webdriver.CapabilitiesCustomizer;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;

/**
 * Hotfix for 'legacy' capabilities that fail session
 * <b>For internal usage only</b>
 */
public class CarinaCapabilitiesCustomizer implements CapabilitiesCustomizer {

    @Override
    public Capabilities customize(Capabilities capabilities) {
        if (capabilities.getCapability("provider") != null) {
            ((MutableCapabilities) capabilities).setCapability("provider", (String) null);
        }
        capabilities.getCapabilityNames()
                .stream()
                .filter(name -> StringUtils.equalsIgnoreCase("zeb-applicationType", name))
                .findFirst()
                .ifPresent((name -> {
                    Object value = capabilities.getCapability(name);
                    ((MutableCapabilities) capabilities).setCapability(name, (String) null);
                    ((MutableCapabilities) capabilities).setCapability("zebrunner:" + name, value);
                }));
        return capabilities;
    }
}
