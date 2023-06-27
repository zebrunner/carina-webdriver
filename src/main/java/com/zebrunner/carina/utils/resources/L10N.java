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
package com.zebrunner.carina.utils.resources;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.asserts.SoftAssert;

import com.zebrunner.carina.utils.IWebElement;
import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;

/*
 * http://maven.apache.org/surefire/maven-surefire-plugin/examples/class-loading.html
 * Need to set useSystemClassLoader=false for maven surefire plugin to receive access to L10N files on CI by ClassLoader
 * <plugin>
 * <groupId>org.apache.maven.plugins</groupId>
 * <artifactId>maven-surefire-plugin</artifactId>
 * <version>3.0.0-M4</version>
 * <configuration>
 * <useSystemClassLoader>false</useSystemClassLoader>
 * </configuration>
 */
public class L10N {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Map<String, Locale> LOCALE_MAP = new ConcurrentHashMap<>();
    private static final String L10N_PREFIX = "{L10N:";
    private static final Properties MISSED_RESOURCES = new Properties();
    private static List<ResourceBundle> resBundles = new ArrayList<>();
    private static SoftAssert mistakes;

    private L10N() {
        // hide
    }

    /**
     * Load L10N resource bundle corresponding to a specific locale.
     * If called setLocale function in the test, must be called to reload resources
     */
    public static void load() {
        // #1679: L10N: made assertion threads dependent
        mistakes = new SoftAssert();
        List<String> loadedResources = new ArrayList<>();
        try {

            for (URL u : Resources.getResourceURLs(u -> {
                String s = u.getPath();
                boolean contains = s.contains(SpecialKeywords.L10N);
                if (contains) {
                    LOGGER.debug("L10N: file URL: {}", u);
                }
                return contains;
            })) {
                LOGGER.debug("Analyzing '{}' L10N resource for loading...", u);
                /*
                 * 2. Exclude localization resources like such L10N.messages_de, L10N.messages_ptBR etc...
                 * Note: we ignore valid resources if 3rd or 5th char from the end is "_". As designed :(
                 */
                String fileName = FilenameUtils.getBaseName(u.getPath());

                if (u.getPath().endsWith("L10N.class")
                        || u.getPath().endsWith("L10N$1.class")) {
                    // separate conditions to support core JUnit tests
                    continue;
                }

                if (fileName.lastIndexOf('_') == fileName.length() - 3
                        || fileName.lastIndexOf('_') == fileName.length() - 5) {
                    LOGGER.debug("'{}' resource IGNORED as it looks like localized resource!", fileName);
                    continue;
                }
                /*
                 * convert "file: <REPO>\target\classes\L10N\messages.properties" to "L10N.messages"
                 */
                String filePath = FilenameUtils.getPath(u.getPath());
                int index = filePath.indexOf(SpecialKeywords.L10N);

                if (index == -1) {
                    LOGGER.warn("Unable to find L10N pattern for {} resource!", u.getPath());
                    continue;
                }

                String resource = filePath.substring(
                        filePath.indexOf(SpecialKeywords.L10N))
                        .replaceAll("/", ".")
                        + fileName;

                if (!loadedResources.contains(resource)) {
                    loadedResources.add(resource);
                    try {
                        LOGGER.debug("Adding '{}' resource...", resource);
                        resBundles.add(ResourceBundle.getBundle(resource, getLocale()));
                        LOGGER.debug("Resource '{}' added.", resource);
                    } catch (MissingResourceException e) {
                        LOGGER.debug("No resource bundle for the " + resource + " can be found", e);
                    }
                } else {
                    LOGGER.debug("Requested resource '{}' is already loaded into the ResourceBundle!", resource);
                }
            }
            LOGGER.debug("init: L10N bundle size: {}", resBundles.size());
        } catch (IllegalArgumentException e) {
            LOGGER.debug("L10N folder with resources is missing!");
        }
    }

    /**
     * Replace default L10N resource bundle.
     *
     * @param resources ArrayList
     *
     */
    public static void load(List<ResourceBundle> resources) {
        // #1679: L10N: made assertion threads dependent
        mistakes = new SoftAssert();
        resBundles = resources;
    }

