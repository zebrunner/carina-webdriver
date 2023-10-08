package com.zebrunner.carina.webdriver.helper;

import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.messager.Messager;
import com.zebrunner.carina.utils.retry.ActionPoller;
import com.zebrunner.carina.webdriver.IDriverPool;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.decorator.annotations.CaseInsensitiveXPath;
import com.zebrunner.carina.webdriver.decorator.annotations.Localized;
import com.zebrunner.carina.webdriver.listener.DriverListener;
import com.zebrunner.carina.webdriver.locator.LocatorType;
import com.zebrunner.carina.webdriver.locator.converter.FormatLocatorConverter;
import com.zebrunner.carina.webdriver.locator.converter.LocalizeLocatorConverter;
import com.zebrunner.carina.webdriver.locator.converter.LocatorConverter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Sleeper;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public interface IExtendedWebElementHelper extends IDriverPool {
    Logger I_EXTENDED_WEB_ELEMENT_LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    Map<String, LinkedList<LocatorConverter>> LOCATOR_CONVERTERS = new ConcurrentHashMap<>();
    Map<String, By> ORIGINAL_LOCATORS = new ConcurrentHashMap<>();
    Duration DEFAULT_EXPLICIT_TIMEOUT = Duration.ofSeconds(Configuration.getRequired(WebDriverConfiguration.Parameter.EXPLICIT_TIMEOUT, Long.class));
    Duration SHORT_TIMEOUT = Duration.ofSeconds(Configuration.getRequired(WebDriverConfiguration.Parameter.EXPLICIT_TIMEOUT, Long.class) / 3);
    Duration DEFAULT_RETRY_INTERVAL = Duration.ofMillis(Configuration.getRequired(WebDriverConfiguration.Parameter.RETRY_INTERVAL, Long.class));

    /**
     * Method which quickly looks for all element and check that they present
     * during EXPLICIT_TIMEOUT
     *
     * @param elements ExtendedWebElement...
     * @return boolean return true only if all elements present.
     */
    default <T extends ExtendedWebElement> boolean allElementsPresent(T... elements) {
        return allElementsPresent(getWaitTimeout(), elements);
    }

    /**
     * Method which quickly looks for all element and check that they present
     * during EXPLICIT_TIMEOUT
     *
     * @param timeout long
     * @param elements ExtendedWebElement...
     * @return boolean return true only if all elements present.
     */
    default <T extends ExtendedWebElement> boolean allElementsPresent(long timeout, T... elements) {
        return allElementsPresent(Duration.ofSeconds(timeout), elements);
    }

    /**
     * Method which quickly looks for all element and check that they present
     * during timeout sec
     *
     * @param timeout {@link Duration}
     * @param elements ExtendedWebElement...
     * @return boolean return true only if all elements present.
     */
    default <T extends ExtendedWebElement> boolean allElementsPresent(Duration timeout, T... elements) {
        int index = 0;
        boolean present = true;
        boolean ret = true;
        int counts = 1;
        timeout = Duration.ofSeconds(timeout.toSeconds() / counts);
        if (timeout.toSeconds() < 1)
            timeout = Duration.ofSeconds(1);
        while (present && index++ < counts) {
            for (T element : elements) {
                present = element.isElementPresent(timeout);
                if (!present) {
                    I_EXTENDED_WEB_ELEMENT_LOGGER.error("{} is not present.", element.getNameWithLocator());
                    ret = false;
                }
            }
        }
        return ret;
    }

    /**
     * Method which quickly looks for all element lists and check that they
     * contain at least one element during SHORT_TIMEOUT
     *
     * @param elements List&lt;ExtendedWebElement&gt;...
     * @return boolean
     */
    @SuppressWarnings("unchecked")
    default <T extends ExtendedWebElement> boolean allElementListsAreNotEmpty(List<T>... elements) {
        return allElementListsAreNotEmpty(SHORT_TIMEOUT, elements);
    }

    /**
     * Method which quickly looks for all element lists and check that they
     * contain at least one element during timeout
     *
     * @param timeout timeout, in seconds
     * @param elements List&lt;ExtendedWebElement&gt;...
     * @return boolean return true only if All Element lists contain at least one element
     */
    default <T extends ExtendedWebElement> boolean allElementListsAreNotEmpty(long timeout, List<T>... elements) {
        return allElementListsAreNotEmpty(Duration.ofSeconds(timeout), elements);
    }

    /**
     * Method which quickly looks for all element lists and check that they
     * contain at least one element during timeout
     *
     * @param timeout {@link Duration}
     * @param elements List&lt;ExtendedWebElement&gt;...
     * @return boolean return true only if All Element lists contain at least one element
     */
    @SuppressWarnings("unchecked")
    default <T extends ExtendedWebElement> boolean allElementListsAreNotEmpty(Duration timeout, List<T>... elements) {
        boolean ret;
        int counts = 3;
        timeout = Duration.ofSeconds(timeout.toSeconds() / counts);
        if (timeout.toSeconds() < 1)
            timeout = Duration.ofSeconds(1);
        for (int i = 0; i < elements.length; i++) {
            boolean present = false;
            int index = 0;
            while (!present && index++ < counts) {
                try {
                    present = elements[i].get(0).isElementPresent(timeout);
                } catch (Exception e) {
                    // do nothing
                }
            }
            ret = (!elements[i].isEmpty());
            if (!ret) {
                String elementsAsString = Arrays.toString(elements);
                I_EXTENDED_WEB_ELEMENT_LOGGER.error("List of elements[{}] from elements {} is empty.", i, elementsAsString);
                return false;
            }
        }
        return true;
    }

    /**
     * Method which quickly looks for any element presence during
     * SHORT_TIMEOUT
     *
     * @param elements ExtendedWebElement...
     * @return true if any of elements was found.
     */
    default <T extends ExtendedWebElement> boolean isAnyElementPresent(T... elements) {
        return isAnyElementPresent(SHORT_TIMEOUT, elements);
    }

    /**
     * Method which quickly looks for any element presence during timeout sec
     *
     * @param timeout timeout, in seconds
     * @param elements ExtendedWebElement...
     * @return true if any of elements was found.
     */
    default <T extends ExtendedWebElement> boolean isAnyElementPresent(long timeout, T... elements) {
        return isAnyElementPresent(Duration.ofSeconds(timeout), elements);
    }

    /**
     * Method which quickly looks for any element presence during timeout sec
     *
     * @param timeout {@link Duration}
     * @param elements ExtendedWebElement...
     * @return true if any of elements was found.
     */
    default <T extends ExtendedWebElement> boolean isAnyElementPresent(Duration timeout, T... elements) {
        int index = 0;
        int counts = 10;
        timeout = Duration.ofSeconds(timeout.toSeconds() / counts);
        if (timeout.toSeconds() < 1)
            timeout = Duration.ofSeconds(1);
        while (index++ < counts) {
            for (T element : elements) {
                if (element.isElementPresent(timeout)) {
                    I_EXTENDED_WEB_ELEMENT_LOGGER.debug("{} is present", element.getNameWithLocator());
                    return true;
                }
            }
        }
        I_EXTENDED_WEB_ELEMENT_LOGGER.error("Unable to find any element from array: {}", Arrays.toString(elements));
        return false;
    }

    /**
     * return Any Present Element from the list which present during
     * SHORT_TIMEOUT
     *
     * @param elements ExtendedWebElement...
     * @return ExtendedWebElement
     */
    default <T extends ExtendedWebElement> T returnAnyPresentElement(T... elements) {
        return returnAnyPresentElement(SHORT_TIMEOUT, elements);
    }

    /**
     * return Any Present Element from the list which present during
     * SHORT_TIMEOUT
     *
     * @param timeout timeout, in seconds
     * @param elements ExtendedWebElement...
     * @return ExtendedWebElement
     */
    default <T extends ExtendedWebElement> T returnAnyPresentElement(long timeout, T... elements) {
        return returnAnyPresentElement(Duration.ofSeconds(timeout), elements);
    }

    /**
     * return Any Present Element from the list which present during timeout sec
     *
     * @param timeout {@link Duration}
     * @param elements ExtendedWebElement...
     * @return ExtendedWebElement
     */
    default <T extends ExtendedWebElement> T returnAnyPresentElement(Duration timeout, T... elements) {
        int index = 0;
        int counts = 10;
        timeout = Duration.ofSeconds(timeout.toSeconds() / counts);
        if (timeout.toSeconds() < 1)
            timeout = Duration.ofSeconds(1);
        while (index++ < counts) {
            for (T element : elements) {
                if (element.isElementPresent(timeout)) {
                    I_EXTENDED_WEB_ELEMENT_LOGGER.debug(element.getNameWithLocator() + " is present");
                    return element;
                }
            }
        }
        // throw exception anyway if nothing was returned inside for cycle
        I_EXTENDED_WEB_ELEMENT_LOGGER.error("All elements are not present");
        throw new RuntimeException("Unable to find any element from array: " + Arrays.toString(elements));
    }

    /**
     * Check that element with text present.
     *
     * @param extWebElement to check if element with text is present
     * @param text of element to check.
     * @return element with text existence status.
     */
    default <T extends ExtendedWebElement> boolean isElementWithTextPresent(final T extWebElement, final String text) {
        return isElementWithTextPresent(extWebElement, text, getWaitTimeout());
    }

    /**
     * Check that element with text present.
     *
     * @param extWebElement to check if element with text is present
     * @param text of element to check.
     * @param timeout Long
     * @return element with text existence status.
     */
    default <T extends ExtendedWebElement> boolean isElementWithTextPresent(final T extWebElement, final String text, long timeout) {
        return isElementWithTextPresent(extWebElement, text, Duration.ofSeconds(timeout));
    }

    /**
     * Check that element with text present.
     *
     * @param extWebElement to check if element with text is present
     * @param text of element to check.
     * @param timeout {@link Duration}
     * @return element with text existence status.
     */
    default <T extends ExtendedWebElement> boolean isElementWithTextPresent(final T extWebElement, final String text, Duration timeout) {
        return extWebElement.isElementWithTextPresent(text, timeout);
    }

    /**
     * Check that element not present on page.
     *
     * @param extWebElement to check if element is not present
     * @return element non-existence status.
     */
    default <T extends ExtendedWebElement> boolean isElementNotPresent(final T extWebElement) {
        return isElementNotPresent(extWebElement, getWaitTimeout());
    }

    /**
     * Check that element not present on page.
     *
     * @param extWebElement to check if element is not present
     * @param timeout to wait
     * @return element non-existence status.
     */
    default <T extends ExtendedWebElement> boolean isElementNotPresent(final T extWebElement, long timeout) {
        return isElementNotPresent(extWebElement, Duration.ofSeconds(timeout));
    }

    /**
     * Check that element not present on page.
     *
     * @param extWebElement to check if element is not present
     * @param timeout to wait
     * @return element non-existence status.
     */
    default <T extends ExtendedWebElement> boolean isElementNotPresent(T extWebElement, Duration timeout) {
        return extWebElement.isElementNotPresent(timeout.toSeconds());
    }

    /**
     * Clicks on element.
     *
     * @param elements ExtendedWebElements to click
     */
    default <T extends ExtendedWebElement> void clickAny(T... elements) {
        clickAny(getWaitTimeout(), elements);
    }

    /**
     * Clicks on element.
     *
     * @param elements ExtendedWebElements to click
     * @param timeout to wait
     */
    default <T extends ExtendedWebElement> void clickAny(long timeout, T... elements) {
        clickAny(Duration.ofSeconds(timeout), elements);
    }

    /**
     * Clicks on element.
     *
     * @param elements ExtendedWebElements to click
     * @param timeout to wait
     */
    default <T extends ExtendedWebElement> void clickAny(Duration timeout, T... elements) {
        // Method which quickly looks for any element and click during timeout
        // sec
        WebDriver drv = getDriver();
        ActionPoller<WebElement> actionPoller = ActionPoller.builder();

        Optional<WebElement> searchableElement = actionPoller.task(() -> {
            WebElement possiblyFoundElement = null;

            for (ExtendedWebElement element : elements) {
                List<WebElement> foundElements = drv.findElements(element.getLocator().orElseThrow());
                if (!foundElements.isEmpty()) {
                    possiblyFoundElement = foundElements.get(0);
                    break;
                }
            }
            return possiblyFoundElement;
        })
                .until(Objects::nonNull)
                .pollEvery(0, ChronoUnit.SECONDS)
                .stopAfter(timeout.toSeconds(), ChronoUnit.SECONDS)
                .execute();

        if (searchableElement.isEmpty()) {
            throw new RuntimeException(String.format("Unable to click onto any elements from array: %s", Arrays.toString(elements)));
        }
        searchableElement.get()
                .click();
    }

    /**
     * Find element on the page<br>
     *
     * @param by see {@link By}
     * @return {@link ExtendedWebElement} if exists, {@code null} otherwise
     */
    default ExtendedWebElement findExtendedWebElement(By by) {
        return findExtendedWebElement(by, by.toString(), getWaitTimeout().toSeconds());
    }

    /**
     * Find element on the page
     *
     * @param by see {@link By}
     * @param timeout time to wait, in seconds
     * @return {@link ExtendedWebElement} if exists, {@code null} otherwise
     */
    default ExtendedWebElement findExtendedWebElement(By by, long timeout) {
        return findExtendedWebElement(by, by.toString(), timeout);
    }

    /**
     * Find element on the page<br>
     *
     * @param by see {@link By}
     * @param name the name that will be given to the found element
     * @return {@link ExtendedWebElement} if exists, {@code null} otherwise
     */
    default ExtendedWebElement findExtendedWebElement(final By by, String name) {
        return findExtendedWebElement(by, name, getWaitTimeout().toSeconds());
    }

    /**
     * Find element on the page
     *
     * @param by see {@link By}
     * @param name the name that will be given to the found element
     * @param timeout time to wait, in seconds
     * @return {@link ExtendedWebElement} if exists, {@code null} otherwise
     */
    default ExtendedWebElement findExtendedWebElement(final By by, String name, long timeout) {
        DriverListener.setMessages(Messager.ELEMENT_FOUND.getMessage(name), Messager.ELEMENT_NOT_FOUND.getMessage(name));

        if (!waitUntil(ExpectedConditions.presenceOfElementLocated(by), timeout)) {
            Messager.ELEMENT_NOT_FOUND.error(name);
            return null;
        }
        ExtendedWebElement element = new ExtendedWebElement(getDriver(), getDriver());
        element.setLocator(by);
        element.setName(name);
        return element;
    }

    /**
     * Find elements on the page<br>
     *
     * @param by see {@link By}
     * @return list of {@link ExtendedWebElement}s, empty list otherwise
     */
    default List<ExtendedWebElement> findExtendedWebElements(By by) {
        return findExtendedWebElements(by, getWaitTimeout().toSeconds());
    }

    /**
     * Find elements on the page
     *
     * @param by see {@link By}
     * @param timeout time to wait, in seconds
     * @return list of {@link ExtendedWebElement}s if found, empty list otherwise
     */
    default List<ExtendedWebElement> findExtendedWebElements(final By by, long timeout) {
        List<ExtendedWebElement> extendedWebElements = new ArrayList<>();
        if (!waitUntil(ExpectedConditions.presenceOfElementLocated(by), timeout)) {
            Messager.ELEMENT_NOT_FOUND.info(by.toString());
            return extendedWebElements;
        }

        int i = 0;
        for (WebElement webElement : getDriver().findElements(by)) {
            ExtendedWebElement foundElement = new ExtendedWebElement(getDriver(), getDriver());
            foundElement.setName(String.format("ExtendedWebElement - [%d]", i));
            foundElement.setWebElement(webElement);
            extendedWebElements.add(foundElement);
            i++;
        }
        return extendedWebElements;
    }

    /**
     * Find nested element
     *
     * @param extendedElement {@link ExtendedWebElement}
     * @param by {@link By}
     * @return {@link ExtendedWebElement} if exists, {@code null} otherwise
     */
    default <T extends ExtendedWebElement> T findExtendedWebElement(T extendedElement, By by) {
        return findExtendedWebElement(extendedElement, by, getWaitTimeout());
    }

    /**
     * Find nested element
     *
     * @param extendedElement {@link ExtendedWebElement}
     * @param by see {@link By}
     * @param timeout {@link Duration}
     * @return {@link ExtendedWebElement} if exists, {@code null} otherwise
     */
    default <T extends ExtendedWebElement> T findExtendedWebElement(T extendedElement, By by, Duration timeout) {
        return findExtendedWebElement(extendedElement, by, extendedElement.getName(), timeout);
    }

    /**
     * Find nested element
     *
     * @param extendedElement {@link ExtendedWebElement}
     * @param by see {@link By}
     * @param name the name that will be given to the found element
     * @return {@link ExtendedWebElement} if exists, {@code null} otherwise
     */
    default <T extends ExtendedWebElement> T findExtendedWebElement(T extendedElement, final By by, String name) {
        return findExtendedWebElement(extendedElement, by, name, getWaitTimeout());
    }

    /**
     * Find nested element
     *
     * @param extendedElement {@link ExtendedWebElement}
     * @param by see {@link By}
     * @param name the name that will be given to the found element
     * @param timeout {@link Duration}
     * @return {@link ExtendedWebElement} if exists, {@code null} otherwise
     */
    @SuppressWarnings("unchecked")
    default <T extends ExtendedWebElement> T findExtendedWebElement(T extendedElement, final By by, String name, Duration timeout) {
        DriverListener.setMessages(Messager.ELEMENT_FOUND.getMessage(name), Messager.ELEMENT_NOT_FOUND.getMessage(name));
        if (!waitUntil(ExpectedConditions.presenceOfElementLocated(Objects.requireNonNull(by)), timeout)) {
            Messager.ELEMENT_NOT_FOUND.error(name);
            return null;
        }
        try {
            T foundElement = (T) ConstructorUtils.invokeConstructor(extendedElement.getClass(), extendedElement.getDriver(), extendedElement);
            foundElement.setLocator(by);
            foundElement.setName(name);
            return foundElement;
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Find nested elements
     *
     * @param extendedElement {@link ExtendedWebElement}
     * @param by see {@link By}
     * @return list of {@link ExtendedWebElement}s, empty list otherwise
     */
    @SuppressWarnings("unused")
    default <T extends ExtendedWebElement> List<T> findExtendedWebElements(T extendedElement, By by) {
        return findExtendedWebElements(extendedElement, by, getWaitTimeout());
    }

    /**
     * Find nested elements
     *
     * @param extendedElement {@link ExtendedWebElement}
     * @param by see {@link By}
     * @param timeout time to wait, in seconds
     * @return list of {@link ExtendedWebElement}s if found, empty list otherwise
     */
    @SuppressWarnings("unchecked")
    default <T extends ExtendedWebElement> List<T> findExtendedWebElements(T extendedElement, final By by, Duration timeout) {
        try {
            List<T> extendedWebElements = new ArrayList<>();
            if (!waitUntil(ExpectedConditions.presenceOfElementLocated(by), timeout)) {
                Messager.ELEMENT_NOT_FOUND.info(by.toString());
                return extendedWebElements;
            }

            int i = 0;
            for (WebElement webElement : extendedElement.findElements(by)) {
                T foundElement = (T) ConstructorUtils.invokeConstructor(extendedElement.getClass(), extendedElement.getDriver(), extendedElement);
                foundElement.setName(String.format("ExtendedWebElement - [%d]", i));
                foundElement.setWebElement(webElement);
                extendedWebElements.add(foundElement);
                i++;
            }
            return extendedWebElements;
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Wait until any condition happens.
     *
     * @param condition {@link ExpectedCondition}
     * @param timeout {@link Duration}
     * @return true if condition happen
     */
    default boolean waitUntil(ExpectedCondition<?> condition, long timeout) {
        return waitUntil(condition, Duration.ofSeconds(timeout));
    }

    /**
     * Wait until any condition happens.
     *
     * @param condition {@link ExpectedCondition}
     * @param timeout {@link Duration}
     * @return true if condition happen
     */
    default boolean waitUntil(ExpectedCondition<?> condition, Duration timeout) {
        if (timeout.toSeconds() < 1) {
            I_EXTENDED_WEB_ELEMENT_LOGGER.warn("Fluent wait less than 1sec timeout might hangs! Updating to 1 sec.");
            timeout = Duration.ofSeconds(1);
        }

        // try to use better tickMillis clock
        Wait<WebDriver> wait = new WebDriverWait(getDriver(), timeout, getWaitInterval(timeout),
                java.time.Clock.tickMillis(java.time.ZoneId.systemDefault()), Sleeper.SYSTEM_SLEEPER)
                        .withTimeout(timeout);

        // [VD] Notes:
        // do not ignore TimeoutException or NoSuchSessionException otherwise you can wait for minutes instead of timeout!
        // [VD] note about NoSuchSessionException is pretty strange. Let's ignore here and return false only in case of
        // TimeoutException putting details into the debug log message. All the rest shouldn't be ignored

        // 7.3.17-SNAPSHOT. Removed NoSuchSessionException (Mar-11-2022)
        // .ignoring(NoSuchSessionException.class) // why do we ignore noSuchSession? Just to minimize errors?

        // 7.3.20.1686-SNAPSHOT. Removed ignoring WebDriverException (Jun-03-2022).
        // Goal to test if inside timeout happens first and remove interruption and future call
        // removed ".ignoring(NoSuchElementException.class);" as NotFoundException ignored by waiter itself
        // added explicit .withTimeout(Duration.ofSeconds(timeout));

        I_EXTENDED_WEB_ELEMENT_LOGGER.debug("waitUntil: starting... timeout: {}", timeout);
        boolean res = false;
        try {
            wait.until(condition);
            res = true;
        } catch (TimeoutException e) {
            I_EXTENDED_WEB_ELEMENT_LOGGER.debug("waitUntil: org.openqa.selenium.TimeoutException", e);
        } finally {
            I_EXTENDED_WEB_ELEMENT_LOGGER.debug("waiter is finished. conditions: {}", condition);
        }
        return res;
    }

    /**
     * <b>For internal usage only</b>
     */
    default By buildConvertedBy(By originalBy, List<LocatorConverter> converters) {
        // do not do converting if there are no locator converters at all
        if (converters.isEmpty()) {
            return originalBy;
        }
        String byAsString = originalBy.toString();
        for (LocatorConverter converter : converters) {
            byAsString = converter.convert(byAsString);
        }

        String finalByAsString = byAsString;
        return Arrays.stream(LocatorType.values())
                .filter(locatorType -> locatorType.is(finalByAsString))
                .findFirst()
                .orElseThrow()
                .buildLocatorFromString(byAsString);
    }

    /**
     * Get element with formatted locator.<br>
     * <p>
     * 1. If element created using {@link org.openqa.selenium.support.FindBy} or same annotations:<br>
     * If parameters were passed to the method, the element will be recreated with a new locator,
     * and if the format method with parameters was already called for this element, the element locator
     * will be recreated based on the original.<br>
     * <b>All original element statuses {@link CaseInsensitiveXPath},
     * {@link Localized} are saved for new element</b>
     *
     * <p>
     * 2. If element created using constructor (it is not recommended to create element by hands):<br>
     * If parameters were passed to the method, the element will be recreated with a new locator.
     *
     * <p>
     * For all cases: if the method is called with no parameters, no locator formatting is applied, but the element will be "recreated".<br>
     *
     * <b>This method does not change the object on which it is called</b>.
     *
     * @param extendedElement {@link ExtendedWebElement}
     * @return new {@link ExtendedWebElement} with formatted locator
     */
    default <T extends ExtendedWebElement> T format(T extendedElement, Object... objects) {
        T formatElement = ObjectUtils.clone(extendedElement);
        if (Arrays.stream(objects).findAny().isEmpty()) {
            return extendedElement;
        }
        LinkedList<LocatorConverter> converters = new LinkedList<>(Optional.ofNullable(LOCATOR_CONVERTERS.get(extendedElement.getUuid()))
                .orElse(new LinkedList<>()));
        boolean isTextContainsL10N = Arrays.stream(objects)
                .map(String::valueOf)
                .anyMatch(text -> LocalizeLocatorConverter.getL10nPattern().matcher(text).find());
        if (isTextContainsL10N) {
            LocalizeLocatorConverter converter = new LocalizeLocatorConverter();
            converters.addFirst(converter);
        }
        FormatLocatorConverter converter = new FormatLocatorConverter(objects);
        converters.addFirst(converter);
        formatElement.setLocator(buildConvertedBy(ORIGINAL_LOCATORS.get(extendedElement.getUuid()), converters));
        return formatElement;
    }

    /**
     * Get list of elements with formatted locator.<br>
     *
     * <p>
     * 1. If element created using {@link org.openqa.selenium.support.FindBy} or same annotations:<br>
     * If parameters were passed to the method, the result elements will be created with a new locator,
     * and if the format method with parameters was already called for this element, the element locator
     * will be recreated based on the original.<br>
     * <br>
     * <b>All original element statuses {@link CaseInsensitiveXPath},
     * {@link Localized} are saved for new elements</b>
     *
     * <p>
     * 2. If element created using constructor (it is not recommended to create element by hands):<br>
     * If parameters were passed to the method, the result elements will be created with a new locator.
     * For all cases: if the method is called with no parameters, no locator formatting is applied, but elements will be created using original
     * locator.<br>
     *
     * <b>This method does not change the object on which it is called</b>.
     *
     * @param extendedElement {@link ExtendedWebElement}
     * @param objects parameters
     * @return {@link List} of {@link ExtendedWebElement} if found, empty list otherwise
     */
    @SuppressWarnings("unchecked")
    default <T extends ExtendedWebElement> List<T> formatToList(T extendedElement, Object... objects) {
        try {
            List<T> extendedElements = new ArrayList<>();
            T tempExtendedElement = format(extendedElement, objects);
            int index = 0;
            for (WebElement element : extendedElement.findElements(tempExtendedElement.getLocator()
                    .orElseThrow(() -> new IllegalStateException("Element do not contains locator.")))) {
                T extendedElementOfList = (T) ConstructorUtils.invokeConstructor(extendedElement.getClass(), extendedElement.getDriver(),
                        extendedElement);
                extendedElementOfList.setWebElement(element);
                extendedElementOfList.setLocator(null);
                extendedElementOfList.setName(String.format("%s - [%s]", extendedElement.getName(), index++));
                extendedElements.add(extendedElementOfList);
            }
            return extendedElements;
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Get waiting timeout.
     *
     * @return {@link Duration}
     */
    default Duration getWaitTimeout() {
        return DEFAULT_EXPLICIT_TIMEOUT;
    }

    /**
     * Get waiting interval
     *
     * @param timeout timeout
     * @return {@link Duration}
     */
    default Duration getWaitInterval(Duration timeout) {
        Duration interval = DEFAULT_RETRY_INTERVAL;
        if (timeout.toSeconds() >= 3 && timeout.toSeconds() <= 10) {
            interval = Duration.ofMillis(500);
        }
        if (timeout.toSeconds() > 10) {
            interval = Duration.ofMillis(1000);
        }
        return interval;
    }
}
