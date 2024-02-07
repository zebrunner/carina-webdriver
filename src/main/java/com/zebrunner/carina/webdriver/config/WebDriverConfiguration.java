package com.zebrunner.carina.webdriver.config;

import java.security.InvalidParameterException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.zebrunner.carina.utils.exception.InvalidConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationUtils;
import org.openqa.selenium.remote.CapabilityType;

import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.config.ConfigurationOption;
import com.zebrunner.carina.utils.config.IParameter;
import com.zebrunner.carina.webdriver.core.capability.AbstractCapabilities;
import com.zebrunner.carina.webdriver.core.capability.DriverType;

import io.appium.java_client.internal.CapabilityHelpers;
import io.appium.java_client.remote.MobileCapabilityType;

public final class WebDriverConfiguration extends Configuration {
    private static final Map<String, Duration> RETRY_NEW_DRIVER_SESSION_IGNORE_MESSAGES = new ConcurrentHashMap<>();

    private static final String CAPABILITIES_PREFIX = "capabilities.";

    public enum Parameter implements IParameter {

        /**
         * Base application URL.<br>
         * Example: {@code https://zebrunner.com}
         */
        URL("url"),

        /**
         * Browser for testing.<br>
         * Example: {@code chrome, firefox, MicrosoftEdge, safari}
         */
        BROWSER("browser"),

        /**
         * Browser language.<br>
         * Example: {@code es, fr}
         */
        BROWSER_LANGUAGE("browser_language"),

        /**
         * Selenium/Appium server URL.<br>
         * Example: {@code http://localhost:4444/wd/hub}
         */
        SELENIUM_URL("selenium_url"),

        /**
         * Comma-separated list of extra driver listeners.
         * Listeners provide extra custom actions for WebDriver and have to be
         * the instances of WebDriverEventListener.<br>
         * Example: com.some_company.core.EventListener
         */
        DRIVER_EVENT_LISTENERS("driver_event_listeners"),

        /**
         * Max number of drivers per thread. <b>Default value: {@code 3}</b>
         */
        MAX_DRIVER_COUNT("max_driver_count"),

        /**
         * Chrome arguments.<br>
         * Example: {@code --test-type, -–disable-device-orientation}
         */
        CHROME_ARGS("chrome_args"),

        /**
         * Chrome preferences.<br>
         * Example: {@code profile.default_content_setting_values.notifications=2, profile.managed_default_content_settings.images=2}
         */
        CHROME_EXPERIMENTAL_OPTS("chrome_experimental_opts"),

        /**
         * Chrome mobile emulation options.<br>
         * Example:{@code deviceName=Nexus 5, userAgent=Mozilla/5.0 (Linux; Android 4.2.1; en-us; Nexus 5 Build/JOP40D) 
         * AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19}
         */
        CHROME_MOBILE_EMULATION_OPTS("chrome_mobile_emulation_opts"),

        /**
         * If it is true we will do driver.close() before driver.quit(). <b>Default: {@code false}</b>
         */
        CHROME_CLOSURE("chrome_closure"),

        /**
         * Firefox arguments.<br>
         * Example: {@code --headless,--disable-notifications}
         */
        FIREFOX_ARGS("firefox_args"),

        /**
         * Firefox preferences.<br>
         * Example: {@code browser.startup.homepage=https://zebrunner.com}
         */
        FIREFOX_PREFERENCES("firefox_preferences"),

        /**
         * If it is true browsers will be running in headless mode. <b>Default: {@code false}</b>
         */
        HEADLESS("headless"),

        /**
         * Hostname of the proxy server.
         */
        PROXY_HOST("proxy_host"),

        /**
         * Port number. <b>Default: {@code 0}</b>
         */
        PROXY_PORT("proxy_port"),

        /**
         * Comma-separated list of internet protocols used to carry the connection
         * information from the source requesting the connection to the destination
         * for which the connection was requested. <b>Default: {@code http,https}</b><br>
         * Example: {@code http,https,socks}
         */
        PROXY_PROTOCOLS("proxy_protocols"),

