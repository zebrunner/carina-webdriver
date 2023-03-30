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
package com.zebrunner.carina.webdriver.locator;

import static io.appium.java_client.pagefactory.utils.WebDriverUnpackUtility.getCurrentContentType;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ByIdOrName;
import org.openqa.selenium.support.pagefactory.AbstractAnnotations;
import org.openqa.selenium.support.pagefactory.ByAll;
import org.openqa.selenium.support.pagefactory.ByChained;
import org.openqa.selenium.support.pagefactory.ElementLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.webdriver.decorator.annotations.CaseInsensitiveXPath;
import com.zebrunner.carina.webdriver.decorator.annotations.Localized;
import com.zebrunner.carina.webdriver.locator.converter.LocalizeLocatorConverter;
import com.zebrunner.carina.webdriver.locator.converter.LocatorConverter;
import com.zebrunner.carina.webdriver.locator.converter.caseinsensitive.CaseInsensitiveConverter;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.bys.ContentMappedBy;
import io.appium.java_client.pagefactory.bys.ContentType;

/**
 * The default element locator, which will lazily locate an element or an
 * element list on a page. This class is designed for use with the
 * {@link org.openqa.selenium.support.PageFactory} and understands the
 * annotations {@link org.openqa.selenium.support.FindBy} and
 * {@link org.openqa.selenium.support.CacheLookup}.
 */
