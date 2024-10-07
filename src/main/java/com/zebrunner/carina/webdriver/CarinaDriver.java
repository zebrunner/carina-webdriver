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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

import com.zebrunner.carina.webdriver.TestPhase.Phase;
import com.zebrunner.carina.webdriver.device.Device;

public class CarinaDriver {
	private final String name;
	private final WebDriver driver;
	private final Device device;
	private final Phase phase;
	private long threadId;
	private final Capabilities originalCapabilities;

	public CarinaDriver(String name, WebDriver driver, Device device, Phase phase, long threadId, Capabilities originalCapabilities) {
		super();
		this.name = name;
		this.driver = driver;
		this.device = device;
		this.phase = phase;
		this.threadId = threadId;
		this.originalCapabilities = originalCapabilities;
	}

	public WebDriver getDriver() {
		return driver;
	}

    public Device getDevice() {
        return device;
    }

	public String getName() {
		return name;
	}

	public long getThreadId() {
		return threadId;
	}

	public Phase getPhase() {
		return phase;
	}

    protected void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    /**
     * Get capabilities that used for creating driver.<br>
     * <b>For internal usage only</b>
     *
     * @return {@link Capabilities}
     */
    public Capabilities getOriginalCapabilities() {
        return originalCapabilities;
    }

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;

		if (o == null || getClass() != o.getClass())
			return false;

		CarinaDriver driver = (CarinaDriver) o;

		return new EqualsBuilder().append(threadId, driver.threadId).append(name, driver.name).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(name).append(threadId).toHashCode();
	}
}
