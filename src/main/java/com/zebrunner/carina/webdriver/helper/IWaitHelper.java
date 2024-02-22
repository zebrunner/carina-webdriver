package com.zebrunner.carina.webdriver.helper;

import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.webdriver.IDriverPool;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Sleeper;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Duration;

public interface IWaitHelper extends IDriverPool {
    Logger I_WAIT_HELPER_LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // for internal usage only
    Duration DEFAULT_EXPLICIT_TIMEOUT = Duration.ofSeconds(Configuration.getRequired(WebDriverConfiguration.Parameter.EXPLICIT_TIMEOUT, Long.class));
    // for internal usage only
    Duration DEFAULT_RETRY_INTERVAL = Duration.ofMillis(Configuration.getRequired(WebDriverConfiguration.Parameter.RETRY_INTERVAL, Long.class));

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

    default boolean waitUntil(ExpectedCondition<?> condition, Duration timeout) {
        return waitUntil(condition, timeout, getDefaultWaitInterval(timeout));
    }

    /**
     * Wait until any condition happens.
     *
     * @param condition {@link ExpectedCondition}
     * @param timeout {@link Duration}
     * @return true if condition happen
     */
    default boolean waitUntil(ExpectedCondition<?> condition, Duration timeout, Duration interval) {
        // try to use better tickMillis clock
        Wait<WebDriver> wait = new WebDriverWait(getDriver(), timeout, interval,
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

        I_WAIT_HELPER_LOGGER.debug("waitUntil: starting... timeout: {}", timeout);
        boolean res = false;
        long startMillis = 0;
        try {
            startMillis = System.currentTimeMillis();
            wait.until(condition);
            res = true;
        } catch (TimeoutException e) {
            I_WAIT_HELPER_LOGGER.debug("waitUntil: org.openqa.selenium.TimeoutException", e);
        } finally {
            long timePassed = System.currentTimeMillis() - startMillis;
            // timePassed is time in ms timeout in sec so we have to adjust
            if (timePassed > 2 * timeout.toSeconds() * 1000) {
                I_WAIT_HELPER_LOGGER.debug("Your retry_interval is too low: {} ms! Increase it or upgrade your hardware",
                        getDefaultWaitInterval(timeout));
            }
        }
        return res;
    }

    default Duration getDefaultWaitTimeout() {
        return DEFAULT_EXPLICIT_TIMEOUT;
    }

    /**
     * Get waiting interval
     *
     * @param timeout timeout
     * @return {@link Duration}
     */
    default Duration getDefaultWaitInterval(Duration timeout) {
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