        /**
         * Excluded hostname(s) for communication via proxy. Available only when proxy_host and proxy_port are declared!
         * Example: {@code localhost.example.com,localhost2.example.com}
         */
        NO_PROXY("no_proxy"),

        /**
         * @deprecated all logic associated with this parameter has been removed.
         */
        @Deprecated(forRemoval = true, since = "1.0.5")
        BROWSERUP_PROXY("browserup_proxy"),

        /**
         * Proxy type. Available proxy types:<br>
         * DIRECT - all connections are created directly, without any proxy involved<br>
         * MANUAL - manual proxy settings (values will be getted from the configuration parameters like proxy_host, proxy_port and so on)<br>
         * PAC - proxy auto-configuration from URL<br>
         * AUTODETECT - proxy auto-detection (presumably with WPAD) (see
         * https://developer.chrome.com/docs/extensions/reference/proxy/#proxy-modes)<br>
         * SYSTEM - the proxy configuration is taken from the operating system<br>
         * ZEBRUNNER - Zebrunner proxy
         */
        PROXY_TYPE("proxy_type"),

        /**
         * Specifies the URL to be used for proxy auto-configuration.
         * Expected format is {@code http://hostname.com:1234/pacfile}.
         * However, the value of this parameter may be local path with pac file (depends on proxy_pac_local param).
         * Ignored if proxy_type is not PAC.
         */
        PROXY_AUTOCONFIG_URL("proxy_autoconfig_url"),

        /**
         * If the parameter value is true then it is assumed that proxy_autoconfig_url contains the path to the file on the local machine.
         * <b>Default: {@code false}</b>. Ignored if proxy_type is not PAC.
         */
        PROXY_PAC_LOCAL("proxy_pac_local"),

        /**
         * Proxy Zebrunner arguments. Used by {@link com.zebrunner.carina.webdriver.proxy.ZebrunnerProxyBuilder}.
         */
        PROXY_ZEBRUNNER_ARGS("proxy_zebrunner_args"),

        /**
         * @deprecated all logic associated with this parameter has been removed.
         */
        @Deprecated(forRemoval = true, since = "1.0.5")
        BROWSERUP_PORT("browserup_port"),

        /**
         * @deprecated all logic associated with this parameter has been removed.
         */
        @Deprecated(forRemoval = true, since = "1.0.5")
        BROWSERUP_PORTS_RANGE("browserup_ports_range"),

        /**
         * @deprecated all logic associated with this parameter has been removed.
         */
        @Deprecated(forRemoval = true, since = "1.0.5")
        BROWSERUP_MITM("browserup_disabled_mitm"),

        /**
         * If set true we will generate screenshots automatically after major driver actions. <b>Default: {@code false}</b>
         */
        AUTO_SCREENSHOT("auto_screenshot"),

        /**
         * system internal property to manage screenshot generation for test and driver failures
         * 
         * @deprecated all logic associated with this parameter has been removed.
         */
        @Deprecated(forRemoval = true, since = "1.0.5")
        ERROR_SCREENSHOT("error_screenshot"),

        /**
         * Global switch for allowing full size screenshots on failures. <b>Default: {@code false}</b>
         */
        ALLOW_FULLSIZE_SCREENSHOT("allow_fullsize_screenshot"),

        /**
         * Timeout is seconds to wait for a certain condition to occur before proceeding further in the code.
         * <b>Default: {@code 20}</b>
         */
        EXPLICIT_TIMEOUT("explicit_timeout"),
        /**
         * Timeout is seconds to read response from Selenium/Appium. <b>Default: {@code 660}</b>
         */
        READ_TIMEOUT("read_timeout"),
        /**
         * The enabled parameter prevents downloading dialog and downloading
         * a file automatically into the test artifact folder.
         * The feature is supported for Chrome and Firefox.<b>Default: false</b>
         */
        AUTO_DOWNLOAD("auto_download"),

