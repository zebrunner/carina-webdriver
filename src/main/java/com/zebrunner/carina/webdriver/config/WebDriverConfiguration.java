package com.zebrunner.carina.webdriver.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class WebDriverConfiguration {
    private static final Set<String> RETRY_NEW_DRIVER_SESSION_IGNORE_MESSAGES = Collections.synchronizedSet(new HashSet<>());

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
}
