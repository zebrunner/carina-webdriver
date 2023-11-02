package com.zebrunner.carina.webdriver.config;

import com.zebrunner.carina.utils.Configuration;
import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.utils.exception.InvalidConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class WebDriverConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Set<String> RETRY_NEW_DRIVER_SESSION_IGNORE_MESSAGES = Collections.synchronizedSet(new HashSet<>());
    private static final String LANGUAGE_TAG = "language_tag";

    /**
     * Add root exception messages, when they appear (when create session) we should retry new session command
     * 
     * @param messages root error message(s)
     */
    public static void addIgnoredNewSessionErrorMessages(String... messages) {
        RETRY_NEW_DRIVER_SESSION_IGNORE_MESSAGES.addAll(Arrays.asList(messages));
    }

    /**
     * <b>For internal usage only</b>
     * 
     * @return {@link Set}
     */
    public static Set<String> getIgnoredNewSessionErrorMessages() {
        return RETRY_NEW_DRIVER_SESSION_IGNORE_MESSAGES;
    }

    /**
     * Get configuration locale<br>
     * Priority:<br>
     * 1 - language_tag parameter<br>
     * 2 - locale and language parameters (legacy)<br>
     * 
     * @return {@link Locale}
     */
    public static Locale getLocale() {
        Locale locale;
        String languageTag = getConfigurationValue(LANGUAGE_TAG);
        if (StringUtils.isNotBlank(languageTag)) {
            locale = Locale.forLanguageTag(languageTag);
            if (locale.getCountry().isEmpty()) {
                throw new InvalidConfigurationException("'language_tag' parameter should contains country.");
            }
            if (locale.getLanguage().isEmpty()) {
                throw new InvalidConfigurationException("'language_tag' parameter should contains language.");
            }
            return locale;
        } else {
            // this legacy logic do not support languages with scripts
            // could be US or en_US
            String localeAsString = Configuration.get(Configuration.Parameter.LOCALE);
            String[] arr = StringUtils.split(localeAsString, "_");
            if (arr.length == 2) {
                locale = new Locale.Builder()
                        .setLanguage(arr[0])
                        .setRegion(arr[1])
                        .build();
            } else if (arr.length == 1) {
                locale = new Locale.Builder()
                        .setLanguage(Configuration.get(Configuration.Parameter.LANGUAGE))
                        .setRegion(localeAsString)
                        .build();
            } else {
                throw new InvalidConfigurationException("Provided locale parameter is invalid: " + localeAsString);
            }
        }
        return locale;
    }

    private static String getConfigurationValue(String param) {
        String value = R.CONFIG.get(param);
        return !(value == null || value.equalsIgnoreCase(SpecialKeywords.NULL)) ? value : StringUtils.EMPTY;
    }
}
