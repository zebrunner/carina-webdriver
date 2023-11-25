package com.zebrunner.carina.webdriver.helper;

import com.zebrunner.carina.utils.LogicUtils;
import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.config.StandardConfigurationOption;
import com.zebrunner.carina.utils.encryptor.EncryptorUtils;
import com.zebrunner.carina.utils.messager.Messager;
import com.zebrunner.carina.webdriver.IDriverPool;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.ScriptTimeoutException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.json.JsonException;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides information about web page
 */
public interface IPageDataHelper extends IDriverPool, IWaitHelper {
    static final Logger I_PAGE_DATA_HELPER_LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /*
     * Get and return the source of the last loaded page.
     *
     * @return String
     */
    default String getPageSource() {
        Messager.GET_PAGE_SOURCE.info();

        Wait<WebDriver> wait = new FluentWait<>(getDriver())
                .pollingEvery(Duration.ofMillis(5000)) // there is no sense to refresh url address too often
                .withTimeout(getDefaultWaitTimeout())
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

    /**
     * Get a string representing the current URL that the browser is looking at.
     *
     * @return url.
     */
    default String getCurrentUrl() {
        return getCurrentUrl(Configuration.getRequired(WebDriverConfiguration.Parameter.EXPLICIT_TIMEOUT, Integer.class));
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
            Wait<WebDriver> wait = new FluentWait<>(getDriver())
                    .pollingEvery(getDefaultWaitInterval(getDefaultWaitTimeout()))
                    .withTimeout(getDefaultWaitTimeout())
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
            I_PAGE_DATA_HELPER_LOGGER.debug("Unable to get driver url during " + timeout + "sec!", e);
        } catch (InterruptedException e) {
            I_PAGE_DATA_HELPER_LOGGER.debug("Unable to get driver url during " + timeout + "sec!", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            I_PAGE_DATA_HELPER_LOGGER.error("ExecutionException error on get driver url!", e);
        } catch (Exception e) {
            I_PAGE_DATA_HELPER_LOGGER.error("Undefined error on get driver url detected!", e);
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
        return isUrlAsExpected(expectedURL, Configuration.getRequired(WebDriverConfiguration.Parameter.EXPLICIT_TIMEOUT, Integer.class));
    }

    /**
     * Checks that current URL is as expected.
     *
     * @param expectedURL Expected Url
     * @param timeout long
     * @return validation result.
     */
    default boolean isUrlAsExpected(String expectedURL, long timeout) {
        String decryptedURL = EncryptorUtils.decrypt(expectedURL);
        if (!(decryptedURL.contains("http:") || decryptedURL.contains("https:"))) {
            decryptedURL = Configuration.getRequired(WebDriverConfiguration.Parameter.URL, StandardConfigurationOption.DECRYPT) + decryptedURL;
        }
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
     * Return page title.
     *
     * @return title String.
     */
    default String getTitle() {
        return getTitle(getDefaultWaitTimeout().toSeconds());
    }

    /**
     * Return page title.
     *
     * @param timeout long
     * @return title String.
     */
    default String getTitle(long timeout) {
        Wait<WebDriver> wait = new FluentWait<>(getDriver())
                .pollingEvery(getDefaultWaitInterval(Duration.ofSeconds(timeout)))
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
        final String decryptedExpectedTitle = EncryptorUtils.decrypt(expectedTitle);
        String title = getTitle(getDefaultWaitTimeout().toSeconds());
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
        final String decryptedExpectedPattern = EncryptorUtils.decrypt(expectedPattern);
        String actual = getTitle(getDefaultWaitTimeout().toSeconds());
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

}