        /**
         * MIME types / Internet Media Types. The parameter is needed only to configure auto-downloading for Firefox.<br>
         * Example: {@code application/pdf}
         */
        AUTO_DOWNLOAD_APPS("auto_download_apps"),

        /**
         * Custom unified path for auto-downloaded artifacts for all tests.
         * <b>Default: NULL to download into the unique test artifacts location.</b>
         */
        CUSTOM_ARTIFACTS_FOLDER("custom_artifacts_folder"),

        /**
         * Timeout interval in ms between calling HTML DOM for the element.
         * <b>Default: 100. For mobile automation specify in between 500-1000</b>
         */
        RETRY_INTERVAL("retry_interval"),

        /**
         * Screenshot width. Works only with the big_screen_height.
         */
        BIG_SCREEN_WIDTH("big_screen_width"),

        /**
         * Screenshot height. Works only with the big_screen_width.
         */
        BIG_SCREEN_HEIGHT("big_screen_height"),

        /**
         * Number of extra attempts to create a driver. <b>Default: 0 means that there will be no extra attempts.</b>
         */
        INIT_RETRY_COUNT("init_retry_count"),

        /**
         * Interval in seconds between the attempts to create a driver. <b>Default: 1</b>
         */
        INIT_RETRY_INTERVAL("init_retry_interval"),

        /**
         * Locale for using by L10N feature. <b>Default: {@code en_US}</b><br>
         * 
         * @see <a href="https://zebrunner.github.io/carina/advanced/localization">documentation</a>
         */
        LOCALE("locale"),

        /**
         * Encoding for generation of new/missed localization resources. <b>Default: {@code utf-8}</b><br>
         * Example: {@code UTF-8}
         */
        LOCALIZATION_ENCODING("localization_encoding"),

        /**
         * Enables auto verification for elements that are marked with @Localized annotations. <b>Default: {@code false}</b>
         */
        LOCALIZATION_TESTING("localization_testing"),
        /**
         * Determines how we detects appearing of web elements on page. Possible values:
         * BY_PRESENCE, BY_VISIBILITY, BY_PRESENCE_OR_VISIBILITY. <b>Default: {@code BY_PRESENCE_OR_VISIBILITY}</b>
         */
        ELEMENT_LOADING_STRATEGY("element_loading_strategy"),

        /**
         * Determines how carina detects whether the expected page is opened. Possible values:
         * BY_ELEMENT, BY_URL, BY_URL_AND_ELEMENT. <b>Default: {@code BY_URL_AND_ELEMENT}</b>
         */
        PAGE_OPENING_STRATEGY("page_opening_strategy"),

        /**
         * Specifies whether to search for pages implementations in dependencies. <b>Default: {@code false}</b><br>
         */
        PAGE_RECURSIVE_REFLECTION("page_recursive_reflection"),

        /**
         * todo add doc
         */
        UNINSTALL_RELATED_APPS("uninstall_related_apps"),

        /**
         * For setDeviceDefaultTimeZoneLanguage method. <b>Default: {@code GMT}</b>
         * todo add description
         */
        DEFAULT_DEVICE_TIMEZONE("default_device_timezone"),

        /**
         * For setDeviceDefaultTimeZoneLanguage method. <b>Default: {@code 24}</b>
         * todo add description
         */
        DEFAULT_DEVICE_TIME_FORMAT("default_device_time_format"),

        /**
         * For setDeviceDefaultTimeZoneLanguage method. <b>Default: {@code en_US}</b>
         * todo add description
         */
        DEFAULT_DEVICE_LANGUAGE("default_device_language"),

        /**
         * For setLocaleAndLanguage method from AbstractCapabilities class.
         * todo add description
         */
        LANGUAGE("language"),

        /**
         * language tag
         * todo add description
         */
        LANGUAGE_TAG("language_tag"),

