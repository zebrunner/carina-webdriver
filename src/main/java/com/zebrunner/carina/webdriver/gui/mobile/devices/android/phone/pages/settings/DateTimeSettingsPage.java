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
package com.zebrunner.carina.webdriver.gui.mobile.devices.android.phone.pages.settings;

import java.lang.invoke.MethodHandles;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.android.IAndroidUtils;
import com.zebrunner.carina.webdriver.DriverHelper;
import com.zebrunner.carina.webdriver.IDriverPool;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.gui.mobile.devices.MobileAbstractPage;

public class DateTimeSettingsPage extends MobileAbstractPage implements IAndroidUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @FindBy(xpath = "//android.widget.TextView[@text = 'Date & time']")
    private ExtendedWebElement dateAndTimeScreenHeaderTitle;

    @FindBy(xpath = "//android.widget.TextView[@text = 'Time zone']")
    private ExtendedWebElement timeZoneOption;

    @FindBy(xpath = "//android.widget.TextView[@text = 'Region']")
    private ExtendedWebElement timeZoneRegionOption;

    @FindBy(id = "android:id/search_src_text")
    private ExtendedWebElement timeZoneRegionSearchInputField;

    @FindBy(xpath = "//*[@resource-id='com.android.settings:id/recycler_view']//android.widget.TextView[contains(@text,'%s')]")
    private ExtendedWebElement timeZoneRegionSearchResult;

    @FindBy(xpath = "//android.widget.TextView[@text = 'Select time zone']")
    private ExtendedWebElement selectTimeZone;

    @FindBy(xpath = "//android.widget.ListView")
    private ExtendedWebElement scrollableContainer;

    @FindBy(id = "com.android.settings:id/recycler_view")
    private ExtendedWebElement scrollableContainerInVersion81;

    @FindBy(className = "android.widget.ListView")
    private ExtendedWebElement scrollableContainerByClassName;

    @FindBy(xpath = "//android.widget.TextView[contains(@text,'%s')]")
    private ExtendedWebElement tzSelectionBase;

    @FindBy(id = "com.android.settings:id/next_button")
    private ExtendedWebElement nextButton;

    private static final String SELECT_TIME_ZONE_TEXT = "Select time zone";

    public DateTimeSettingsPage(WebDriver driver) {
        super(driver);
    }

    /**
     * openTimeZoneSetting
     */
    public void openTimeZoneSetting() {
        boolean found = selectTimeZone.clickIfPresent(SHORT_TIMEOUT);
        if (!found) {
            boolean scrolled = scroll(SELECT_TIME_ZONE_TEXT, scrollableContainerByClassName,
                    SelectorType.CLASS_NAME, SelectorType.TEXT).isElementPresent();
            if (scrolled) {
                found = selectTimeZone.clickIfPresent(SHORT_TIMEOUT);
            } else {
                throw new RuntimeException("Desired Time Zone Menu item not found.. ");
            }
        }
        LOGGER.info("Select Time Zone Menu item was clicked: {}", found);
    }

    /**
     * selectTimeZone
     *
     * @param tz       String
     * @param timezone String
     * @param tzGMT    String
     * @return boolean
     */
    public boolean selectTimeZone(String tz, String timezone, String tzGMT) {
        boolean selected = false;
        String deviceOsFullVersion = IDriverPool.getDefaultDevice().getOsVersion();
        int deviceOsVersion;

        //Adding extra step required to get to TimeZone screen on devices running versions > 8
        if (deviceOsFullVersion.contains(".")) {
            deviceOsVersion = Integer.parseInt(deviceOsFullVersion.split("\\.")[0]);
        } else {
            deviceOsVersion = Integer.parseInt(deviceOsFullVersion);
        }

        //if device OS version >= 9, we have to set Country Region and obtain city from timeZone
        setupTimezoneRegion(timezone, deviceOsVersion);
        tz = tz.split("/")[1].replace("_", " ");

        //locating timeZone by City
        if (!tz.isEmpty() && locateTimeZoneByCity(tz, deviceOsVersion)) {
            tzSelectionBase.format(tz).click();
            selected = true;
        }

        //locating timeZone by GMT
        if (!selected && locateTimeZoneByGMT(tzGMT, deviceOsVersion)) {
            tzSelectionBase.format(tzGMT).click();
            selected = true;
        }

        return selected;
    }

    /**
     * setup timezone region (this method is responsible for setting up timezone region, req. for OS version &gt; 8)
     *
     * @param timezoneRegion         String
     * @param deviceOsVersion  int
     */
    private void setupTimezoneRegion(String timezoneRegion, int deviceOsVersion){
        if (deviceOsVersion >= 9) {
            LOGGER.info("Detected Android version 8 or above, selecting country region for 'Time Zone' option..");
            timeZoneRegionOption.clickIfPresent();
            timeZoneRegionSearchInputField.type(timezoneRegion);
            timeZoneRegionSearchResult.format(timezoneRegion).click();
        }
    }

    /**
     * selectTimezoneByGMT
     *
     * @param tzGMT           String
     * @param deviceOsVersion int
     * @return boolean
     */
    private boolean locateTimeZoneByGMT(String tzGMT, int deviceOsVersion) {
        LOGGER.info("Searching for tz by GTM: {}", tzGMT);
        boolean result = false;

        try {
            if (deviceOsVersion > 8) {
                result = scroll(tzGMT, scrollableContainerInVersion81,
                        SelectorType.ID, SelectorType.TEXT_CONTAINS).isElementPresent();
            } else {
                result = scroll(tzGMT, scrollableContainerByClassName,
                        SelectorType.CLASS_NAME, SelectorType.TEXT_CONTAINS).isElementPresent();
            }
        } catch (NoSuchElementException e) {
            LOGGER.debug("Element is not found.", e);
        }

        return result;
    }

    /**
     * selectTimezoneByCity
     *
     * @param tz         String
     * @param deviceOsVersion  int
     * @return boolean
     */
    private boolean locateTimeZoneByCity(String tz, int deviceOsVersion){
        LOGGER.info("Searching for tz by City: {}", tz);
        boolean result = false;

        if (deviceOsVersion > 8) {
            try {
                result = scroll(tz, scrollableContainerInVersion81,
                        SelectorType.ID, SelectorType.TEXT_CONTAINS).isElementPresent();
            } catch (NoSuchElementException e){
                LOGGER.debug("Element is not found.", e);
            }
        } else {
            try {
                result = scroll(tz, scrollableContainerByClassName,
                        SelectorType.CLASS_NAME, SelectorType.TEXT_CONTAINS).isElementPresent();
            } catch (NoSuchElementException e){
                LOGGER.debug("Element is not found.", e);
            }
        }

        return result;
    }

    /**
     * clickNextButton
     *
     * @return boolean
     */
    public boolean clickNextButton() {
        boolean res = nextButton.clickIfPresent(SHORT_TIMEOUT);
        LOGGER.info("Next button was clicked: {}", res);
        return res;
    }

    /**
     * isOpened
     *
     * @param timeout long
     * @return boolean
     */
    public boolean isOpened(long timeout) {
        return dateAndTimeScreenHeaderTitle.isElementPresent(timeout);
    }

    @Override
    public boolean isOpened() {
        return isOpened(DriverHelper.EXPLICIT_TIMEOUT);
    }

}
