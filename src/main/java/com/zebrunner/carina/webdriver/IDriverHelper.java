package com.zebrunner.carina.webdriver;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.ScriptTimeoutException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.UnsupportedCommandException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.json.JsonException;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.decorators.Decorated;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Sleeper;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.zebrunner.carina.utils.Configuration;
import com.zebrunner.carina.utils.CryptoUtils;
import com.zebrunner.carina.utils.ICommonUtils;
import com.zebrunner.carina.utils.LogicUtils;
import com.zebrunner.carina.utils.messager.Messager;
import com.zebrunner.carina.utils.retry.ActionPoller;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.decorator.annotations.CaseInsensitiveXPath;
import com.zebrunner.carina.webdriver.decorator.annotations.Localized;
import com.zebrunner.carina.webdriver.gui.AbstractUIObject;
import com.zebrunner.carina.webdriver.listener.DriverListener;
import com.zebrunner.carina.webdriver.locator.converter.FormatLocatorConverter;
import com.zebrunner.carina.webdriver.locator.converter.LocalizeLocatorConverter;
import com.zebrunner.carina.webdriver.locator.converter.LocatorConverter;

/**
 * Provides common methods for interacting with the page
 */
public interface IDriverHelper extends IDriverPool, ICommonUtils {

    Logger I_DRIVER_HELPER_LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    long EXPLICIT_TIMEOUT = Configuration.getLong(Configuration.Parameter.EXPLICIT_TIMEOUT);
    long RETRY_TIME = Configuration.getLong(Configuration.Parameter.RETRY_INTERVAL);

    // --------------------------------------------------------------------------
    // Base UI interaction operations
    // --------------------------------------------------------------------------

    /**
     * Method which quickly looks for all element and check that they present
     * during EXPLICIT_TIMEOUT
     *
     * @param elements ExtendedWebElement...
     * @return boolean return true only if all elements present.
     */
    @SuppressWarnings("unused")
    default boolean allElementsPresent(ExtendedWebElement... elements) {
        return allElementsPresent(EXPLICIT_TIMEOUT, elements);
    }

