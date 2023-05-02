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
package com.zebrunner.carina.webdriver.decorator.extractor;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;

@Deprecated(forRemoval = false, since = "1.0.3")
public abstract class AbstractElementExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public boolean isInside(Rectangle rect, int x, int y) {
        return rect.x <= x && rect.x + rect.width >= x && rect.y <= y && rect.y + rect.height >= y;
    }

    /**
     * Method to generate rectangle for the element since current version of
     * appium driver throws unimplemented exception
     * 
     * @param element WebElement
     * @return Rectangle
     */
    public Rectangle getRect(WebElement element) {
        return new Rectangle(element.getLocation(), element.getSize());
    }

    /**
     * Method return 1 in case if y is lower than element, -1 if higher, 0 - if
     * 'y' within the element's range
     * 
     * @param rect Rectangle
     * @param y int
     * @return int
     */
    public int isLower(Rectangle rect, int y) {
        LOGGER.debug("isLower(): Rectangle: x - {}. y - {}. Width: {}, height: {}", rect.x, rect.y, rect.width, rect.height);
        if (y > rect.y + rect.height) {
            return 1;
        } else if (y < rect.y) {
            return -1;
        }
        return 0;
    }

    public abstract ExtendedWebElement getElementsByCoordinates(final int x, final int y);

    /**
     * Method extracts all end level elements (elements have no children) which
     * are on the screen
     *
     * @param driver WebDriver
     * @return list List
     */
    public List<WebElement> getEndLevelElements(WebDriver driver) {
        return driver.findElements(By.xpath("//*[count(./*)=0]"));
    }
}