    /**
     * Return translated value by key for default locale.
     *
     * @param key String
     *
     * @return String
     */
    public static String getText(String key) {
        if (key.contains(L10N_PREFIX)) {
            key = key.replace(L10N_PREFIX, "");
            key = key.substring(0, key.length() - 1);
        }

        LOGGER.debug("getText: L10N bundle size: {}", resBundles.size());
        for (ResourceBundle bundle : resBundles) {
            try {
                String value = bundle.getString(key);
                if (bundle.getLocale().toString().equals(getLocale().toString())) {
                    return value;
                }
            } catch (MissingResourceException e) {
                // do nothing
            }
        }
        return key;
    }

    /**
     * Verify that ExtendedWebElement text is correctly localized.
     * Called automatically when an action is performed on an element
     * marked with the Localized annotation (getText, hover, etc.)
     * 
     * @param element IWebElement
     * @return boolean
     */
    public static boolean verify(IWebElement element) {
        if (!Configuration.getRequired(WebDriverConfiguration.Parameter.LOCALIZATION_TESTING, Boolean.class)) {
            return true;
        }

        String actualText = element.getText();
        String key = element.getName();

        String expectedText = getText(key);
        boolean isValid = actualText.contains(expectedText) && !expectedText.isEmpty();

        if (!isValid) {
            String error = "Expected: '" + expectedText + "', length=" + expectedText.length() +
                    ". Actual: '" + actualText + "', length=" + actualText.length() + ".";

            LOGGER.error(error);
            mistakes.fail(error);

            String newItem = key + "=" + actualText;
            LOGGER.info("Making new localization string: {}", newItem);
            MISSED_RESOURCES.setProperty(key, actualText);
        } else {
            LOGGER.debug("Found localization text '{}' in {} encoding: {}", actualText, getEncoding(), expectedText);
        }

        return isValid;
    }

    /**
     * Raise summarized asserts for mistakes in localization
     */
    public static void assertAll() {
        mistakes.assertAll();
    }

    /**
     * Override default locale globally.
     *
     * @param loc String
     */
    public static void setLocale(String loc) {
        setLocale(loc, false);
    }

    public static void setLocale(String loc, boolean currentTestOnly) {
        LOGGER.warn("Default locale: '{}' will be overwritten by {} {}.", Configuration.getRequired(WebDriverConfiguration.Parameter.LOCALE), loc,
                currentTestOnly ? "for current test only" : "globally");
        R.CONFIG.put(WebDriverConfiguration.Parameter.LOCALE.getKey(), loc, currentTestOnly);
    }

    /**
     * Get current locale
     * 
     * @return see {@link Locale}
     */
    public static Locale getLocale() {
        return getLocale(Configuration.getRequired(WebDriverConfiguration.Parameter.LOCALE));
    }

    /**
     * Flush missed localization resources to property file.
     */
    public static void flush() {
        if (MISSED_RESOURCES.size() == 0) {
            LOGGER.info("There are no new localization properties.");
            return;
        }

        Locale locale = getLocale();

        LOGGER.info("New localization for '{}'", locale);
        LOGGER.info("Properties: {}", MISSED_RESOURCES);

        String missedResorceFile = "missed_" + locale + ".properties";
        try (OutputStream fostream = new FileOutputStream(missedResorceFile);
                Writer ostream = new OutputStreamWriter(fostream, getEncoding());) {
            MISSED_RESOURCES.store(ostream, null);
        } catch (Exception e) {
            LOGGER.error("Unable to store missed resources: {}!", missedResorceFile, e);
        }
        MISSED_RESOURCES.clear();
    }

    private static String getEncoding() {
        return Configuration.getRequired(WebDriverConfiguration.Parameter.LOCALIZATION_ENCODING).toUpperCase();
    }

    private static Locale getLocale(String locale) {
        if (!LOCALE_MAP.containsKey(locale)) {
            String[] localeSetttings = locale.trim().split("_");
            String lang = "";
            String country = "";
            lang = localeSetttings[0];
            if (localeSetttings.length > 1) {
                country = localeSetttings[1];
            }
            LOCALE_MAP.put(locale, new Locale(lang, country));
        }
        return LOCALE_MAP.get(locale);
    }

}