        /**
         * todo add description
         */
        SCROLL_TO_ELEMENT_Y_OFFSET("scroll_to_element_y_offset"),

        /**
         * todo add description
         */
        MAX_NEW_SESSION_QUEUE("max_new_session_queue");

        private final String name;

        Parameter(String name) {
            this.name = name;
        }

        @Override
        public String getKey() {
            return name;
        }
    }

    @Override
    public String toString() {
        String description = "";
        description = asString(Parameter.values()).orElse("");
        Optional<String> capabilities = getCapabilitiesConfiguration();
        if (capabilities.isPresent()) {
            description += capabilities.get();
        }
        if (description.isEmpty()) {
            return "";
        }
        return "\n=========== WebDriver configuration ===========\n" +
                description;
    }

    private static Optional<String> getCapabilitiesConfiguration() {
        StringBuilder sb = new StringBuilder();
        sb.append("-------------- Driver capabilities ------------\n");
        Map<String, String> properties = (Map<String, String>) new HashMap(R.CONFIG.getProperties());
        Map<String, Object> capabilities = AbstractCapabilities.getGlobalCapabilities(properties);
        capabilities.putAll(AbstractCapabilities.getEnvCapabilities(properties));
        capabilities.forEach((key, value) -> sb.append(String.format("%s=%s%n", key, value)));
        return sb.length() > 0 ? Optional.of(sb.toString()) : Optional.empty();
    }

    public static Optional<String> getAppiumCapability(String capabilityName) {
        return Optional.ofNullable(get(CAPABILITIES_PREFIX + CapabilityHelpers.APPIUM_PREFIX + capabilityName)
                .orElse(get(CAPABILITIES_PREFIX + "appium:options." + capabilityName)
                        .orElse(get(CAPABILITIES_PREFIX + capabilityName).orElse(null))));
    }

    public static Optional<String> getAppiumCapability(String capabilityName, ConfigurationOption... options) {
        return Optional.ofNullable(get(CAPABILITIES_PREFIX + CapabilityHelpers.APPIUM_PREFIX + capabilityName, options)
                .orElse(get(CAPABILITIES_PREFIX + "appium:options." + capabilityName, options)
                        .orElse(get(CAPABILITIES_PREFIX + capabilityName, options).orElse(null))));
    }

    public static Optional<String> getZebrunnerCapability(String capabilityName) {
        return Optional.ofNullable(get(CAPABILITIES_PREFIX + "zebrunner:" + capabilityName)
                .orElseGet(() -> get(CAPABILITIES_PREFIX + "zebrunner:options." + capabilityName)
                        .orElseGet(() -> get(CAPABILITIES_PREFIX + capabilityName).orElse(null))));
    }

    public static Optional<String> getZebrunnerCapability(String capabilityName, ConfigurationOption... options) {
        return Optional.ofNullable(get(CAPABILITIES_PREFIX + "zebrunner:" + capabilityName, options)
                .orElseGet(() -> get(CAPABILITIES_PREFIX + "zebrunner:options." + capabilityName, options)
                        .orElseGet(() -> get(CAPABILITIES_PREFIX + capabilityName, options).orElse(null))));
    }

    public static Optional<String> getCapability(String capabilityName) {
        return get(CAPABILITIES_PREFIX + capabilityName);
    }

    public static Optional<String> getCapability(String capabilityName, ConfigurationOption... options) {
        return get(CAPABILITIES_PREFIX + capabilityName, options);
    }

    public static Optional<String> getCapability(String capabilityName, String providerPrefix, ConfigurationOption... options) {
        Optional<String> value = get(CAPABILITIES_PREFIX + providerPrefix + capabilityName, options);
        if (value.isPresent()) {
            return value;
        }
        return get(CAPABILITIES_PREFIX + capabilityName, options);
    }