    /**
     * Method which quickly looks for all element and check that they present
     * during timeout sec
     *
     * @param timeout long
     * @param elements ExtendedWebElement...
     * @return boolean return true only if all elements present.
     */
    default boolean allElementsPresent(long timeout, ExtendedWebElement... elements) {
        int index = 0;
        boolean present = true;
        boolean ret = true;
        int counts = 1;
        timeout = timeout / counts;
        if (timeout < 1)
            timeout = 1;
        while (present && index++ < counts) {
            for (ExtendedWebElement element : elements) {
                present = element.isElementPresent(timeout);
                if (!present) {
                    I_DRIVER_HELPER_LOGGER.error("{} is not present.", element.getNameWithLocator());
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
    @SuppressWarnings({ "unchecked", "unused" })
    default boolean allElementListsAreNotEmpty(List<ExtendedWebElement>... elements) {
        return allElementListsAreNotEmpty(EXPLICIT_TIMEOUT / 3, elements);
    }

    /**
     * Method which quickly looks for all element lists and check that they
     * contain at least one element during timeout
     *
     * @param timeout long
     * @param elements List&lt;ExtendedWebElement&gt;...
     * @return boolean return true only if All Element lists contain at least one element
     */
    @SuppressWarnings("unchecked")
    default boolean allElementListsAreNotEmpty(long timeout, List<ExtendedWebElement>... elements) {
        boolean ret;
        int counts = 3;
        timeout = timeout / counts;
        if (timeout < 1)
            timeout = 1;
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
                I_DRIVER_HELPER_LOGGER.error("List of elements[{}] from elements {} is empty.", i, elementsAsString);
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
    @SuppressWarnings("unused")
    default boolean isAnyElementPresent(ExtendedWebElement... elements) {
        return isAnyElementPresent(EXPLICIT_TIMEOUT / 3, elements);
    }

    /**
     * Method which quickly looks for any element presence during timeout sec
     *
     * @param timeout long
     * @param elements ExtendedWebElement...
     * @return true if any of elements was found.
     */
    default boolean isAnyElementPresent(long timeout, ExtendedWebElement... elements) {
        int index = 0;
        int counts = 10;
        timeout = timeout / counts;
        if (timeout < 1)
            timeout = 1;
        while (index++ < counts) {
            for (ExtendedWebElement element : elements) {
                if (element.isElementPresent(timeout)) {
                    I_DRIVER_HELPER_LOGGER.debug("{} is present", element.getNameWithLocator());
                    return true;
                }
            }
        }
        String elementsAsString = Arrays.toString(elements);
        I_DRIVER_HELPER_LOGGER.error("Unable to find any element from array: {}", elementsAsString);
        return false;
    }

    /**
     * return Any Present Element from the list which present during
     * SHORT_TIMEOUT
     *
     * @param elements ExtendedWebElement...
     * @return ExtendedWebElement
     */
    default ExtendedWebElement returnAnyPresentElement(ExtendedWebElement... elements) {
        return returnAnyPresentElement(EXPLICIT_TIMEOUT / 3, elements);
    }

    /**
     * return Any Present Element from the list which present during timeout sec
     *
     * @param timeout long
     * @param elements ExtendedWebElement...
     * @return ExtendedWebElement
     */
    default ExtendedWebElement returnAnyPresentElement(long timeout, ExtendedWebElement... elements) {
        int index = 0;
        int counts = 10;
        timeout = timeout / counts;
        if (timeout < 1)
            timeout = 1;
        while (index++ < counts) {
            for (ExtendedWebElement element : elements) {
                if (element.isElementPresent(timeout)) {
                    I_DRIVER_HELPER_LOGGER.debug("{} is present", element.getNameWithLocator());
                    return element;
                }
            }
        }
        // throw exception anyway if nothing was returned inside for cycle
        I_DRIVER_HELPER_LOGGER.error("All elements are not present");
        throw new RuntimeException("Unable to find any element from array: " + Arrays.toString(elements));
    }

    /**
     * Clicks on element.
     *
     * @param elements ExtendedWebElements to click
     */
    @SuppressWarnings("unused")
    default void clickAny(ExtendedWebElement... elements) {
        clickAny(EXPLICIT_TIMEOUT, elements);
    }

    /**
     * Clicks on element.
     *
     * @param elements ExtendedWebElements to click
     * @param timeout to wait
     */
    default void clickAny(long timeout, ExtendedWebElement... elements) {
        // Method which quickly looks for any element and click during timeout
        // sec
        ActionPoller<ExtendedWebElement> actionPoller = ActionPoller.builder();

        Optional<ExtendedWebElement> searchableElement = actionPoller.task(() -> {
            ExtendedWebElement possiblyFoundElement = null;

            for (ExtendedWebElement element : elements) {
                if (element.isClickable()) {
                    possiblyFoundElement = element;
                    break;
                }
            }
            return possiblyFoundElement;
        })
                .until(Objects::nonNull)
                .pollEvery(0, ChronoUnit.SECONDS)
                .stopAfter(timeout, ChronoUnit.SECONDS)
                .execute();

        if (searchableElement.isEmpty()) {
            throw new RuntimeException(String.format("Unable to click onto any elements from array: %s", Arrays.toString(elements)));
        }

        searchableElement.get()
                .click();
    }

    /**
     * Check that element with text present.
     *
     * @deprecated use {@link ExtendedWebElement#isElementWithTextPresent(String)} instead
     * @param extWebElement to check if element with text is present
     * @param text of element to check.
     * @return element with text existence status.
     */
    @Deprecated(forRemoval = true, since = "1.0.1")
    default boolean isElementWithTextPresent(final ExtendedWebElement extWebElement, final String text) {
        return isElementWithTextPresent(extWebElement, text, EXPLICIT_TIMEOUT);
    }

    /**
     * Check that element with text present.
     *
     * @deprecated use {@link ExtendedWebElement#isElementWithTextPresent(String)} instead
     *
     * @param extWebElement to check if element with text is present
     * @param text of element to check.
     * @param timeout Long
     * @return element with text existence status.
     */
    @Deprecated(forRemoval = true, since = "1.0.1")
    default boolean isElementWithTextPresent(final ExtendedWebElement extWebElement, final String text, long timeout) {
        return extWebElement.isElementWithTextPresent(text, timeout);
    }

    /**
     * Check that element not present on page.
     *
     * @deprecated use {@link ExtendedWebElement#isElementNotPresent(long)} instead
     *
     * @param extWebElement to check if element is not present
     * @return element non-existence status.
     */
    @Deprecated(forRemoval = true, since = "1.0.1")
    default boolean isElementNotPresent(final ExtendedWebElement extWebElement) {
        return isElementNotPresent(extWebElement, EXPLICIT_TIMEOUT);
    }

    /**
     * Check that element not present on page.
     *
     * @deprecated use {@link ExtendedWebElement#isElementNotPresent(long)} instead
     *
     * @param extWebElement to check if element is not present
     * @param timeout to wait
     * @return element non-existence status.
     */
    @Deprecated(forRemoval = true, since = "1.0.1")
    default boolean isElementNotPresent(final ExtendedWebElement extWebElement, long timeout) {
        return extWebElement.isElementNotPresent(timeout);
    }

    /**
     * Check that element not present on page.
     *
     * @deprecated use {@link ExtendedWebElement#isElementNotPresent(long)} instead
     *
     * @param element to check if element is not present
     * @param controlInfo String
     * @return element non-existence status.
     */
    @Deprecated(forRemoval = true, since = "1.0.1")
    default boolean isElementNotPresent(String controlInfo, final WebElement element) {
        throw new UnsupportedOperationException();
        // return isElementNotPresent(new ExtendedWebElement(element, controlInfo));
    }

    /**
     * Checks that alert modal is shown.
     *
     * @return whether the alert modal present.
     */
    default boolean isAlertPresent() {
        try {
            getDriver().switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            return false;
        }
    }

    /*
     * Get and return the source of the last loaded page.
     * 
     * @return String
     */
    @SuppressWarnings("unused")
    default String getPageSource() {
        Messager.GET_PAGE_SOURCE.info();

        Wait<WebDriver> wait = new FluentWait<>(getDriver())
                .pollingEvery(Duration.ofMillis(5000)) // there is no sense to refresh url address too often
                .withTimeout(Duration.ofSeconds(Configuration.getInt(Configuration.Parameter.EXPLICIT_TIMEOUT)))
                .ignoring(WebDriverException.class)
                .ignoring(JavascriptException.class); // org.openqa.selenium.JavascriptException: javascript error: Cannot read property 'outerHTML'
                                                      // of null
        String res = "";
        try {
            res = wait.until(WebDriver::getPageSource);
        } catch (ScriptTimeoutException | TimeoutException e) {
            Messager.FAIL_GET_PAGE_SOURCE.error();
        }
        Messager.GET_PAGE_SOURCE.info();
        return res;
    }

    /*
     * Add cookie object into the driver
     * 
     * @param Cookie
     */
    default void addCookie(Cookie cookie) {
        DriverListener.setMessages(Messager.ADD_COOKIE.getMessage(cookie.getName()),
                Messager.FAIL_ADD_COOKIE.getMessage(cookie.getName()));
        Wait<WebDriver> wait = new FluentWait<>(getDriver())
                .pollingEvery(Duration.ofMillis(Configuration.getInt(Configuration.Parameter.RETRY_INTERVAL)))
                .withTimeout(Duration.ofSeconds(Configuration.getInt(Configuration.Parameter.EXPLICIT_TIMEOUT)))
                .ignoring(WebDriverException.class)
                .ignoring(
                        JsonException.class); // org.openqa.selenium.json.JsonException: Expected to read a START_MAP but instead have: END. Last 0
                                              // characters rea
        wait.until(drv -> {
            drv.manage().addCookie(cookie);
            return true;
        });
    }

    /**
     * Get a string representing the current URL that the browser is looking at.
     *
     * @return url.
     */
    default String getCurrentUrl() {
        return getCurrentUrl(Configuration.getInt(Configuration.Parameter.EXPLICIT_TIMEOUT));
    }

    /**
     * Get a string representing the current URL that the browser is looking at.
     *
     * @param timeout long
     * @return validation result.
     */
    default String getCurrentUrl(long timeout) {
        // explicitly limit time for the getCurrentUrl operation
        Future<?> future = Executors.newSingleThreadExecutor().submit(() -> {
            // organize fluent waiter for getting url
            Wait<WebDriver> wait = new FluentWait<WebDriver>(getDriver())
                    .pollingEvery(Duration.ofMillis(Configuration.getInt(Configuration.Parameter.RETRY_INTERVAL)))
                    .withTimeout(Duration.ofSeconds(Configuration.getInt(Configuration.Parameter.EXPLICIT_TIMEOUT)))
                    .ignoring(WebDriverException.class)
                    .ignoring(
                            JsonException.class); // org.openqa.selenium.json.JsonException: Expected to read a START_MAP but instead have: END. Last
                                                  // 0 characters rea
            return wait.until(WebDriver::getCurrentUrl);
        });

        String url = "";
        try {
            url = (String) future.get(timeout, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            I_DRIVER_HELPER_LOGGER.debug("Unable to get driver url during " + timeout + "sec!", e);
        } catch (InterruptedException e) {
            I_DRIVER_HELPER_LOGGER.debug("Unable to get driver url during " + timeout + "sec!", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            I_DRIVER_HELPER_LOGGER.error("ExecutionException error on get driver url!", e);
        } catch (Exception e) {
            I_DRIVER_HELPER_LOGGER.error("Undefined error on get driver url detected!", e);
        }
        return url;
    }

    /**
     * Checks that current URL is as expected.
     *
     * @param expectedURL expected Url
     * @return validation result.
     */
    default boolean isUrlAsExpected(String expectedURL) {
        return isUrlAsExpected(expectedURL, Configuration.getInt(Configuration.Parameter.EXPLICIT_TIMEOUT));
    }

    /**
     * Checks that current URL is as expected.
     *
     * @param expectedURL Expected Url
     * @param timeout long
     * @return validation result.
     */
    default boolean isUrlAsExpected(String expectedURL, long timeout) {
        String decryptedURL = CryptoUtils.INSTANCE.decryptIfEncrypted(expectedURL);
        decryptedURL = getEnvArgURL(decryptedURL);
        String actualUrl = getCurrentUrl(timeout);
        if (LogicUtils.isURLEqual(decryptedURL, actualUrl)) {
            Messager.EXPECTED_URL.info(actualUrl);
            return true;
        } else {
            Messager.UNEXPECTED_URL.error(expectedURL, actualUrl);
            return false;
        }
    }

    /**
     * Get full or relative URL considering Env argument
     * 
     * todo hide
     *
     * @param decryptedURL String
     * @return url
     */
    default String getEnvArgURL(String decryptedURL) {
        if (!(decryptedURL.contains("http:") || decryptedURL.contains("https:"))) {
            if (Configuration.getEnvArg(Configuration.Parameter.URL.getKey()).isEmpty()) {
                decryptedURL = Configuration.get(Configuration.Parameter.URL) + decryptedURL;
            } else {
                decryptedURL = Configuration.getEnvArg(Configuration.Parameter.URL.getKey()) + decryptedURL;
            }
        }
        return decryptedURL;
    }

    /**
     * Get clipboard text
     *
     * @return String saved in clipboard
     */
    default String getClipboardText() {
        try {
            I_DRIVER_HELPER_LOGGER.debug("Trying to get clipboard from remote machine with hub...");
            String url = getSelenoidClipboardUrl(getDriver());
            String username = getField(url, 1);
            String password = getField(url, 2);

            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("GET");

            if (!username.isEmpty() && !password.isEmpty()) {
                String usernameColonPassword = username + ":" + password;
                String basicAuthPayload = "Basic " + Base64.getEncoder().encodeToString(usernameColonPassword.getBytes());
                con.addRequestProperty("Authorization", basicAuthPayload);
            }

            String clipboardText = "";
            int status = con.getResponseCode();
            if (200 <= status && status <= 299) {
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder sb = new StringBuilder();
                while ((inputLine = br.readLine()) != null) {
                    sb.append(inputLine);
                }
                br.close();
                clipboardText = sb.toString();
            } else {
                I_DRIVER_HELPER_LOGGER.debug("Trying to get clipboard from local java machine...");
                clipboardText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            }

            clipboardText = clipboardText.replace("\n", "");
            I_DRIVER_HELPER_LOGGER.info("Clipboard: {}", clipboardText);
            return clipboardText;
        } catch (Exception e) {
            throw new RuntimeException("Error when try to get clipboard text.", e);
        }
    }

    /**
     * Set text to clipboard
     *
     * @param text text
     * @return true if successful, false otherwise
     */
    default boolean setClipboardText(String text) {
        boolean isSuccessful = false;
        try {
            I_DRIVER_HELPER_LOGGER.debug("Trying to set text to clipboard on the remote machine with hub...");
            String url = getSelenoidClipboardUrl(getDriver());
            String username = getField(url, 1);
            String password = getField(url, 2);

            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) new URL(url)
                    .openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);

            if (!username.isEmpty() && !password.isEmpty()) {
                String usernameColonPassword = username + ":" + password;
                String basicAuthPayload = "Basic " + Base64.getEncoder().encodeToString(usernameColonPassword.getBytes());
                con.addRequestProperty("Authorization", basicAuthPayload);
            }

            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(text);
            wr.flush();
            wr.close();

            int status = con.getResponseCode();
            if (!(200 <= status && status <= 299)) {
                throw new IOException("Response status code is not successful");
            }
            isSuccessful = true;
        } catch (Exception e) {
            I_DRIVER_HELPER_LOGGER.error("Error occurred when try to set clipboard to remote machine with hub", e);
            try {
                I_DRIVER_HELPER_LOGGER.debug("Trying to set clipboard to the local java machine...");
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
                isSuccessful = true;
            } catch (Exception ex) {
                I_DRIVER_HELPER_LOGGER.error("Error occurred when try to set clipboard to the local machine", ex);
            }
        }
        return isSuccessful;
    }

    private String getSelenoidClipboardUrl(WebDriver driver) {
        String seleniumHost = Configuration.getSeleniumUrl().replace("wd/hub", "clipboard/");
        if (seleniumHost.isEmpty()) {
            seleniumHost = Configuration.getEnvArg(Configuration.Parameter.URL.getKey()).replace("wd/hub", "clipboard/");
        }
        WebDriver drv = (driver instanceof Decorated<?>) ? (WebDriver) ((Decorated<?>) driver).getOriginal() : driver;
        String sessionId = ((RemoteWebDriver) drv).getSessionId().toString();
        String url = seleniumHost + sessionId;
        I_DRIVER_HELPER_LOGGER.debug("url: {}", url);
        return url;
    }

    private String getField(String url, int position) {
        Pattern pattern = Pattern.compile(".*:\\/\\/(.*):(.*)@");
        Matcher matcher = pattern.matcher(url);

        return matcher.find() ? matcher.group(position) : "";
    }

    /**
     * Return page title.
     *
     * @return title String.
     */
    default String getTitle() {
        return getTitle(Configuration.getInt(Configuration.Parameter.EXPLICIT_TIMEOUT));
    }

    /**
     * Return page title.
     *
     * @param timeout long
     * @return title String.
     */
    default String getTitle(long timeout) {
        Wait<WebDriver> wait = new FluentWait<>(getDriver())
                .pollingEvery(Duration.ofMillis(RETRY_TIME))
                .withTimeout(Duration.ofSeconds(timeout))
                .ignoring(WebDriverException.class)
                .ignoring(JavascriptException.class); // org.openqa.selenium.JavascriptException: javascript error: Cannot read property 'outerHTML'
                                                      // of null
        String res = "";
        try {
            res = wait.until(WebDriver::getTitle);
        } catch (ScriptTimeoutException | TimeoutException e) {
            Messager.FAIL_GET_TITLE.error();
        }
        return res;
    }

    /**
     * Checks that page title is as expected.
     *
     * @param expectedTitle expected title
     * @return validation result.
     */
    default boolean isTitleAsExpected(final String expectedTitle) {
        final String decryptedExpectedTitle = CryptoUtils.INSTANCE.decryptIfEncrypted(expectedTitle);
        String title = getTitle(EXPLICIT_TIMEOUT);
        boolean result = title.contains(decryptedExpectedTitle);
        if (result) {
            Messager.TITLE_CORRECT.info(expectedTitle);
        } else {
            Messager.TITLE_NOT_CORRECT.error(expectedTitle, title);
        }
        return result;
    }

    /**
     * Checks that page suites to expected pattern.
     *
     * @param expectedPattern Expected Pattern
     * @return validation result.
     */
    default boolean isTitleAsExpectedPattern(String expectedPattern) {
        final String decryptedExpectedPattern = CryptoUtils.INSTANCE.decryptIfEncrypted(expectedPattern);
        String actual = getTitle(EXPLICIT_TIMEOUT);
        Pattern p = Pattern.compile(decryptedExpectedPattern);
        Matcher m = p.matcher(actual);
        boolean result = m.find();
        if (result) {
            Messager.TITLE_CORRECT.info(actual);
        } else {
            Messager.TITLE_NOT_CORRECT.error(expectedPattern, actual);
        }
        return result;
    }

    /**
     * Go back in browser.
     */
    default void navigateBack() {
        getDriver().navigate().back();
        Messager.BACK.info();
    }

    /**
     * Refresh the current page.
     */
    default void refresh() {
        refresh(Configuration.getInt(Configuration.Parameter.EXPLICIT_TIMEOUT));
    }

    /**
     * Refresh the current page.
     *
     * @param timeout in seconds
     */
    default void refresh(long timeout) {
        Wait<WebDriver> wait = new FluentWait<>(getDriver())
                .pollingEvery(Duration.ofMillis(5000)) // there is no sense to refresh url address too often
                .withTimeout(Duration.ofSeconds(timeout))
                .ignoring(WebDriverException.class)
                .ignoring(JsonException.class); // org.openqa.selenium.json.JsonException: Expected to read a START_MAP but instead have: END. Last 0
        // characters read
        try {
            wait.until((Function<WebDriver, Void>) drv -> {
                drv.navigate().refresh();
                return null;
            });
        } catch (ScriptTimeoutException | TimeoutException e) {
            Messager.FAIL_REFRESH.error();
        }
        Messager.REFRESH.info();
    }

    default void pressTab() {
        Actions builder = new Actions(getDriver());
        builder.sendKeys(Keys.TAB).perform();
    }

    /**
     * Drags and drops element to specified place.
     *
     * @param from element to drag.
     * @param to element to drop to.
     */
    default void dragAndDrop(final ExtendedWebElement from, final ExtendedWebElement to) {

        if (from.isElementPresent() && to.isElementPresent()) {
            WebDriver drv = getDriver();
            if (!drv.toString().contains("safari")) {
                Actions builder = new Actions(drv);
                Action dragAndDrop = builder.clickAndHold(from).moveToElement(to)
                        .release(to).build();
                dragAndDrop.perform();
            } else {
                String xto = Integer.toString(((WebElement) to).getLocation().x);
                String yto = Integer.toString(((WebElement) to).getLocation().y);
                ((JavascriptExecutor) getDriver())
                        .executeScript(
                                "function simulate(f,c,d,e){var b,a=null;for(b in eventMatchers)if(eventMatchers[b].test(c)){a=b;break}if(!a)return!1;document.createEvent?(b=document.createEvent(a),a==\"HTMLEvents\"?b.initEvent(c,!0,!0):b.initMouseEvent(c,!0,!0,document.defaultView,0,d,e,d,e,!1,!1,!1,!1,0,null),f.dispatchEvent(b)):(a=document.createEventObject(),a.detail=0,a.screenX=d,a.screenY=e,a.clientX=d,a.clientY=e,a.ctrlKey=!1,a.altKey=!1,a.shiftKey=!1,a.metaKey=!1,a.button=1,f.fireEvent(\"on\"+c,a));return!0} var eventMatchers={HTMLEvents:/^(?:load|unload|abort|error|select|change|submit|reset|focus|blur|resize|scroll)$/,MouseEvents:/^(?:click|dblclick|mouse(?:down|up|over|move|out))$/}; "
                                        + "simulate(arguments[0],\"mousedown\",0,0); simulate(arguments[0],\"mousemove\",arguments[1],arguments[2]); simulate(arguments[0],\"mouseup\",arguments[1],arguments[2]); ",
                                from, xto, yto);
            }
            Messager.ELEMENTS_DRAGGED_AND_DROPPED.info(from.getName(), to.getName());
        } else {
            Messager.ELEMENTS_NOT_DRAGGED_AND_DROPPED.error(from.getNameWithLocator(), to.getNameWithLocator());
        }
    }

    /**
     * Drags and drops element to specified place. Elements Need To have an id.
     *
     * @param from the element to drag.
     * @param to the element to drop to.
     */
    default void dragAndDropHtml5(final ExtendedWebElement from, final ExtendedWebElement to) {
        String source = from.getAttribute("id");
        String target = to.getAttribute("id");
        if (source.isEmpty() || target.isEmpty()) {
            Messager.ELEMENTS_NOT_DRAGGED_AND_DROPPED.error(from.getNameWithLocator(), to.getNameWithLocator());
        } else {
            jQuerify(getDriver());
            String javaScript = "(function( $ ) {        $.fn.simulateDragDrop = function(options) { return this.each(function() {  "
                    + " new $.simulateDragDrop(this, options); }); };  $.simulateDragDrop = function(elem, options) { "
                    + " this.options = options;  this.simulateEvent(elem, options); };  "
                    + " $.extend($.simulateDragDrop.prototype, {  simulateEvent: function(elem, options) {   "
                    + " /*Simulating drag start*/  var type = 'dragstart';  "
                    + " var event = this.createEvent(type); this.dispatchEvent(elem, type, event);  "
                    + "  /*Simulating drop*/    type = 'drop';  "
                    + "var dropEvent = this.createEvent(type, {}); "
                    + "dropEvent.dataTransfer = event.dataTransfer;  "
                    + "this.dispatchEvent($(options.dropTarget)[0], type, dropEvent);  "
                    + " /*Simulating drag end*/       type = 'dragend'; "
                    + "  var dragEndEvent = this.createEvent(type, {}); "
                    + " dragEndEvent.dataTransfer = event.dataTransfer; "
                    + " this.dispatchEvent(elem, type, dragEndEvent);   }, "
                    + " createEvent: function(type) {   var event = document.createEvent(\"CustomEvent\"); "
                    + "  event.initCustomEvent(type, true, true, null);   event.dataTransfer = {  "
                    + " data: {    },  setData: function(type, val){  "
                    + " this.data[type] = val; }, "
                    + "  getData: function(type){   return this.data[type];   } "
                    + "   };   return event;  },               "
                    + " dispatchEvent: function(elem, type, event) {  if(elem.dispatchEvent) { "
                    + "    elem.dispatchEvent(event);  }else if( elem.fireEvent ) {  "
                    + "  elem.fireEvent(\"on\"+type, event);  }  }  });})(jQuery);";

            ((JavascriptExecutor) getDriver()).executeScript(javaScript + "$('#" + source + "')" +
                    ".simulateDragDrop({ dropTarget: '#" + target + "'});");
            Messager.ELEMENTS_DRAGGED_AND_DROPPED.info(from.getName(), to.getName());
        }
    }

    private static void jQuerify(WebDriver driver) {
        String jQueryLoader = "(function(jqueryUrl, callback) {\n" +
                "    if (typeof jqueryUrl != 'string') {\n" +
                "        jqueryUrl = 'https://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js';\n" +
                "    }\n" +
                "    if (typeof jQuery == 'undefined') {\n" +
                "        var script = document.createElement('script');\n" +
                "        var head = document.getElementsByTagName('head')[0];\n" +
                "        var done = false;\n" +
                "        script.onload = script.onreadystatechange = (function() {\n" +
                "            if (!done && (!this.readyState || this.readyState == 'loaded'\n" +
                "                    || this.readyState == 'complete')) {\n" +
                "                done = true;\n" +
                "                script.onload = script.onreadystatechange = null;\n" +
                "                head.removeChild(script);\n" +
                "                callback();\n" +
                "            }\n" +
                "        });\n" +
                "        script.src = jqueryUrl;\n" +
                "        head.appendChild(script);\n" +
                "    }\n" +
                "    else {\n" +
                "        callback();\n" +
                "    }\n" +
                "})(arguments[0], arguments[arguments.length - 1]);";
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(10));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeAsyncScript(jQueryLoader);
    }

    /**
     * Performs slider move for specified offset.
     *
     * @param slider slider
     * @param moveX move x
     * @param moveY move y
     */
    default void slide(ExtendedWebElement slider, int moveX, int moveY) {
        // TODO: SZ migrate to FluentWaits
        if (slider.isElementPresent()) {
            WebDriver drv = getDriver();
            (new Actions(drv)).moveToElement(slider).dragAndDropBy(slider, moveX, moveY)
                    .build().perform();
            Messager.SLIDER_MOVED.info(slider.getNameWithLocator(), String.valueOf(moveX), String.valueOf(moveY));
        } else {
            Messager.SLIDER_NOT_MOVED.error(slider.getNameWithLocator(), String.valueOf(moveX), String.valueOf(moveY));
        }
    }

    /**
     * Accepts alert modal.
     */
    default void acceptAlert() {
        WebDriver drv = getDriver();

        long retryInterval = getRetryInterval(EXPLICIT_TIMEOUT);
        Wait<WebDriver> wait = new WebDriverWait(drv, Duration.ofSeconds(EXPLICIT_TIMEOUT), Duration.ofMillis(retryInterval));
        try {
            wait.until((Function<WebDriver, Object>) dr -> isAlertPresent());
            drv.switchTo().alert().accept();
            Messager.ALERT_ACCEPTED.info("");
        } catch (Exception e) {
            Messager.ALERT_NOT_ACCEPTED.error("");
        }
    }

    // todo hide
    default long getRetryInterval(long timeout) {
        long retryInterval = RETRY_TIME;
        if (timeout >= 3 && timeout <= 10) {
            retryInterval = 500;
        }
        if (timeout > 10) {
            retryInterval = 1000;
        }
        return retryInterval;
    }

    /**
     * Cancels alert modal.
     */
    default void cancelAlert() {
        WebDriver drv = getDriver();
        long retryInterval = getRetryInterval(EXPLICIT_TIMEOUT);
        Wait<WebDriver> wait = new WebDriverWait(drv, Duration.ofSeconds(EXPLICIT_TIMEOUT), Duration.ofMillis(retryInterval));
        try {
            wait.until((Function<WebDriver, Object>) dr -> isAlertPresent());
            drv.switchTo().alert().dismiss();
            Messager.ALERT_CANCELED.info("");
        } catch (Exception e) {
            Messager.ALERT_NOT_CANCELED.error("");
        }
    }

    // --------------------------------------------------------------------------
    // Methods from v1.0
    // --------------------------------------------------------------------------

    /**
     * Executes a script on an element
     * <p>
     * Really should only be used when the web driver is sucking at exposing
     * functionality natively
     *
     * todo move to ExtendedWebElement
     *
     * @param script The script to execute
     * @param element The target of the script, referenced as arguments[0]
     * @return Object
     */
    default Object trigger(String script, WebElement element) {
        return ((JavascriptExecutor) getDriver()).executeScript(script, element);
    }

    /**
     * Executes a script.
     * <p>
     * Really should only be used when the web driver is sucking at exposing
     * functionality natively.
     * todo move to ExtendedWebElement
     * 
     * @param script The script to execute
     * @return Object
     */
    default Object trigger(String script) {
        return ((JavascriptExecutor) getDriver()).executeScript(script);
    }

    /**
     * Opens a new tab for the given URL
     *
     * @param url The URL to
     * @throws RuntimeException If unable to open tab
     */
    default void openTab(String url) {
        final String decryptedURL = CryptoUtils.INSTANCE.decryptIfEncrypted(url);
        String script = "var d=document,a=d.createElement('a');a.target='_blank';a.href='%s';a.innerHTML='.';d.body.appendChild(a);return a";
        Object element = trigger(String.format(script, decryptedURL));
        if (element instanceof WebElement) {
            WebElement anchor = (WebElement) element;
            anchor.click();
            trigger("var a=arguments[0];a.parentNode.removeChild(a);", anchor);
        } else {
            throw new RuntimeException("Unable to open tab");
        }
    }

    default void switchWindow() {
        WebDriver drv = getDriver();
        Set<String> handles = drv.getWindowHandles();
        String current = drv.getWindowHandle();
        if (handles.size() > 1) {
            handles.remove(current);
        }
        String newTab = handles.iterator().next();
        drv.switchTo().window(newTab);
    }

    // --------------------------------------------------------------------------
    // Base UI validations
    // --------------------------------------------------------------------------

    /**
     * @deprecated use {@link ExtendedWebElement#assertElementPresent(long)}
     */
    @Deprecated(forRemoval = true, since = "1.0.1")
    default void assertElementPresent(final ExtendedWebElement extWebElement) {
        assertElementPresent(extWebElement, EXPLICIT_TIMEOUT);
    }

    /**
     * @deprecated use {@link ExtendedWebElement#assertElementPresent(long)}
     */
    @Deprecated(forRemoval = true, since = "1.0.1")
    default void assertElementPresent(final ExtendedWebElement extWebElement, long timeout) {
        extWebElement.assertElementPresent(timeout);
    }

    /**
     * @deprecated use {@link ExtendedWebElement#assertElementWithTextPresent(String, long)}
     */
    @Deprecated(forRemoval = true, since = "1.0.1")
    default void assertElementWithTextPresent(final ExtendedWebElement extWebElement, final String text) {
        assertElementWithTextPresent(extWebElement, text, EXPLICIT_TIMEOUT);
    }

    /**
     * @deprecated use {@link ExtendedWebElement#assertElementWithTextPresent(String, long)}
     */
    @Deprecated(forRemoval = true, since = "1.0.1")
    default void assertElementWithTextPresent(final ExtendedWebElement extWebElement, final String text, long timeout) {
        extWebElement.assertElementWithTextPresent(text, timeout);
    }

    // --------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------

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
     * @return new {@link ExtendedWebElement} with formatted locator
     */
    default <T extends AbstractUIObject> T formatElement(T element, Object... objects) {
        if (Arrays.stream(objects).findAny().isPresent()) {
            LinkedList<LocatorConverter> copyLocatorConverters = new LinkedList<>(element.getLocatorConverters());
            // start of case when we have L10N only on this step
            boolean isTextContainsL10N = Arrays.stream(objects)
                    .map(String::valueOf)
                    .anyMatch(text -> LocalizeLocatorConverter.getL10nPattern().matcher(text).find());
            if (isTextContainsL10N) {
                boolean isAlreadyContainsL10NConverter = element.getLocatorConverters()
                        .stream()
                        .anyMatch(LocalizeLocatorConverter.class::isInstance);
                if (!isAlreadyContainsL10NConverter) {
                    LocalizeLocatorConverter converter = new LocalizeLocatorConverter();
                    copyLocatorConverters.addFirst(converter);
                }
            }
            // end of case when we have L10N only on this step

            if (copyLocatorConverters.stream()
                    .anyMatch(FormatLocatorConverter.class::isInstance)) {
                I_DRIVER_HELPER_LOGGER.debug(
                        "Called format method of ExtendedWebElement class with parameters, but FormatLocatorConverter already exists "
                                + "for element: '{}', so locator will be recreated from original locator with new format parameters.",
                        element.getDescriptionName());
                copyLocatorConverters.removeIf(FormatLocatorConverter.class::isInstance);
            }

            FormatLocatorConverter converter = new FormatLocatorConverter(objects);
            copyLocatorConverters.addFirst(converter);
            element.getOriginalBy().orElseThrow();

            return (T) AbstractUIObject.Builder.getInstance()
                    .setBy(element.getOriginalBy().get())
                    .setDriver(element.getDriver())
                    .setSearchContext(element.getSearchContext())
                    .setLoadingStrategy(element.getLoadingStrategy())
                    .setLocatorConverters(copyLocatorConverters)
                    // .setLocalizationKey(localizationKey)
                    .setDescriptionName(AbstractUIObject.DescriptionBuilder.getInstance()
                            .setClassName(element.getClass().getSimpleName())
                            .setContextDescription(element.getSearchContext().toString())
                            .setDescription("Format objects: " + Arrays.toString(objects))
                            .build())
                    .build(element.getClass());
        }
        return element;
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
     *
     * For all cases: if the method is called with no parameters, no locator formatting is applied, but elements will be created using original
     * locator.<br>
     *
     * <b>This method does not change the object on which it is called</b>.
     *
     * @param objects parameters
     * @return {@link List} of {@link ExtendedWebElement} if found, empty list otherwise
     */
    default <T extends AbstractUIObject> List<T> formatElementToList(T element, Object... objects) {
        List<T> extendedWebElementList = new ArrayList<>();

        LinkedList<LocatorConverter> copyLocatorConverters = new LinkedList<>(element.getLocatorConverters());

        if (Arrays.stream(objects).findAny().isPresent()) {
            // start of case when we have L10N only on this step
            boolean isTextContainsL10N = Arrays.stream(objects)
                    .map(String::valueOf)
                    .anyMatch(text -> LocalizeLocatorConverter.getL10nPattern().matcher(text).find());
            if (isTextContainsL10N) {
                boolean isAlreadyContainsL10NConverter = copyLocatorConverters
                        .stream()
                        .anyMatch(LocalizeLocatorConverter.class::isInstance);
                if (!isAlreadyContainsL10NConverter) {
                    LocalizeLocatorConverter converter = new LocalizeLocatorConverter();
                    copyLocatorConverters.addFirst(converter);
                }
            }
            // end of case when we have L10N only on this step

            if (copyLocatorConverters.stream().anyMatch(FormatLocatorConverter.class::isInstance)) {
                I_DRIVER_HELPER_LOGGER.debug(
                        "Called formatToList method of ExtendedWebElement class with parameters, but FormatLocatorConverter already exists "
                                + "for element: '{}', so locator will be recreated from original locator with new format parameters.",
                        element.getDescriptionName());
                copyLocatorConverters.removeIf(FormatLocatorConverter.class::isInstance);
            }
            FormatLocatorConverter converter = new FormatLocatorConverter(objects);
            copyLocatorConverters.addFirst(converter);
        }

        element.getOriginalBy()
                .orElseThrow();

        By finalBy = (By) AbstractUIObject.Builder.getInstance()
                .setBy(element.getOriginalBy().get())
                .setDriver(element.getDriver())
                .setSearchContext(element.getSearchContext())
                .setLocatorConverters(copyLocatorConverters)
                .build(element.getClass())
                .getBy()
                .orElseThrow();

        int i = 0;
        for (WebElement el : element.getSearchContext().findElements(finalBy)) {
            T extEl = (T) AbstractUIObject.Builder.getInstance()
                    .setElement(el)
                    .setDriver(element.getDriver())
                    .setSearchContext(element.getSearchContext())
                    .setLoadingStrategy(element.getLoadingStrategy())
                    .setDescriptionName(AbstractUIObject.DescriptionBuilder.getInstance()
                            .setClassName(element.getClass().getSimpleName())
                            .setContextDescription(element.getSearchContext().toString())
                            .setDescription("Objects: " + Arrays.toString(objects))
                            .setIndex(String.valueOf(i))
                            .build())
                    .build(element.getClass());
            // getLocalizationKey().ifPresent(key -> builder.setLocalizationKey(key + i));
            extendedWebElementList.add(extEl);
        }
        return extendedWebElementList;
    }

    /**
     * Find element on the page<br>
     * Element search is limited by the {@link Configuration.Parameter#EXPLICIT_TIMEOUT}
     *
     * @param by see {@link By}
     * @return {@link ExtendedWebElement} if exists, {@code null} otherwise
     */
    default ExtendedWebElement findExtendedWebElement(By by) {
        return findExtendedWebElement(by, by.toString(), EXPLICIT_TIMEOUT);
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
     * Element search is limited by the {@link Configuration.Parameter#EXPLICIT_TIMEOUT}
     *
     * @param by see {@link By}
     * @param name the name that will be given to the found element
     * @return {@link ExtendedWebElement} if exists, {@code null} otherwise
     */
    default ExtendedWebElement findExtendedWebElement(final By by, String name) {
        return findExtendedWebElement(by, name, EXPLICIT_TIMEOUT);
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
        return AbstractUIObject.Builder.getInstance()
                .setBy(by)
                .setDescriptionName(AbstractUIObject.DescriptionBuilder.getInstance()
                        .setClassName(ExtendedWebElement.class.getSimpleName())
                        .setContextDescription(getDriver().toString())
                        .build())
                .setDriver(getDriver())
                .setSearchContext(getDriver())
                .build(ExtendedWebElement.class);
    }

    /**
     * Find elements on the page<br>
     * Elements search is limited by the {@link Configuration.Parameter#EXPLICIT_TIMEOUT}
     *
     * @param by see {@link By}
     * @return list of {@link ExtendedWebElement}s, empty list otherwise
     */
    default List<ExtendedWebElement> findExtendedWebElements(By by) {
        return findExtendedWebElements(by, EXPLICIT_TIMEOUT);
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
        List<WebElement> webElements = getDriver().findElements(by);
        int i = 0;
        for (WebElement element : webElements) {
            ExtendedWebElement extEl = AbstractUIObject.Builder.getInstance()
                    .setElement(element)
                    .setDescriptionName(AbstractUIObject.DescriptionBuilder.getInstance()
                            .setClassName(ExtendedWebElement.class.getSimpleName())
                            .setContextDescription(getDriver().toString())
                            .setIndex(String.valueOf(i))
                            .build())
                    .setDriver(getDriver())
                    .setSearchContext(getDriver())
                    .build(ExtendedWebElement.class);
            extendedWebElements.add(extEl);
            i++;
        }
        return extendedWebElements;
    }

    /**
     * Wait until any condition happens.
     * <br>
     * CHANGES SINCE 1.0.1:<br>
     * 1. Logic fully replaced by logic from waitUntil method from ExtendedWebElement
     *
     * @param condition see {@link ExpectedCondition}
     * @param timeout timeout
     * @return true if condition happen
     */
    default boolean waitUntil(ExpectedCondition<?> condition, long timeout) {
        if (timeout < 1) {
            I_DRIVER_HELPER_LOGGER.warn("Fluent wait less than 1sec timeout might hangs! Updating to 1 sec.");
            timeout = 1;
        }

        long retryInterval = getRetryInterval(timeout);

        // try to use better tickMillis clock
        Wait<WebDriver> wait = new WebDriverWait(getDriver(), Duration.ofSeconds(timeout), Duration.ofMillis(retryInterval),
                java.time.Clock.tickMillis(java.time.ZoneId.systemDefault()), Sleeper.SYSTEM_SLEEPER)
                        .withTimeout(Duration.ofSeconds(timeout));

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

        I_DRIVER_HELPER_LOGGER.debug("waitUntil: starting... timeout: {}", timeout);
        boolean res = false;
        try {
            wait.until(condition);
            res = true;
        } catch (TimeoutException e) {
            I_DRIVER_HELPER_LOGGER.debug("waitUntil: org.openqa.selenium.TimeoutException", e);
        } finally {
            I_DRIVER_HELPER_LOGGER.debug("waiter is finished. conditions: {}", condition);
        }
        return res;
    }

    // TODO: uncomment javadoc when T could be described correctly
    /*
     * Method to handle SocketException due to okhttp factory initialization (java client 6.*).
     * Second execution of the same function works as expected.
     *
     * @param T The expected class of the supplier.
     * 
     * @param supplier Object
     * 
     * @return result Object
     */
    default <T> T performIgnoreException(Supplier<T> supplier) {
        try {
            I_DRIVER_HELPER_LOGGER.debug("Command will be performed with the exception ignoring");
            return supplier.get();
        } catch (WebDriverException e) {
            I_DRIVER_HELPER_LOGGER.info("Webdriver exception has been fired. One more attempt to execute action.", e);
            I_DRIVER_HELPER_LOGGER.info(supplier.toString());
            return supplier.get();
        }
    }

    /**
     * Open URL.
     *
     * @param url to open.
     */
    default void openURL(String url) {
        openURL(url, Configuration.getInt(Configuration.Parameter.EXPLICIT_TIMEOUT));
    }

    /**
     * Open URL.
     *
     * @param url to open.
     * @param timeout long
     */
    default void openURL(String url, long timeout) {
        final String decryptedURL = getEnvArgURL(CryptoUtils.INSTANCE.decryptIfEncrypted(url));
        WebDriver drv = getDriver();

        setPageLoadTimeout(drv, timeout);
        DriverListener.setMessages(Messager.OPENED_URL.getMessage(url), Messager.NOT_OPENED_URL.getMessage(url));

        // [VD] there is no sense to use fluent wait here as selenium just don't return something until page is ready!
        // explicitly limit time for the openURL operation
        try {
            Messager.OPENING_URL.info(url);
            drv.get(decryptedURL);
        } catch (UnhandledAlertException e) {
            drv.switchTo().alert().accept();
        } catch (TimeoutException e) {
            trigger("window.stop();"); // try to cancel page loading
            Assert.fail("Unable to open url during " + timeout + "sec!");
        } catch (Exception e) {
            Assert.fail("Undefined error on open url detected: " + e.getMessage(), e);
        } finally {
            // restore default pageLoadTimeout driver timeout
            setPageLoadTimeout(drv, getPageLoadTimeout());
            I_DRIVER_HELPER_LOGGER.debug("finished driver.get call.");
        }
    }

    // todo hide
    default void setPageLoadTimeout(WebDriver drv, long timeout) {
        try {
            drv.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(timeout));
        } catch (UnsupportedCommandException e) {
            // TODO: review upcoming appium 2.0 changes
            I_DRIVER_HELPER_LOGGER.debug("Appium: Not implemented yet for pageLoad timeout!");
        } catch (WebDriverException e) {
            I_DRIVER_HELPER_LOGGER.debug("Appium: Not implemented yet for pageLoad timeout!", e);
        }
    }

    // todo hide
    default long getPageLoadTimeout() {
        long timeout = 300;
        // #1705: limit pageLoadTimeout driver timeout by idleTimeout
        // if (!R.CONFIG.get("capabilities.idleTimeout").isEmpty()) {
        // long idleTimeout = R.CONFIG.getLong("capabilities.idleTimeout");
        // if (idleTimeout < timeout) {
        // timeout = idleTimeout;
        // }
        // }
        return timeout;
    }

}
