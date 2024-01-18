package com.zebrunner.carina.webdriver.helper;

import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.config.StandardConfigurationOption;
import com.zebrunner.carina.utils.encryptor.EncryptorUtils;
import com.zebrunner.carina.utils.messager.Messager;
import com.zebrunner.carina.webdriver.IDriverPool;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.listener.DriverListener;
import org.apache.commons.lang3.StringUtils;
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
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.json.JsonException;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.lang.invoke.MethodHandles;
import java.security.InvalidParameterException;
import java.time.Duration;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IPageActionsHelper extends IDriverPool, IWaitHelper {
    Logger I_PAGE_ACTIONS_HELPER_LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Go back in browser.
     */
    default void navigateBack() {
        getDriver().navigate().back();
        Messager.BACK.info();
    }

    /**
     * Refresh browser.
     */
    default void refresh() {
        refresh(getDefaultWaitTimeout());
    }

    /**
     * Refresh browser.
     */
    default void refresh(long timeout) {
        refresh(Duration.ofSeconds(timeout));
    }

    /**
     * Refresh browser.
     *
     * @param timeout long
     */
    default void refresh(Duration timeout) {
        Wait<WebDriver> wait = new FluentWait<>(getDriver())
                .pollingEvery(Duration.ofMillis(5000)) // there is no sense to refresh url address too often
                .withTimeout(timeout)
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
    default void acceptAlert() {
        Wait<WebDriver> wait = new WebDriverWait(getDriver(), getDefaultWaitTimeout(), getDefaultWaitInterval(getDefaultWaitTimeout()));
        try {
            wait.until((Function<WebDriver, Object>) dr -> isAlertPresent());
            getDriver().switchTo().alert().accept();
            Messager.ALERT_ACCEPTED.info("");
        } catch (Exception e) {
            Messager.ALERT_NOT_ACCEPTED.error("");
        }
    }

    /**
     * Cancels alert modal.
     */
    default void cancelAlert() {
        Wait<WebDriver> wait = new WebDriverWait(getDriver(), getDefaultWaitTimeout(), getDefaultWaitInterval(getDefaultWaitTimeout()));
        try {
            wait.until((Function<WebDriver, Object>) dr -> isAlertPresent());
            getDriver().switchTo().alert().dismiss();
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
    default boolean isAlertPresent() {
        try {
            getDriver().switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            return false;
        }
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
        final String decryptedURL = EncryptorUtils.decrypt(url);
        String script = "var d=document,a=d.createElement('a');a.target='_blank';a.href='%s';a.innerHTML='.';d.body.appendChild(a);return a";
        Object element = trigger(String.format(script, decryptedURL));
        if (element instanceof WebElement) {
            WebElement anchor = (WebElement) element;
            anchor.click();
            ((JavascriptExecutor) getDriver()).executeScript("var a=arguments[0];a.parentNode.removeChild(a);", anchor);
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

    /**
     * Open URL.
     *
     * @param url to open.
     */
    default void openURL(String url) {
        openURL(url, getDefaultWaitTimeout());
    }

    default void openURL(String url, long timeout) {
        openURL(url, Duration.ofSeconds(timeout));
    }

    /**
     * Open URL.
     *
     * @param url to open.
     * @param timeout long
     */
    default void openURL(String url, Duration timeout) {
        if (StringUtils.isBlank(url)) {
            throw new InvalidParameterException("Parameter 'url' could not be null or empty.");
        }
        String decryptedURL = EncryptorUtils.decrypt(StringUtils.trim(url));
        if (!(StringUtils.startsWithAny(decryptedURL, "http:", "https:"))) {
            decryptedURL = Configuration.getRequired(WebDriverConfiguration.Parameter.URL, StandardConfigurationOption.DECRYPT) + decryptedURL;
        }
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
            Assert.fail("Unable to open url during " + timeout.toSeconds() + "sec!");
        } catch (Exception e) {
            Assert.fail("Undefined error on open url detected: " + e.getMessage(), e);
        } finally {
            // restore default pageLoadTimeout driver timeout
            setPageLoadTimeout(drv, getPageLoadTimeout());
            I_PAGE_ACTIONS_HELPER_LOGGER.debug("finished driver.get call.");
        }
    }

    default void setPageLoadTimeout(WebDriver drv, long timeout) {
        setPageLoadTimeout(drv, Duration.ofSeconds(timeout));
    }

    /**
     * For internal usage only
     */
    default void setPageLoadTimeout(WebDriver drv, Duration timeout) {
        try {
            drv.manage().timeouts().pageLoadTimeout(timeout);
        } catch (UnsupportedCommandException e) {
            // TODO: review upcoming appium 2.0 changes
            I_PAGE_ACTIONS_HELPER_LOGGER.debug("Appium: Not implemented yet for pageLoad timeout!");
        } catch (WebDriverException e) {
            I_PAGE_ACTIONS_HELPER_LOGGER.debug("Appium: Not implemented yet for pageLoad timeout!", e);
        }
    }

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
            I_PAGE_ACTIONS_HELPER_LOGGER.debug("Command will be performed with the exception ignoring");
            return supplier.get();
        } catch (WebDriverException e) {
            I_PAGE_ACTIONS_HELPER_LOGGER.info("Webdriver exception has been fired. One more attempt to execute action.", e);
            I_PAGE_ACTIONS_HELPER_LOGGER.info(supplier.toString());
            return supplier.get();
        }
    }

    /**
     * For internal usage only
     */
    default Duration getPageLoadTimeout() {
        // #1705: limit pageLoadTimeout driver timeout by idleTimeout
        // if (!R.CONFIG.get("capabilities.idleTimeout").isEmpty()) {
        // long idleTimeout = R.CONFIG.getLong("capabilities.idleTimeout");
        // if (idleTimeout < timeout) {
        // timeout = idleTimeout;
        // }
        // }
        return Duration.ofSeconds(300);
    }
}