    /**
     * Takes browserName from browser configuration parameter
     *
     * @return browser name
     */
    public static Optional<String> getBrowser() {
        AtomicReference<String> browser = new AtomicReference<>(null);
        get(Parameter.BROWSER).ifPresent(browser::set);
        getCapability(CapabilityType.BROWSER_NAME).ifPresent(browser::set);
        return Optional.ofNullable(browser.get());
    }

    /**
     * Returns driver type depends on platform and browser
     *
     * @return driver type
     */
    public static DriverType getDriverType() {
        Optional<String> platform = getCapability(CapabilityType.PLATFORM_NAME);
        Optional<String> browserName = getBrowser();

        if (platform.isPresent() && (SpecialKeywords.ANDROID.equalsIgnoreCase(platform.get()) ||
                SpecialKeywords.IOS.equalsIgnoreCase(platform.get()) ||
                SpecialKeywords.TVOS.equalsIgnoreCase(platform.get()))) {
            return DriverType.MOBILE;
        }

        if (browserName.isPresent()) {
            return DriverType.DESKTOP;
        }

        if (platform.isEmpty()) {
            throw new IllegalArgumentException(String.format("Cannot detect driver type. Capabilities '%s', '%s', '%s' are empty.",
                    CapabilityType.BROWSER_NAME, CapabilityType.PLATFORM_NAME, MobileCapabilityType.UDID));
        }

        if (SpecialKeywords.WINDOWS.equalsIgnoreCase(platform.get())) {
            return DriverType.WINDOWS;
        }

        if (SpecialKeywords.MAC.equalsIgnoreCase(platform.get())) {
            return DriverType.MAC;
        }

        throw new IllegalArgumentException(String.format("Cannot detect driver type. Unsupported platform: '%s'", platform.get()));
    }

    /**
     * Add root exception messages that should be ignored during session startup.
     * So when we try to create session, and we got such exception,
     * we will retry new session command.
     *
     * @param messages root exception message(s) with timeouts
     */
    @SuppressWarnings("unused")
    public static void addIgnoredNewSessionErrorMessages(Map<String, Duration> messages) {
        messages.forEach((key, value) -> {
            if (value != null && value.isNegative()) {
                throw new InvalidParameterException(
                        String.format("Invalid duration for new session error: '%s'. Duration could not be negative.", key));
            }
            RETRY_NEW_DRIVER_SESSION_IGNORE_MESSAGES.put(key, DurationUtils.zeroIfNull(value));
        });
    }

    /**
     * Add root exception messages that should be ignored during session startup.
     * So when we try to create session, and we got such exception,
     * we will retry new session command.
     *
     * @param messages root exception message(s). Be careful, all messages added using this method will be retried infinitely
     */
    @SuppressWarnings("unused")
    public static void addIgnoredNewSessionErrorMessages(String... messages) {
        Arrays.asList(messages).forEach(key -> RETRY_NEW_DRIVER_SESSION_IGNORE_MESSAGES.put(key, Duration.ZERO));
    }

    /**
     * <b>For internal usage only</b>
     *
     * @return {@link Set}
     */
    public static Map<String, Duration> getIgnoredNewSessionErrorMessages() {
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
        Optional<String> languageTag = Configuration.get(Parameter.LANGUAGE_TAG);
        if (languageTag.isPresent()) {
            locale = Locale.forLanguageTag(languageTag.get());
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
            String localeAsString = Configuration.getRequired(WebDriverConfiguration.Parameter.LOCALE);
            String[] arr = StringUtils.split(localeAsString, "_");
            if (arr.length == 2) {
                locale = new Locale.Builder()
                        .setLanguage(arr[0])
                        .setRegion(arr[1])
                        .build();
            } else if (arr.length == 1) {
                locale = new Locale.Builder()
                        .setLanguage(Configuration.getRequired(WebDriverConfiguration.Parameter.LANGUAGE))
                        .setRegion(localeAsString)
                        .build();
            } else {
                throw new InvalidConfigurationException("Provided locale parameter is invalid: " + localeAsString);
            }
        }
        return locale;
    }
}
