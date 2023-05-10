package com.zebrunner.carina.webdriver.locator;

import static io.appium.java_client.pagefactory.utils.WebDriverUnpackUtility.getCurrentContentType;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Optional;

import javax.annotation.Nullable;

import org.openqa.selenium.Beta;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.webdriver.decorator.annotations.CaseInsensitiveXPath;
import com.zebrunner.carina.webdriver.locator.converter.LocalizeLocatorConverter;
import com.zebrunner.carina.webdriver.locator.converter.LocatorConverter;
import com.zebrunner.carina.webdriver.locator.converter.caseinsensitive.CaseInsensitiveConverter;

import io.appium.java_client.pagefactory.bys.ContentType;

/**
 * For internal usage only
 */
@Beta
public final class LocatorUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private LocatorUtils() {

    }

    /**
     * Get type of locator
     * 
     * @param by {@link By}
     * @return {@link Optional} with {@link LocatorType} if locator type detected , {@link Optional#empty()} otherwise
     */
    public static Optional<LocatorType> getLocatorType(By by) {
        String locator = by.toString();
        Optional<LocatorType> locatorType = Arrays.stream(LocatorType.values())
                .filter(l -> l.is(locator))
                .findFirst();
        if (locatorType.isEmpty()) {
            LOGGER.debug("Cannot find suitable locator: '{}'. Investigate....", by);
        }
        return locatorType;
    }

    public static Optional<LocatorConverter> getL10NLocatorConverter(By by) {
        if (LocalizeLocatorConverter.getL10nPattern().matcher(by.toString()).find()) {
            return Optional.of(new LocalizeLocatorConverter());
        }
        return Optional.empty();
    }

    public static Optional<LocatorConverter> getCaseInsensitiveLocatorConverter(WebDriver driver, @Nullable CaseInsensitiveXPath annotation) {
        // [AS] do not try to use searchContext for getCurrentContentType method, because it may be a proxy and when we try to
        // get driver from it, there will be 'org.openqa.selenium.NoSuchElementException' because on this moment page is not opened,
        // so we just use driver instead
        if (annotation != null) {
            return Optional.of(new CaseInsensitiveConverter(annotation, ContentType.NATIVE_MOBILE_SPECIFIC.equals(getCurrentContentType(driver))));
        }
        return Optional.empty();
    }
}
