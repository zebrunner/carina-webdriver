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
package com.zebrunner.carina.webdriver.screenshot;

import java.time.Duration;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.webdriver.ScreenshotType;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;

public interface IScreenshotRule {

    /**
     * Get the type of screenshots for which the current rule will apply
     * 
     * @return {@link ScreenshotType}
     */
    ScreenshotType getScreenshotType();

    /**
     * Take a screenshot or not
     * 
     * @return true if allow capture screenshot, false otherwise
     */
    boolean isTakeScreenshot();

    /**
     * Allow full-size screenshot or not
     * 
     * @return true if allow capture of full-size screenshot, false otherwise
     */
    boolean isAllowFullSize();

    /**
     * Get image resize dimensions
     * 
     * @return {@link ImmutablePair}, left - width, right - height
     */
    default ImmutablePair<Integer, Integer> getImageResizeDimensions() {
        return new ImmutablePair<>(Configuration.get(WebDriverConfiguration.Parameter.BIG_SCREEN_WIDTH, Integer.class).orElse(-1),
                Configuration.get(WebDriverConfiguration.Parameter.BIG_SCREEN_HEIGHT, Integer.class).orElse(-1));
    }

    /**
     * Get timeout for screenshot capturing
     * 
     * @return {@link Duration} timeout
     */
    default Duration getTimeout() {
        int divider = isAllowFullSize() ? 2 : 3;
        return Duration.ofSeconds(Configuration.getRequired(WebDriverConfiguration.Parameter.EXPLICIT_TIMEOUT, Integer.class) / divider);
    }
}
