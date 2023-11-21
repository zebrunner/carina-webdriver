package com.zebrunner.carina.webdriver.helper;

import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.messager.Messager;
import com.zebrunner.carina.webdriver.IDriverPool;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.listener.DriverListener;
import org.json.JSONObject;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.json.JsonException;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import java.time.Duration;
import java.util.Optional;

public interface IPageStorageHelper extends IDriverPool {
    String REDUX_STORE_STATE_BASE_PATH = "window.store.getState()";

    /*
     * Add cookie object into the driver
     *
     * @param Cookie
     */
    default void addCookie(Cookie cookie) {
        DriverListener.setMessages(Messager.ADD_COOKIE.getMessage(cookie.getName()),
                Messager.FAIL_ADD_COOKIE.getMessage(cookie.getName()));
        Wait<WebDriver> wait = new FluentWait<>(getDriver())
                .pollingEvery(Duration.ofMillis(Configuration.getRequired(WebDriverConfiguration.Parameter.RETRY_INTERVAL, Integer.class)))
                .withTimeout(Duration.ofSeconds(Configuration.getRequired(WebDriverConfiguration.Parameter.EXPLICIT_TIMEOUT, Integer.class)))
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
     * add an item to local storage
     *
     * @param name the name of the item to save
     * @param value the value of the item to save
     */
    default void addToLocalStorage(String name, String value) {
        ((JavascriptExecutor) getDriver())
                .executeScript("window.localStorage.setItem(arguments[0], arguments[0]);", name, value);
    }

    /**
     * gets the value of the stored item
     *
     * @param name the item that is stored
     * @return an optional containing the value of the stored item or an empty optional if nothing is stored.
     */
    default Optional<String> getValueFromLocalStorage(String name) {
        return Optional.ofNullable((String) ((JavascriptExecutor) getDriver())
                .executeScript("return window.localStorage.getItem(arguments[0]);", name));
    }

    /**
     * gets the name of a stored item via an index
     *
     * @param index the position of where the item is stored
     * @return an optional containing the name of the stored item for the index or an empty optional if
     *         nothing is at the given index.
     */
    default Optional<String> getNameFromLocalStorage(int index) {
        return Optional.ofNullable((String) ((JavascriptExecutor) getDriver())
                .executeScript("return window.localStorage.key(arguments[0]);", index));
    }

    /**
     * remove an item from local storage
     *
     * @param name the name of the item to remove
     */
    default void removeFromLocalStorage(String name) {
        ((JavascriptExecutor) getDriver()).executeScript("window.localStorage.removeItem(arguments[0]);", name);
    }

    /**
     * clears the local storage
     */
    default void clearLocalStorage() {
        ((JavascriptExecutor) getDriver()).executeScript("window.localStorage.clear();");
    }

    /**
     * Get the current states for the Redux stores
     *
     * @return all store states
     */
    default JSONObject getReduxStoreStates() {
        String response = (String) ((JavascriptExecutor) getDriver())
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
    default JSONObject getReduxStoreStateFor(String path) {
        String response = (String) ((JavascriptExecutor) getDriver())
                .executeScript("return JSON.stringify(arguments[0].arguments[1]);", REDUX_STORE_STATE_BASE_PATH, path);
        return new JSONObject(response);
    }

    /**
     * update a redux store with a particular state
     *
     * @param action the type of action you want to update
     * @param payload the payload to be updated
     */
    default void updateReduxStoreStateWith(String action, JSONObject payload) {
        ((JavascriptExecutor) getDriver())
                .executeScript("arguments[0]({type: arguments[1], payload: arguments[2]});");
    }

}
