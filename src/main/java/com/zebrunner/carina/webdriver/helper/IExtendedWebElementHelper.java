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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
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

@SuppressWarnings({ "unused", "unchecked" })
public interface IExtendedWebElementHelper extends IDriverPool, IWaitHelper {
    Logger I_EXTENDED_WEB_ELEMENT_LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @SuppressWarnings("squid:S2386")
    Map<String, LinkedList<LocatorConverter>> LOCATOR_CONVERTERS = new ConcurrentHashMap<>();
    @SuppressWarnings("squid:S2386")
    Map<String, By> ORIGINAL_LOCATORS = new ConcurrentHashMap<>();
    Duration SHORT_TIMEOUT = Duration.ofSeconds(Configuration.getRequired(WebDriverConfiguration.Parameter.EXPLICIT_TIMEOUT, Long.class) / 3);

    /**
     * Method which quickly looks for all element and check that they present
     * during EXPLICIT_TIMEOUT
     *
     * @param elements ExtendedWebElement...
     * @return boolean return true only if all elements present.
     */
    default <T extends ExtendedWebElement> boolean allElementsPresent(final T... elements) {
        return allElementsPresent(getDefaultWaitTimeout(), elements);
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
                    I_EXTENDED_WEB_ELEMENT_LOGGER.debug("{} is present", element.getNameWithLocator());
                    return element;
                }
            }
        }
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
        return isElementWithTextPresent(extWebElement, text, getDefaultWaitTimeout());
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
        return isElementNotPresent(extWebElement, getDefaultWaitTimeout());
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
        clickAny(getDefaultWaitTimeout(), elements);
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
                List<WebElement> foundElements = drv.findElements(element.getBy());
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
        return findExtendedWebElement(by, by.toString(), getDefaultWaitTimeout().toSeconds());
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
        return findExtendedWebElement(by, name, getDefaultWaitTimeout().toSeconds());
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
        element.setBy(by);
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
        return findExtendedWebElements(by, getDefaultWaitTimeout().toSeconds());
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
            foundElement.setElement(webElement);
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
        return findExtendedWebElement(extendedElement, by, getDefaultWaitTimeout());
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
        return findExtendedWebElement(extendedElement, by, name, getDefaultWaitTimeout());
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
        if (!waitUntil(ExpectedConditions.presenceOfNestedElementLocatedBy(extendedElement.getElement(), Objects.requireNonNull(by)), timeout)) {
            Messager.ELEMENT_NOT_FOUND.error(name);
            return null;
        }
        try {
            T foundElement;
            if (ConstructorUtils.getAccessibleConstructor(extendedElement.getClass(), WebDriver.class, SearchContext.class) != null) {
                foundElement = (T) ConstructorUtils.invokeConstructor(extendedElement.getClass(),
                        new Object[] { extendedElement.getDriver(), extendedElement },
                        new Class<?>[] { WebDriver.class, SearchContext.class });
            } else if (ConstructorUtils.getAccessibleConstructor(extendedElement.getClass(), WebDriver.class) != null) {
                foundElement = (T) ConstructorUtils.invokeConstructor(extendedElement.getClass(), new Object[] { extendedElement.getDriver() },
                        new Class<?>[] { WebDriver.class });
            }  else {
                throw new NoSuchMethodException(
                        String.format("Could not find suitable constructor (WebDriver) or (WebDriver, SearchContext) in '%s' class.", extendedElement.getClass()));
            }
            foundElement.setBy(by);
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
        return findExtendedWebElements(extendedElement, by, getDefaultWaitTimeout());
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
            if (!waitUntil(ExpectedConditions.presenceOfNestedElementLocatedBy(extendedElement.getElement(), Objects.requireNonNull(by)), timeout)) {
                Messager.ELEMENT_NOT_FOUND.info(by.toString());
                return extendedWebElements;
            }

            int i = 0;
            for (WebElement webElement : extendedElement.findElements(by)) {
                T foundElement;
                if (ConstructorUtils.getAccessibleConstructor(extendedElement.getClass(), WebDriver.class, SearchContext.class) != null) {
                    foundElement = (T) ConstructorUtils.invokeConstructor(extendedElement.getClass(),
                            new Object[] { extendedElement.getDriver(), extendedElement },
                            new Class<?>[] { WebDriver.class, SearchContext.class });
                } else if (ConstructorUtils.getAccessibleConstructor(extendedElement.getClass(), WebDriver.class) != null) {
                    foundElement = (T) ConstructorUtils.invokeConstructor(extendedElement.getClass(), new Object[] { extendedElement.getDriver() },
                            new Class<?>[] { WebDriver.class });
                }  else {
                    throw new NoSuchMethodException(
                            String.format("Could not find suitable constructor (WebDriver) or (WebDriver, SearchContext) in '%s' class.", extendedElement.getClass()));
                }
                foundElement.setName(String.format("ExtendedWebElement - [%d]", i));
                foundElement.setElement(webElement);
                extendedWebElements.add(foundElement);
                i++;
            }
            return extendedWebElements;
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            return ExceptionUtils.rethrow(e);
        }
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
                .orElseGet(LinkedList::new));
        boolean isTextContainsL10N = Arrays.stream(objects)
                .map(String::valueOf)
                .anyMatch(text -> LocalizeLocatorConverter.getL10nPattern().matcher(text).find());
        if (isTextContainsL10N) {
            LocalizeLocatorConverter converter = new LocalizeLocatorConverter();
            converters.addFirst(converter);
        }
        FormatLocatorConverter converter = new FormatLocatorConverter(objects);
        converters.addFirst(converter);
        By originalBy = ORIGINAL_LOCATORS.get(extendedElement.getUuid()) != null ?
                ORIGINAL_LOCATORS.get(extendedElement.getUuid()) : extendedElement.getBy();
        formatElement.setBy(buildConvertedBy(originalBy, converters));
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
            for (WebElement element : extendedElement.getSearchContext()
                    .findElements(Objects.requireNonNull(tempExtendedElement.getBy()))) {
                T extendedElementOfList = (T) ConstructorUtils.invokeConstructor(extendedElement.getClass(), extendedElement.getDriver(),
                        extendedElement.getSearchContext());
                extendedElementOfList.setElement(element);
                extendedElementOfList.setBy(null);
                extendedElementOfList.setName(String.format("%s - [%s]", extendedElement.getName(), index++));
                extendedElements.add(extendedElementOfList);
            }
            return extendedElements;
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    default <T extends ExtendedWebElement> void assertElementPresent(final T extWebElement) {
        assertElementPresent(extWebElement, getDefaultWaitTimeout().toSeconds());
    }

    default <T extends ExtendedWebElement> void assertElementPresent(final T extWebElement, long timeout) {
        extWebElement.assertElementPresent(timeout);
    }

    default <T extends ExtendedWebElement> void assertElementWithTextPresent(final T extWebElement, final String text) {
        assertElementWithTextPresent(extWebElement, text, getDefaultWaitTimeout().toSeconds());
    }

    default <T extends ExtendedWebElement> void assertElementWithTextPresent(final T extWebElement, final String text, long timeout) {
        extWebElement.assertElementWithTextPresent(text, timeout);
    }

    /**
     * Executes a script on an element
     * <p>
     * Really should only be used when the web driver is sucking at exposing
     * functionality natively
     *
     * @param script The script to execute
     * @param element The target of the script, referenced as arguments[0]
     * @return Object
     */
    default Object trigger(String script, WebElement element) {
        return ((JavascriptExecutor) getDriver()).executeScript(script, element);
    }

    /**
     * Drags and drops element to specified place.
     *
     * @param from element to drag.
     * @param to element to drop to.
     */
    default void dragAndDropElement(final ExtendedWebElement from, final ExtendedWebElement to) {
        if (from.isElementPresent() && to.isElementPresent()) {
            WebDriver drv = getDriver();
            if (!drv.toString().contains("safari")) {
                Actions builder = new Actions(drv);
                Action dragAndDrop = builder.clickAndHold(from.getElement()).moveToElement(to.getElement())
                        .release(to.getElement()).build();
                dragAndDrop.perform();
            } else {
                WebElement locatorfrom = from.getElement();
                WebElement locatorTo = to.getElement();
                String xto = Integer.toString(locatorTo.getLocation().x);
                String yto = Integer.toString(locatorTo.getLocation().y);
                ((JavascriptExecutor) getDriver())
                        .executeScript(
                                "function simulate(f,c,d,e){var b,a=null;for(b in eventMatchers)if(eventMatchers[b].test(c)){a=b;break}if(!a)return!1;document.createEvent?(b=document.createEvent(a),a==\"HTMLEvents\"?b.initEvent(c,!0,!0):b.initMouseEvent(c,!0,!0,document.defaultView,0,d,e,d,e,!1,!1,!1,!1,0,null),f.dispatchEvent(b)):(a=document.createEventObject(),a.detail=0,a.screenX=d,a.screenY=e,a.clientX=d,a.clientY=e,a.ctrlKey=!1,a.altKey=!1,a.shiftKey=!1,a.metaKey=!1,a.button=1,f.fireEvent(\"on\"+c,a));return!0} var eventMatchers={HTMLEvents:/^(?:load|unload|abort|error|select|change|submit|reset|focus|blur|resize|scroll)$/,MouseEvents:/^(?:click|dblclick|mouse(?:down|up|over|move|out))$/}; "
                                        + "simulate(arguments[0],\"mousedown\",0,0); simulate(arguments[0],\"mousemove\",arguments[1],arguments[2]); simulate(arguments[0],\"mouseup\",arguments[1],arguments[2]); ",
                                locatorfrom, xto, yto);
            }
            Messager.ELEMENTS_DRAGGED_AND_DROPPED.info(from.getName(), to.getName());
        } else {
            Messager.ELEMENTS_NOT_DRAGGED_AND_DROPPED.error(from.getNameWithLocator(), to.getNameWithLocator());
        }
    }
}
