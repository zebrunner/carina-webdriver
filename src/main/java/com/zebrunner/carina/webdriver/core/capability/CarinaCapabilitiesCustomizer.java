package com.zebrunner.carina.webdriver.core.capability;

import com.zebrunner.agent.core.webdriver.CapabilitiesCustomizer;
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
        return capabilities;
    }
}
