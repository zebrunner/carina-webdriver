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

import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
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
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.zebrunner.carina.utils.LogicUtils;
import com.zebrunner.carina.utils.common.CommonUtils;
import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.config.StandardConfigurationOption;
import com.zebrunner.carina.utils.encryptor.EncryptorUtils;
import com.zebrunner.carina.utils.messager.Messager;
import com.zebrunner.carina.utils.retry.ActionPoller;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration.Parameter;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.gui.AbstractPage;
import com.zebrunner.carina.webdriver.helper.IChromeDevToolsHelper;
import com.zebrunner.carina.webdriver.listener.DriverListener;
import com.zebrunner.carina.webdriver.locator.LocatorType;
import com.zebrunner.carina.webdriver.locator.LocatorUtils;

/**
 * DriverHelper - WebDriver wrapper for logging and reporting features. Also it
 * contains some complex operations with UI.
 *
 * @author Alex Khursevich
 */
public class DriverHelper implements IChromeDevToolsHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String REDUX_STORE_STATE_BASE_PATH = "window.store.getState()";
    protected static final long EXPLICIT_TIMEOUT = Configuration.getRequired(Parameter.EXPLICIT_TIMEOUT, Long.class);
    protected static final long SHORT_TIMEOUT = Configuration.getRequired(Parameter.EXPLICIT_TIMEOUT, Long.class) / 3;
    protected static final long RETRY_TIME = Configuration.getRequired(Parameter.RETRY_INTERVAL, Long.class);
    protected WebDriver driver;
    protected String pageURL = getUrl();

    public DriverHelper() {
    }

    public DriverHelper(WebDriver driver) {
        this();
        Objects.requireNonNull(driver, "WebDriver not initialized, check log files for details!");
        this.driver = driver;
    }

    /**
     * Opens page according to specified in constructor URL.
     */
    public void open() {
        openURL(this.pageURL);
    }

    /**
     * Open URL.
     *
     * @param url to open.
     */
    public void openURL(String url) {
        openURL(url, Configuration.getRequired(WebDriverConfiguration.Parameter.EXPLICIT_TIMEOUT, Integer.class));
    }

    /**
     * Open URL.
     *
     * @param url     to open.
     * @param timeout long
     */
    public void openURL(String url, long timeout) {
        final String decryptedURL = getEnvArgURL(EncryptorUtils.decrypt(url));
        this.pageURL = decryptedURL;
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
            LOGGER.debug("finished driver.get call.");
        }
    }

    protected void setPageURL(String relURL) {
        String baseURL;
        if (Configuration.get(Configuration.Parameter.ENV).isPresent()) {
            baseURL = Configuration.get("base", StandardConfigurationOption.ENVIRONMENT)
                    .orElse(Configuration.getRequired(Parameter.URL));
        } else {
            baseURL = Configuration.getRequired(Parameter.URL);
        }
        this.pageURL = baseURL + relURL;
    }

    protected void setPageAbsoluteURL(String url) {
        this.pageURL = url;
    }

    public String getPageURL() {
        return this.pageURL;
    }

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
    public boolean allElementsPresent(ExtendedWebElement... elements) {
        return allElementsPresent(EXPLICIT_TIMEOUT, elements);
    }

    /**
     * Method which quickly looks for all element and check that they present
     * during timeout sec
     *
     * @param timeout  long
     * @param elements ExtendedWebElement...
     * @return boolean return true only if all elements present.
     */
    public boolean allElementsPresent(long timeout, ExtendedWebElement... elements) {
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
                    LOGGER.error("{} is not present.", element.getNameWithLocator());
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
    public boolean allElementListsAreNotEmpty(List<ExtendedWebElement>... elements) {
        return allElementListsAreNotEmpty(SHORT_TIMEOUT, elements);
    }

    /**
     * Method which quickly looks for all element lists and check that they
     * contain at least one element during timeout
     *
     * @param timeout  long
     * @param elements List&lt;ExtendedWebElement&gt;...
     * @return boolean return true only if All Element lists contain at least one element
     */
    @SuppressWarnings("unchecked")
    public boolean allElementListsAreNotEmpty(long timeout, List<ExtendedWebElement>... elements) {
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
                LOGGER.error("List of elements[{}] from elements {} is empty.", i, elementsAsString);
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
    public boolean isAnyElementPresent(ExtendedWebElement... elements) {
        return isAnyElementPresent(SHORT_TIMEOUT, elements);
    }

    /**
     * Method which quickly looks for any element presence during timeout sec
     *
     * @param timeout  long
     * @param elements ExtendedWebElement...
     * @return true if any of elements was found.
     */
    public boolean isAnyElementPresent(long timeout, ExtendedWebElement... elements) {
        int index = 0;
        int counts = 10;
        timeout = timeout / counts;
        if (timeout < 1)
            timeout = 1;
        while (index++ < counts) {
            for (ExtendedWebElement element : elements) {
                if (element.isElementPresent(timeout)) {
                    LOGGER.debug("{} is present", element.getNameWithLocator());
                    return true;
                }
            }
        }

        LOGGER.error("Unable to find any element from array: {}", Arrays.toString(elements));
        return false;
    }

    /**
     * return Any Present Element from the list which present during
     * SHORT_TIMEOUT
     *
     * @param elements ExtendedWebElement...
     * @return ExtendedWebElement
     */
    public ExtendedWebElement returnAnyPresentElement(ExtendedWebElement... elements) {
        return returnAnyPresentElement(SHORT_TIMEOUT, elements);
    }

    /**
     * return Any Present Element from the list which present during timeout sec
     *
     * @param timeout  long
     * @param elements ExtendedWebElement...
     * @return ExtendedWebElement
     */
    public ExtendedWebElement returnAnyPresentElement(long timeout, ExtendedWebElement... elements) {
        int index = 0;
        int counts = 10;
        timeout = timeout / counts;
        if (timeout < 1)
            timeout = 1;
        while (index++ < counts) {
            for (ExtendedWebElement element : elements) {
                if (element.isElementPresent(timeout)) {
                    LOGGER.debug(element.getNameWithLocator() + " is present");
                    return element;
                }
            }
        }
        //throw exception anyway if nothing was returned inside for cycle
        LOGGER.error("All elements are not present");
        throw new RuntimeException("Unable to find any element from array: " + Arrays.toString(elements));
    }

    /**
     * Check that element with text present.
     *
     * @param extWebElement to check if element with text is present
     * @param text          of element to check.
     * @return element with text existence status.
     */
    public boolean isElementWithTextPresent(final ExtendedWebElement extWebElement, final String text) {
        return isElementWithTextPresent(extWebElement, text, EXPLICIT_TIMEOUT);
    }

    /**
     * Check that element with text present.
     *
     * @param extWebElement to check if element with text is present
     * @param text          of element to check.
     * @param timeout       Long
     * @return element with text existence status.
     */
    public boolean isElementWithTextPresent(final ExtendedWebElement extWebElement, final String text, long timeout) {
        return extWebElement.isElementWithTextPresent(text, timeout);
    }

    /**
     * Check that element not present on page.
     *
     * @param extWebElement to check if element is not present
     * @return element non-existence status.
     */
    public boolean isElementNotPresent(final ExtendedWebElement extWebElement) {
        return isElementNotPresent(extWebElement, EXPLICIT_TIMEOUT);
    }

    /**
     * Check that element not present on page.
     *
     * @param extWebElement to check if element is not present
     * @param timeout to wait
     * @return element non-existence status.
     */

    public boolean isElementNotPresent(final ExtendedWebElement extWebElement, long timeout) {
        return extWebElement.isElementNotPresent(timeout);
    }

    /**
     * Check that element not present on page.
     *
     * @param element to check if element is not present
     * @param controlInfo String
     * @return element non-existence status.
     */

    public boolean isElementNotPresent(String controlInfo, final WebElement element) {
        return isElementNotPresent(new ExtendedWebElement(element, controlInfo));
    }

    /**
     * Clicks on element.
     *
     * @param elements ExtendedWebElements to click
     */

    public void clickAny(ExtendedWebElement... elements) {
        clickAny(EXPLICIT_TIMEOUT, elements);
    }

    /**
     * Clicks on element.
     *
     * @param elements ExtendedWebElements to click
     * @param timeout to wait
     */

    public void clickAny(long timeout, ExtendedWebElement... elements) {
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
                .stopAfter(timeout, ChronoUnit.SECONDS)
                .execute();

        if (searchableElement.isEmpty()) {
            throw new RuntimeException(String.format("Unable to click onto any elements from array: %s", Arrays.toString(elements)));
        }

        searchableElement.get()
                .click();
    }

    /*
     * Get and return the source of the last loaded page.
     * @return String
     */
    public String getPageSource() {
        Messager.GET_PAGE_SOURCE.info();

        Wait<WebDriver> wait = new FluentWait<>(getDriver())
                .pollingEvery(Duration.ofMillis(5000)) // there is no sense to refresh url address too often
                .withTimeout(Duration.ofSeconds(Configuration.getRequired(Parameter.EXPLICIT_TIMEOUT, Integer.class)))
                .ignoring(WebDriverException.class)
                .ignoring(JavascriptException.class); // org.openqa.selenium.JavascriptException: javascript error: Cannot read property 'outerHTML' of null
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
     * @param Cookie
     */
    public void addCookie(Cookie cookie) {
        DriverListener.setMessages(Messager.ADD_COOKIE.getMessage(cookie.getName()),
                Messager.FAIL_ADD_COOKIE.getMessage(cookie.getName()));
        Wait<WebDriver> wait = new FluentWait<>(getDriver())
                .pollingEvery(Duration.ofMillis(Configuration.getRequired(Parameter.RETRY_INTERVAL, Integer.class)))
                .withTimeout(Duration.ofSeconds(Configuration.getRequired(Parameter.EXPLICIT_TIMEOUT, Integer.class)))
                .ignoring(WebDriverException.class)
                .ignoring(
                        JsonException.class); // org.openqa.selenium.json.JsonException: Expected to read a START_MAP but instead have: END. Last 0 characters rea
        wait.until(drv -> {
            drv.manage().addCookie(cookie);
            return true;
        });
    }

    /**
     * add an item to local storage
     *
     * @param name the name of the item to save
     * @param value the value of the item to save
     */
    public void addToLocalStorage(String name, String value) {
        ((JavascriptExecutor) getDriver())
                .executeScript("window.localStorage.setItem(arguments[0], arguments[0]);", name, value);
    }

    /**
     * gets the value of the stored item
     *
     * @param name the item that is stored
     * @return an optional containing the value of the stored item or an empty optional if nothing is stored.
     */
    public Optional<String> getValueFromLocalStorage(String name) {
        return Optional.ofNullable((String) ((JavascriptExecutor) getDriver())
                .executeScript("return window.localStorage.getItem(arguments[0]);", name));
    }

    /**
     * gets the name of a stored item via an index
     *
     * @param index the position of where the item is stored
     * @return an optional containing the name of the stored item for the index or an empty optional if
     * nothing is at the given index.
     */
    public Optional<String> getNameFromLocalStorage(int index) {
        return Optional.ofNullable((String) ((JavascriptExecutor) getDriver())
                .executeScript("return window.localStorage.key(arguments[0]);", index));
    }

    /**
     * remove an item from local storage
     *
     * @param name the name of the item to remove
     */
    public void removeFromLocalStorage(String name) {
        ((JavascriptExecutor) getDriver()).executeScript("window.localStorage.removeItem(arguments[0]);", name);
    }

    /**
     * clears the local storage
     */
    public void clearLocalStorage() {
        ((JavascriptExecutor) getDriver()).executeScript("window.localStorage.clear();");
    }

    /**
     * Get the current states for the Redux stores
     *
     * @return all store states
     */
    public JSONObject getReduxStoreStates() {
        String response = (String)((JavascriptExecutor) getDriver())
                .executeScript("return JSON.stringify(arguments[0]);", REDUX_STORE_STATE_BASE_PATH);
        return new JSONObject(response);
    }

    /**
     * Get the Redux store state for the specific path. For example, if the desired store state would be accessed
     * via:
     *
     * {@code window.store.getState().foo}
     *
     * then this method would return the value with a path of {@code foo}
     *
     * @param path redux path you want a state for
     * @return redux store state
     */
    public JSONObject getReduxStoreStateFor(String path) {
        String response = (String)((JavascriptExecutor) getDriver())
                .executeScript("return JSON.stringify(arguments[0].arguments[1]);", REDUX_STORE_STATE_BASE_PATH, path);
        return new JSONObject(response);
    }

    /**
     * update a redux store with a particular state
     *
     * @param action the type of action you want to update
     * @param payload the payload to be updated
     */
    public void updateReduxStoreStateWith(String action, JSONObject payload) {
        ((JavascriptExecutor) getDriver())
                .executeScript("arguments[0]({type: arguments[1], payload: arguments[2]});");
    }

    /**
     * Get a string representing the current URL that the browser is looking at.
     *
     * @return url.
     */
    public String getCurrentUrl() {
        return getCurrentUrl(Configuration.getRequired(Parameter.EXPLICIT_TIMEOUT, Integer.class));
    }

    /**
     * Get a string representing the current URL that the browser is looking at.
     *
     * @param timeout long
     * @return validation result.
     */
    public String getCurrentUrl(long timeout) {
        // explicitly limit time for the getCurrentUrl operation
        Future<?> future = Executors.newSingleThreadExecutor().submit(() -> {
            //organize fluent waiter for getting url
            Wait<WebDriver> wait = new FluentWait<>(getDriver())
                    .pollingEvery(Duration.ofMillis(Configuration.getRequired(Parameter.RETRY_INTERVAL, Integer.class)))
                    .withTimeout(Duration.ofSeconds(Configuration.getRequired(Parameter.EXPLICIT_TIMEOUT, Integer.class)))
                    .ignoring(WebDriverException.class)
                    .ignoring(
                            JsonException.class); // org.openqa.selenium.json.JsonException: Expected to read a START_MAP but instead have: END. Last 0 characters rea
            return wait.until(WebDriver::getCurrentUrl);
        });

        String url = "";
        try {
            url = (String) future.get(timeout, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.debug("Unable to get driver url during " + timeout + "sec!", e);
        } catch (InterruptedException e) {
            LOGGER.debug("Unable to get driver url during " + timeout + "sec!", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOGGER.error("ExecutionException error on get driver url!", e);
        } catch (Exception e) {
            LOGGER.error("Undefined error on get driver url detected!", e);
        }
        return url;
    }

    /**
     * Checks that current URL is as expected.
     *
     * @param expectedURL expected Url
     * @return validation result.
     */
    public boolean isUrlAsExpected(String expectedURL) {
        return isUrlAsExpected(expectedURL, Configuration.getRequired(Parameter.EXPLICIT_TIMEOUT, Integer.class));
    }

    /**
     * Checks that current URL is as expected.
     *
     * @param expectedURL Expected Url
     * @param timeout     long
     * @return validation result.
     */
    public boolean isUrlAsExpected(String expectedURL, long timeout) {
        String decryptedURL = EncryptorUtils.decrypt(expectedURL);
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
     * @param decryptedURL String
     * @return url
     */
    private String getEnvArgURL(String decryptedURL) {
        if (!(decryptedURL.contains("http:") || decryptedURL.contains("https:"))) {
            return Configuration.getRequired(Parameter.URL) + decryptedURL;
        }
        return decryptedURL;
    }

    /**
     * Get clipboard text
     *
     * @return String saved in clipboard
     */
    public String getClipboardText() {
        try {
            LOGGER.debug("Trying to get clipboard from remote machine with hub...");
            String url = getSelenoidClipboardUrl(driver);
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
                LOGGER.debug("Trying to get clipboard from local java machine...");
                clipboardText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            }

            clipboardText = clipboardText.replace("\n", "");
            LOGGER.info("Clipboard: {}", clipboardText);
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
    public boolean setClipboardText(String text) {
        boolean isSuccessful = false;
        try {
            LOGGER.debug("Trying to set text to clipboard on the remote machine with hub...");
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
            LOGGER.error("Error occurred when try to set clipboard to remote machine with hub", e);
            try {
                LOGGER.debug("Trying to set clipboard to the local java machine...");
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
                isSuccessful = true;
            } catch (Exception ex) {
                LOGGER.error("Error occurred when try to set clipboard to the local machine", ex);
            }
        }
        return isSuccessful;
    }

    private String getSelenoidClipboardUrl(WebDriver driver) {
        String seleniumHost = Configuration.getRequired(Parameter.SELENIUM_URL).replace("wd/hub", "clipboard/");
        WebDriver drv = (driver instanceof Decorated<?>) ? (WebDriver) ((Decorated<?>) driver).getOriginal() : driver;
        String sessionId = ((RemoteWebDriver) drv).getSessionId().toString();
        String url = seleniumHost + sessionId;
        LOGGER.debug("url: {}", url);
        return url;
    }

    private String getField(String url, int position) {
        Pattern pattern = Pattern.compile(".*:\\/\\/(.*):(.*)@");
        Matcher matcher = pattern.matcher(url);

        return matcher.find() ? matcher.group(position) : "";
    }

    /**
     * Pause for specified timeout.
     *
     * @param timeout in seconds.
     */
    public void pause(long timeout) {
        CommonUtils.pause(timeout);
    }

    public void pause(Double timeout) {
        CommonUtils.pause(timeout);
    }

    /**
     * Return page title.
     *
     * @return title String.
     */
    public String getTitle() {
        return getTitle(Configuration.getRequired(Parameter.EXPLICIT_TIMEOUT, Integer.class));
    }

    /**
     * Return page title.
     *
     * @param timeout long
     * @return title String.
     */
    public String getTitle(long timeout) {
        Wait<WebDriver> wait = new FluentWait<>(getDriver())
                .pollingEvery(Duration.ofMillis(RETRY_TIME))
                .withTimeout(Duration.ofSeconds(timeout))
                .ignoring(WebDriverException.class)
                .ignoring(JavascriptException.class); // org.openqa.selenium.JavascriptException: javascript error: Cannot read property 'outerHTML' of null
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
    public boolean isTitleAsExpected(final String expectedTitle) {
        final String decryptedExpectedTitle = EncryptorUtils.decrypt(expectedTitle);
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
    public boolean isTitleAsExpectedPattern(String expectedPattern) {
        final String decryptedExpectedPattern = EncryptorUtils.decrypt(expectedPattern);
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
    public void navigateBack() {
        getDriver().navigate().back();
        Messager.BACK.info();
    }

    /**
     * Refresh browser.
     */
    public void refresh() {
        refresh(Configuration.getRequired(Parameter.EXPLICIT_TIMEOUT, Integer.class));
    }

    /**
     * Refresh browser.
     *
     * @param timeout long
     */
    public void refresh(long timeout) {
        Wait<WebDriver> wait = new FluentWait<>(getDriver())
                .pollingEvery(Duration.ofMillis(5000)) // there is no sense to refresh url address too often
                .withTimeout(Duration.ofSeconds(timeout))
                .ignoring(WebDriverException.class)
                .ignoring(JsonException.class); // org.openqa.selenium.json.JsonException: Expected to read a START_MAP but instead have: END. Last 0 characters read
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

    public void pressTab() {
        Actions builder = new Actions(getDriver());
        builder.sendKeys(Keys.TAB).perform();
    }

    /**
     * Drags and drops element to specified place.
     *
     * @param from element to drag.
     * @param to   element to drop to.
     */
    public void dragAndDrop(final ExtendedWebElement from, final ExtendedWebElement to) {

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
                ((JavascriptExecutor) driver)
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

    /**
     * Drags and drops element to specified place. Elements Need To have an id.
     *
     * @param from the element to drag.
     * @param to   the element to drop to.
     */
    public void dragAndDropHtml5(final ExtendedWebElement from, final ExtendedWebElement to) {
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

            ((JavascriptExecutor) driver).executeScript(javaScript + "$('#" + source + "')" +
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
     * @param moveX  move x
     * @param moveY  move y
     */
    public void slide(ExtendedWebElement slider, int moveX, int moveY) {
    	//TODO: SZ migrate to FluentWaits
        if (slider.isElementPresent()) {
            WebDriver drv = getDriver();
            (new Actions(drv)).moveToElement(slider.getElement()).dragAndDropBy(slider.getElement(), moveX, moveY)
                    .build().perform();
			Messager.SLIDER_MOVED.info(slider.getNameWithLocator(), String.valueOf(moveX), String.valueOf(moveY));
        } else {
            Messager.SLIDER_NOT_MOVED.error(slider.getNameWithLocator(), String.valueOf(moveX), String.valueOf(moveY));
        }
    }

    /**
     * Accepts alert modal.
     */
    public void acceptAlert() {
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

    /**
     * Cancels alert modal.
     */
    public void cancelAlert() {
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

    /**
     * Checks that alert modal is shown.
     *
     * @return whether the alert modal present.
     */
    public boolean isAlertPresent() {
        try {
            getDriver().switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            return false;
        }
    }

    // --------------------------------------------------------------------------
    // Methods from v1.0
    // --------------------------------------------------------------------------
    public boolean isPageOpened(final AbstractPage page) {
        return isPageOpened(page, EXPLICIT_TIMEOUT);
    }

    public boolean isPageOpened(final AbstractPage page, long timeout) {
        boolean result;
        long retryInterval = getRetryInterval(timeout);
        Wait<WebDriver> wait = new WebDriverWait(getDriver(), Duration.ofSeconds(timeout), Duration.ofMillis(retryInterval));
        try {
            wait.until((Function<WebDriver, Object>) dr -> LogicUtils.isURLEqual(page.getPageURL(), dr.getCurrentUrl()));
            result = true;
        } catch (Exception e) {
            result = false;
        }
        if (!result) {
            LOGGER.warn("Actual URL differs from expected one. Expected '{}' but found '{}'",
                    page.getPageURL(), getDriver().getCurrentUrl());
        }
        return result;
    }

    /**
     * Executes a script on an element
     * <p>
     * Really should only be used when the web driver is sucking at exposing
     * functionality natively
     *
     * @param script  The script to execute
     * @param element The target of the script, referenced as arguments[0]
     * @return Object
     */
    public Object trigger(String script, WebElement element) {
        return ((JavascriptExecutor) getDriver()).executeScript(script, element);
    }

    /**
     * Executes a script.
     * <p>
     * Really should only be used when the web driver is sucking at exposing
     * functionality natively.
     *
     * @param script The script to execute
     * @return Object
     */
    public Object trigger(String script) {
        return ((JavascriptExecutor) getDriver()).executeScript(script);
    }

    /**
     * Opens a new tab for the given URL
     *
     * @param url The URL to
     * @throws RuntimeException If unable to open tab
     */
    public void openTab(String url) {
        final String decryptedURL = EncryptorUtils.decrypt(url);
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

    public void switchWindow() {
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
    public void assertElementPresent(final ExtendedWebElement extWebElement) {
        assertElementPresent(extWebElement, EXPLICIT_TIMEOUT);
    }

    public void assertElementPresent(final ExtendedWebElement extWebElement, long timeout) {
        extWebElement.assertElementPresent(timeout);
    }

    public void assertElementWithTextPresent(final ExtendedWebElement extWebElement, final String text) {
        assertElementWithTextPresent(extWebElement, text, EXPLICIT_TIMEOUT);
    }

    public void assertElementWithTextPresent(final ExtendedWebElement extWebElement, final String text, long timeout) {
        extWebElement.assertElementWithTextPresent(text, timeout);
    }

    // --------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------

    /**
     * Find element on the page<br>
     * Element search is limited by the {@link Parameter#EXPLICIT_TIMEOUT}
     *
     * @param by see {@link By}
     * @return {@link ExtendedWebElement} if exists, {@code null} otherwise
     */
    public ExtendedWebElement findExtendedWebElement(By by) {
        return findExtendedWebElement(by, by.toString(), EXPLICIT_TIMEOUT);
    }

    /**
     * Find element on the page
     *
     * @param by see {@link By}
     * @param timeout time to wait, in seconds
     * @return {@link ExtendedWebElement} if exists, {@code null} otherwise
     */
    public ExtendedWebElement findExtendedWebElement(By by, long timeout) {
        return findExtendedWebElement(by, by.toString(), timeout);
    }

    /**
     * Find element on the page<br>
     * Element search is limited by the {@link Parameter#EXPLICIT_TIMEOUT}
     *
     * @param by see {@link By}
     * @param name the name that will be given to the found element
     * @return {@link ExtendedWebElement} if exists, {@code null} otherwise
     */
    public ExtendedWebElement findExtendedWebElement(final By by, String name) {
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
    public ExtendedWebElement findExtendedWebElement(final By by, String name, long timeout) {
        DriverListener.setMessages(Messager.ELEMENT_FOUND.getMessage(name), Messager.ELEMENT_NOT_FOUND.getMessage(name));

        if (!waitUntil(ExpectedConditions.presenceOfElementLocated(by), timeout)) {
            Messager.ELEMENT_NOT_FOUND.error(name);
            return null;
        }
        return new ExtendedWebElement(by, name, getDriver(), getDriver());
    }

    /**
     * Find elements on the page<br>
     * Elements search is limited by the {@link Parameter#EXPLICIT_TIMEOUT}
     *
     * @param by see {@link By}
     * @return list of {@link ExtendedWebElement}s, empty list otherwise
     */
    public List<ExtendedWebElement> findExtendedWebElements(By by) {
        return findExtendedWebElements(by, EXPLICIT_TIMEOUT);
    }

    /**
     * Find elements on the page
     *
     * @param by see {@link By}
     * @param timeout time to wait, in seconds
     * @return list of {@link ExtendedWebElement}s if found, empty list otherwise
     */
    public List<ExtendedWebElement> findExtendedWebElements(final By by, long timeout) {
        List<ExtendedWebElement> extendedWebElements = new ArrayList<>();
        if (!waitUntil(ExpectedConditions.presenceOfElementLocated(by), timeout)) {
            Messager.ELEMENT_NOT_FOUND.info(by.toString());
    		return extendedWebElements;
    	}

        Optional<LocatorType> locatorType = LocatorUtils.getLocatorType(by);
        boolean isByForListSupported = locatorType.isPresent() && locatorType.get().isIndexSupport();
        String locatorAsString = by.toString();
        List<WebElement> webElements = getDriver().findElements(by);
        int i = 0;
        for (WebElement element : webElements) {
            String name = String.format("ExtendedWebElement - [%d]", i);
            ExtendedWebElement tempElement = new ExtendedWebElement(by, name, getDriver(), getDriver());
            tempElement.setElement(element);
            tempElement.setIsSingle(false);
            if (isByForListSupported) {
                tempElement.setIsRefreshSupport(true);
                tempElement.setBy(locatorType.get().buildLocatorWithIndex(locatorAsString, i));
            } else {
                tempElement.setIsRefreshSupport(false);
            }
            extendedWebElements.add(tempElement);
            i++;
        }
        return extendedWebElements;
    }

    protected void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    public WebDriver getDriver() {
        if (driver == null) {
            long currentThreadId = Thread.currentThread().getId();
            LOGGER.error("There is no any initialized driver for thread: {}", currentThreadId);
            throw new RuntimeException("Driver isn't initialized.");
        }
        return driver;
    }

    /**
     * Wait until any condition happens.
     *
     * @param condition ExpectedCondition
     * @param timeout   timeout
     * @return true if condition happen
     */
	public boolean waitUntil(ExpectedCondition<?> condition, long timeout) {
        boolean result;
        long startMillis = 0;
        final WebDriver drv = getDriver();
        long retryInterval = getRetryInterval(timeout);
        Wait<WebDriver> wait = new WebDriverWait(drv, Duration.ofSeconds(timeout), Duration.ofMillis(retryInterval));
        try {
            startMillis = System.currentTimeMillis();
            wait.until(condition);
            result = true;
            LOGGER.debug("waitUntil: finished true...");
        } catch (NoSuchElementException | TimeoutException e) {
            // don't write exception even in debug mode
            LOGGER.debug("waitUntil: NoSuchElementException | TimeoutException e... {}", condition.toString());
            result = false;
        } catch (Exception e) {
            LOGGER.error("waitUntil: " + condition.toString(), e);
            result = false;
        } finally {
            long timePassed = System.currentTimeMillis() - startMillis;
            // timePassed is time in ms timeout in sec so we have to adjust
            if (timePassed > 2 * timeout * 1000) {
                LOGGER.error("Your retry_interval is too low: {} ms! Increase it or upgrade your hardware", RETRY_TIME);
            }
        }
        return result;
    }

    //TODO: uncomment javadoc when T could be described correctly
    /*
     * Method to handle SocketException due to okhttp factory initialization (java client 6.*).
     * Second execution of the same function works as expected.
     *
     * @param T The expected class of the supplier.
     * @param supplier Object
     * @return result Object
     */
    public <T> T performIgnoreException(Supplier<T> supplier) {
        try {
            LOGGER.debug("Command will be performed with the exception ignoring");
            return supplier.get();
        } catch (WebDriverException e) {
            LOGGER.info("Webdriver exception has been fired. One more attempt to execute action.", e);
            LOGGER.info(supplier.toString());
            return supplier.get();
        }
    }

    private String getUrl() {
        return Configuration.get(Parameter.URL).orElse("");
    }

    private static void setPageLoadTimeout(WebDriver drv, long timeout) {
        try {
            drv.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(timeout));
        } catch (UnsupportedCommandException e) {
            // TODO: review upcoming appium 2.0 changes
            LOGGER.debug("Appium: Not implemented yet for pageLoad timeout!");
        } catch (WebDriverException e) {
            LOGGER.debug("Appium: Not implemented yet for pageLoad timeout!", e);
        }
    }

    private long getPageLoadTimeout() {
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

    private long getRetryInterval(long timeout) {
        long retryInterval = RETRY_TIME;
        if (timeout >= 3 && timeout <= 10) {
            retryInterval = 500;
        }
        if (timeout > 10) {
            retryInterval = 1000;
        }
        return retryInterval;
    }
}
