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
package com.zebrunner.carina.webdriver.gui.mobile.devices.android.phone.pages.tzchanger;

import java.lang.invoke.MethodHandles;

import com.zebrunner.carina.webdriver.IDriverHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.webdriver.DriverHelper;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.gui.mobile.devices.MobileAbstractPage;

public class TZChangerPage extends MobileAbstractPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String TIMEZONE_TEXT_BASE = "//android.widget.TextView[contains(@text,'%s')]";

    @FindBy(id = "com.futurek.android.tzc:id/txt_selected")
    protected ExtendedWebElement title;

    @FindBy(xpath = "//android.widget.ListView")
    protected ExtendedWebElement scrollableContainer;

    @FindBy(xpath = "//android.widget.TextView[@resource-id='android:id/text1' and contains(@text,'%s')]")
    protected ExtendedWebElement tzSelectionBase;

    @FindBy(id = "com.android.settings:id/next_button")
    protected ExtendedWebElement nextButton;

    public TZChangerPage(WebDriver driver) {
        super(driver);
    }

    /**
     * selectTimeZone
     *
     * @param timezone String format should be "Europe/London"
     * @return boolean
     */
    public boolean selectTimeZone(String timezone) {
        int defaultSwipeTime = 30;
        boolean selected = false;
        String tz = "";

        if (timezone.contains("/")) {
            tz = timezone.split("/")[0];
        } else {
            LOGGER.error("Incorrect timezone format: {}", timezone);
            return false;
        }

        LOGGER.info("Searching for tz: {}", tz);
        if (scrollableContainer.isElementPresent(SHORT_TIMEOUT)) {
            LOGGER.info("Scrollable container present.");
            boolean scrolled = swipe(
                    tzSelectionBase.format(tz),
                    scrollableContainer, defaultSwipeTime);
            if (!scrolled) {
                LOGGER.info("Probably we have long list. Let's increase swipe attempts.");
                defaultSwipeTime = 50;
                scrolled = swipe(
                        tzSelectionBase.format(tz),
                        scrollableContainer, defaultSwipeTime);
            }
            if (scrolled) {

                LOGGER.info("Select timezone folder: {}", tz);
                tzSelectionBase.format(tz).click();

                LOGGER.info("Searching for {}", timezone);
                scrolled = swipe(
                        tzSelectionBase.format(timezone),
                        scrollableContainer, defaultSwipeTime);
                if (scrolled) {

                    LOGGER.info("Select timezone by TimeZone text: {}", timezone);
                    tzSelectionBase.format(timezone).click();
                    selected = true;
                } else {
                    LOGGER.error("Did not find timezone by timezone text: {}", timezone);
                    defaultSwipeTime = 30;
                    scrolled = swipe(
                            tzSelectionBase.format(timezone),
                            scrollableContainer, defaultSwipeTime);
                    if (scrolled) {
                        LOGGER.info("Select timezone: {}", timezone);
                        tzSelectionBase.format(timezone).click();
                        selected = true;
                    }
                }
            } else {
                LOGGER.error("Didn't find timezone: {}", timezone);
            }
        }

        return selected;
    }

    /**
     * isOpened
     *
     * @param timeout long
     * @return boolean
     */
    public boolean isOpened(long timeout) {
        return title.isElementPresent(timeout);
    }

    @Override
    public boolean isOpened() {
        return isOpened(IDriverHelper.EXPLICIT_TIMEOUT);
    }
}