public class ExtendedElementLocator implements ElementLocator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final WebDriver driver;
    private SearchContext searchContext;
    private final String className;
    private final By originalBy;
    private By by;
    private boolean caseInsensitive = false;
    private boolean localized = false;
    private final LinkedList<LocatorConverter> locatorConverters = new LinkedList<>();

    /**
     * Creates a new element locator.
     *
     * @param searchContext The context to use when finding the element
     * @param field The field on the Page Object that will hold the located
     *            value
     */
    public ExtendedElementLocator(WebDriver driver, SearchContext searchContext, Field field, AbstractAnnotations annotations) {
        this.driver = driver;
        this.searchContext = searchContext;
        String[] classPath = field.getDeclaringClass().toString().split("\\.");
        this.className = classPath[classPath.length - 1];
        this.by = annotations.buildBy();
        this.originalBy = annotations.buildBy();
        if (LocalizeLocatorConverter.getL10nPattern().matcher(this.by.toString()).find()) {
            this.locatorConverters.add(new LocalizeLocatorConverter());
        }
        if (field.isAnnotationPresent(CaseInsensitiveXPath.class)) {
            CaseInsensitiveXPath csx = field.getAnnotation(CaseInsensitiveXPath.class);
            // [AS] do not try to use searchContext for getCurrentContentType method, because it may be a proxy and when we try to
            // get driver from it, there will be 'org.openqa.selenium.NoSuchElementException' because on this moment page is not opened,
            // so we just use driver instead
            locatorConverters.add(new CaseInsensitiveConverter(csx, ContentType.NATIVE_MOBILE_SPECIFIC.equals(getCurrentContentType(driver))));
            caseInsensitive = true;
        }
        if (field.isAnnotationPresent(Localized.class)) {
            this.localized = true;
        }

        buildConvertedBy();
    }

    @SuppressWarnings("unchecked")
    public void buildConvertedBy() {
        // do not do converting if there are no locator converters at all
        if (locatorConverters.isEmpty()) {
            return;
        }

        if (by.getClass().isAssignableFrom(ContentMappedBy.class)) {
            Map<ContentType, By> contentByMap;
            try {
                contentByMap = (Map<ContentType, By>) FieldUtils.readDeclaredField(by, "map", true);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e.getMessage());
            }
            for (By complexBy : contentByMap.values()) {
                By[] simpleByArray = parseComplexBy(complexBy);
                for (By simpleBy : simpleByArray) {
                    changeValueInBy(simpleBy);
                }
            }
        } else {
            By[] simpleByArray = parseComplexBy(by);
            for (By simpleBy : simpleByArray) {
                changeValueInBy(simpleBy);
            }
        }
    }

    private void changeValueInBy(By simpleBy) {
        try {
            String toConvert = simpleBy.toString();
            for (LocatorConverter converter : locatorConverters) {
                toConvert = converter.convert(toConvert);
            }

            //if convert where executed
            if (!toConvert.equals(simpleBy.toString())) {
                //overriding only locator
                toConvert = toConvert.substring(toConvert.indexOf(':') + 1).trim();

                if (AppiumBy.class.isAssignableFrom(simpleBy.getClass())) {
                    By.Remotable.Parameters parameters = (By.Remotable.Parameters) FieldUtils.readField(simpleBy, "remoteParameters", true);
                    FieldUtils.writeField(parameters, "value", toConvert, true);
                } else {
                    Field fieldBy = simpleBy.getClass().getDeclaredFields()[0];
                    FieldUtils.writeField(simpleBy, fieldBy.getName(), toConvert, true);
                    By.Remotable.Parameters parameters;
                    try {
                        parameters = (By.Remotable.Parameters) FieldUtils.readField(simpleBy, "params", true);
                    } catch (IllegalArgumentException | IllegalAccessException ex) {
                        parameters = (By.Remotable.Parameters) FieldUtils.readField(simpleBy, "remoteParams", true);
                    }
                    FieldUtils.writeField(parameters, "value", toConvert, true);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private By[] parseComplexBy(By by) {
        By[] bys = null;
        Class<? extends By> complexByClass = by.getClass();
        //ByIdOrName, ByAll, ByChained, By (and maybe carina's ByAny)
        try {
            if (ByIdOrName.class.isAssignableFrom(complexByClass)) {
                //By.Id + By.Name = 2
                bys = new By[2];
                bys[0] = (By) FieldUtils.readDeclaredField(by, "idFinder", true);
                bys[1] = (By) FieldUtils.readDeclaredField(by, "nameFinder", true);
            } else if (ByAll.class.isAssignableFrom(complexByClass) ||
                    ByChained.class.isAssignableFrom(complexByClass) ||
                    ByAny.class.isAssignableFrom(complexByClass)) {
                bys = (By[]) FieldUtils.readDeclaredField(by, "bys", true);
            } else {
                bys = new By[]{by};
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        List<By> byList = new ArrayList<>();
        for (By possiblySimpleBy : bys) {
            if (ByIdOrName.class.isAssignableFrom(possiblySimpleBy.getClass()) ||
                    ByAll.class.isAssignableFrom(possiblySimpleBy.getClass()) ||
                    ByChained.class.isAssignableFrom(possiblySimpleBy.getClass()) ||
                    ByAny.class.isAssignableFrom(possiblySimpleBy.getClass())) {
                By[] recursiveBys = parseComplexBy(possiblySimpleBy);
                byList.addAll(List.of(recursiveBys));
            } else {
                byList.add(possiblySimpleBy);
            }
        }

        return byList.toArray(new By[0]);
    }

    /**
     * From io.appium.java_client.pagefactory.AppiumElementLocator
     * This methods makes sets some settings of the {@link By} according to
     * the given instance of {@link SearchContext}. If there is some {@link ContentMappedBy}
     * then it is switched to the searching for some html or native mobile element.
     * Otherwise nothing happens there.
     *
     * @param currentBy is some locator strategy
     * @param currentContent is an instance of some subclass of the {@link SearchContext}.
     * @return the corrected {@link By} for the further searching
     *
     */
    private By getBy(By currentBy, SearchContext currentContent) {
        if (!ContentMappedBy.class.isAssignableFrom(currentBy.getClass())) {
            return currentBy;
        }

        return ContentMappedBy.class.cast(currentBy)
                .useContent(getCurrentContentType(currentContent));
    }

    /**
     * Find the element.
     */
    public WebElement findElement() {

        if (by == null) {
            throw new NullPointerException("By cannot be null");
        }

        //TODO: test how findElements work for web and android
        // maybe migrate to the latest appium java driver and reuse original findElement!
        List<WebElement> elements = searchContext.findElements(getBy(by, searchContext));

        WebElement element = null;
        if (elements.size() == 1) {
            element = elements.get(0);
        } else if (elements.size() > 1) {
            element = elements.get(0);
            LOGGER.debug("{} elements detected by: {}", elements.size(), by.toString());
        }

        if (element == null) {
            throw new NoSuchElementException(SpecialKeywords.NO_SUCH_ELEMENT_ERROR + by);
        }
        return element;
    }

    /**
     * Find the element list.
     */
    public List<WebElement> findElements() {
        List<WebElement> elements = null;

        try {
            elements = searchContext.findElements(getBy(by, searchContext));
        } catch (NoSuchElementException e) {
            LOGGER.debug("Unable to find elements: " + e.getMessage());
        }

        if (elements == null) {
            throw new NoSuchElementException(SpecialKeywords.NO_SUCH_ELEMENT_ERROR + by.toString());
        }

        return elements;
    }

    public SearchContext getSearchContext() {
        return this.searchContext;
    }

    public void setSearchContext(SearchContext searchContext) {
        this.searchContext = searchContext;
    }

    public boolean isLocalized() {
        return this.localized;
    }

    public boolean isCaseInsensitive() {
        return this.caseInsensitive;
    }

    public By getBy() {
        return this.by;
    }

    public WebDriver getDriver() {
        return this.driver;
    }

    public String getClassName() {
        return className;
    }

    public LinkedList<LocatorConverter> getLocatorConverters() {
        return this.locatorConverters;
    }
}
