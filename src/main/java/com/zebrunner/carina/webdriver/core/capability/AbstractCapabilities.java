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
package com.zebrunner.carina.webdriver.core.capability;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.zebrunner.carina.webdriver.proxy.ZebrunnerProxyBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.openqa.selenium.InvalidArgumentException;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.remote.CapabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.exception.InvalidConfigurationException;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;

import javax.annotation.Nullable;

public abstract class AbstractCapabilities<T extends MutableCapabilities> {
    // TODO: [VD] reorganize in the same way Firefox profiles args/options if any and review other browsers
    // support customization for Chrome args and options

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Pattern CAPABILITY_WITH_TYPE_PATTERN = Pattern.compile("^(?<name>.+)(?<type>\\[.+\\])$");
    private static final List<String> W3C_STRING_CAPABILITIES = List.of(CapabilityType.BROWSER_NAME,
            CapabilityType.BROWSER_VERSION, CapabilityType.PLATFORM_NAME,
            CapabilityType.PAGE_LOAD_STRATEGY, CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR);
    private static final List<String> W3C_BOOLEAN_CAPABILITIES = List.of(CapabilityType.ACCEPT_INSECURE_CERTS, CapabilityType.SET_WINDOW_RECT,
            CapabilityType.STRICT_FILE_INTERACTABILITY);

    private static final String ZEBRUNNER_MITMPROXY_ENABLED_CAPABILITY = "zebrunner:Mitm";
    private static final String ZEBRUNNER_MITMPROXY_ARGS_CAPABILITY = "zebrunner:MitmArgs";
    private static final String ZEBRUNNER_MITMPROXY_TYPE_CAPABILITY = "zebrunner:MitmType";

    /**
     * Get capabilities from the configuration ({@link R#CONFIG}).
     * Additional capabilities can also be added (depends on implementation).
     *
     * @return see {@link T}
     */
    public abstract T getCapability(String testName);

    protected void addAppiumProxy(T capabilities) {
        Optional<String> proxyType = Configuration.get(WebDriverConfiguration.Parameter.PROXY_TYPE);

        if (proxyType.isEmpty()) {
            return;
        }

        if (proxyType.get().equalsIgnoreCase("Zebrunner")) {
            capabilities.setCapability(ZEBRUNNER_MITMPROXY_ENABLED_CAPABILITY, true);
            Configuration.get(WebDriverConfiguration.Parameter.PROXY_ZEBRUNNER_ARGS)
                    .ifPresent(args -> capabilities.setCapability(ZEBRUNNER_MITMPROXY_ARGS_CAPABILITY, args));
            Configuration.get(ZebrunnerProxyBuilder.PROXY_TYPE_PARAMETER)
                    .ifPresent(args -> capabilities.setCapability(ZEBRUNNER_MITMPROXY_TYPE_CAPABILITY, args));
            return;
        }
        throw new InvalidConfigurationException(String.format("Invalid proxy type: %s", proxyType.get()));

    }

    /**
     * Add proxy capability. Should only be used for Selenium session only.
     *
     * @param capabilities see {@link T}
     */
    protected void addProxy(T capabilities) {
        Optional<String> proxyType = Configuration.get(WebDriverConfiguration.Parameter.PROXY_TYPE);

        if (proxyType.isEmpty()) {
            return;
        }

        if (proxyType.get().equalsIgnoreCase("Zebrunner")) {
            capabilities.setCapability(ZEBRUNNER_MITMPROXY_ENABLED_CAPABILITY, true);
            Configuration.get(WebDriverConfiguration.Parameter.PROXY_ZEBRUNNER_ARGS)
                    .ifPresent(args -> capabilities.setCapability(ZEBRUNNER_MITMPROXY_ARGS_CAPABILITY, args));
            Configuration.get(ZebrunnerProxyBuilder.PROXY_TYPE_PARAMETER)
                    .ifPresent(args -> capabilities.setCapability(ZEBRUNNER_MITMPROXY_TYPE_CAPABILITY, args));
            return;
        }

        Proxy proxy = new Proxy();
        switch (Proxy.ProxyType.valueOf(proxyType.get())) {
        case DIRECT:
            proxy.setProxyType(Proxy.ProxyType.DIRECT);
            break;
        case MANUAL:
            proxy = getManualSeleniumProxy();
            break;
        case PAC:
            String autoConfigURL = Configuration.get(WebDriverConfiguration.Parameter.PROXY_AUTOCONFIG_URL)
                    .orElseThrow(() -> new InvalidConfigurationException(
                            "ProxyType is PAC, but proxy_autoconfig_url is empty. Please, provide autoconfig url"));
            if (Configuration.get(WebDriverConfiguration.Parameter.PROXY_PAC_LOCAL, Boolean.class).orElse(false)) {
                Path path = Path.of(autoConfigURL);
                if (!Files.exists(path)) {
                    throw new InvalidConfigurationException("'proxy_pac_local' parameter value is true, "
                            + "but there is no file on the path specified in parameter 'proxy_autoconfig_url'. Path: " + path);
                }
                if (Files.isDirectory(path)) {
                    throw new InvalidConfigurationException("'proxy_pac_local' parameter value is true, "
                            + "but the path specified in the 'proxy_pac_local' parameter does not point to the file, "
                            + "but to the directory. Specify the path to the file. Path: " + path);
                }
                autoConfigURL = encodePAC(path);
            }
            proxy.setProxyAutoconfigUrl(autoConfigURL);
            break;
        case UNSPECIFIED:
            // do nothing - unspecified is set by default
            break;
        case AUTODETECT:
            proxy.setAutodetect(true);
            break;
        case SYSTEM:
            proxy.setProxyType(Proxy.ProxyType.SYSTEM);
            break;
        default:
            throw new InvalidConfigurationException("ProxyType was not detected.");
        }
        capabilities.setCapability(CapabilityType.PROXY, proxy);
    }

    /**
     * Encode PAC file to encoded link with Base64
     *
     * @param pathToPac {@link Path} to the pac file
     * @return encoded link to pac file
     * @throws UncheckedIOException if error happens when try to read/encode content of the file
     */
    private static String encodePAC(Path pathToPac) {
        try {
            return String.format("data:application/x-javascript-config;base64,%s",
                    new String(Base64.getEncoder().encode(Files.readAllBytes(pathToPac))));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Proxy getManualSeleniumProxy() {
        String proxyHost = Configuration.get(WebDriverConfiguration.Parameter.PROXY_HOST)
                .orElseThrow(() -> new InvalidConfigurationException(
                        "Provided 'MANUAL' proxy type, but 'proxy_host' parameter is not specified."));
        String proxyPort = Configuration.get(WebDriverConfiguration.Parameter.PROXY_PORT)
                .orElseThrow(() -> new InvalidConfigurationException(
                        "Provided 'MANUAL' proxy type, but 'proxy_port' parameter is not specified."));
        List<String> protocols = Arrays.asList(Configuration.get(WebDriverConfiguration.Parameter.PROXY_PROTOCOLS).orElseThrow(
                () -> new InvalidConfigurationException(
                        "Provided 'MANUAL' proxy type, but 'proxy_protocols' parameter is not specified."))
                .split("[\\s,]+"));

        Proxy proxy = new Proxy();
        String proxyAddress = String.format("%s:%s", proxyHost, proxyPort);
        if (protocols.contains("http")) {
            LOGGER.info("Http proxy will be set: {}:{}", proxyHost, proxyPort);
            proxy.setHttpProxy(proxyAddress);
        }
        if (protocols.contains("https")) {
            LOGGER.info("Https proxy will be set: {}:{}", proxyHost, proxyPort);
            proxy.setSslProxy(proxyAddress);
        }
        if (protocols.contains("ftp")) {
            LOGGER.info("FTP proxy will be set: {}:{}", proxyHost, proxyPort);
            proxy.setFtpProxy(proxyAddress);
        }
        if (protocols.contains("socks")) {
            LOGGER.info("Socks proxy will be set: {}:{}", proxyHost, proxyPort);
            proxy.setSocksProxy(proxyAddress);
        }
        Configuration.get(WebDriverConfiguration.Parameter.NO_PROXY).ifPresent(proxy::setNoProxy);
        return proxy;
    }

    /**
     * Add capabilities from configuration {@link R#CONFIG}.
     *
     * @param options see {@link T}
     */
    protected void addConfigurationCapabilities(T options) {
        addPropertiesCapabilities(options, R.CONFIG.getProperties());
    }

    /**
     * <b>For internal usage only</b>
     */
    public static Map<String, Object> getGlobalCapabilities(Map<String, String> props) {
        return props.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith("capabilities."))
                .filter(entry -> entry.getValue() != null)
                .map(entry -> {
                    MutablePair<String, String> pair = new MutablePair<>();
                    pair.setLeft(entry.getKey().replaceFirst("capabilities.", ""));
                    pair.setRight(entry.getValue());
                    return pair;
                })
                .map(p -> parseCapabilityType(p.getLeft(), p.getRight()))
                .filter(Objects::nonNull) // null-safe
                .collect(Collectors.toMap(MutablePair::getLeft, MutablePair::getRight));
    }

    /**
     * <b>For internal usage only</b>
     */
    public static Map<String, Object> getEnvCapabilities(Map<String, String> props) {
        Optional<String> env = Configuration.get(Configuration.Parameter.ENV);
        if (env.isEmpty()) {
            return Map.of();
        }
        return props.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(env.get() + ".capabilities."))
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> !entry.getValue().isBlank())
                .map(entry -> {
                    MutablePair<String, String> pair = new MutablePair<>();
                    pair.setLeft(entry.getKey().replaceFirst(env.get() + ".capabilities.", ""));
                    pair.setRight(entry.getValue());
                    return pair;
                })
                .map(p -> parseCapabilityType(p.getLeft(), p.getRight()))
                .filter(Objects::nonNull) // null-safe
                .collect(Collectors.toMap(MutablePair::getLeft, MutablePair::getRight));
    }

    /**
     * Add capabilities from properties
     *
     * @param options see {@link C}
     * @param props see {@link Properties}
     */
    static <C extends MutableCapabilities> void addPropertiesCapabilities(C options, Properties props) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Map<String, String> properties = new HashMap(props);
        Map<String, Object> capabilities = getGlobalCapabilities(properties);
        capabilities.putAll(getEnvCapabilities(properties));
        for (Map.Entry<String, Object> entry : capabilities.entrySet()) {
            List<String> names = Arrays.asList(entry.getKey().split("\\."));

            // TODO add support of any nesting
            if (names.isEmpty()) {
                // should never happen
                throw new RuntimeException("Something went wrong when try to create capabilities from configuration.");
            } else if (names.size() == 1) {
                options.setCapability(names.get(0), entry.getValue());
            } else if (names.size() == 2) {
                HashMap<String, Object> nestCapability = new HashMap<>();
                if (options.getCapability(names.get(0)) != null) {
                    // If we already have inner capability, we think that it is HashMap<String, Object> (custom capabilities)
                    nestCapability = (HashMap<String, Object>) options.getCapability(names.get(0));
                }

                nestCapability.put(names.get(1), entry.getValue());
                options.setCapability(names.get(0), nestCapability);
            } else if (names.size() == 3) {
                HashMap<String, Object> nestCapability = new HashMap<>();
                HashMap<String, Object> secondNestCapability = new HashMap<>();

                if (options.getCapability(names.get(0)) != null) {
                    // If we already have inner capability, we think that it is HashMap<String, Object>
                    nestCapability = (HashMap<String, Object>) options.getCapability(names.get(0));
                    if (nestCapability.containsKey(names.get(1))) {
                        secondNestCapability = (HashMap<String, Object>) nestCapability.get(names.get(1));
                    }
                }
                secondNestCapability.put(names.get(2), entry.getValue());
                nestCapability.put(names.get(1), secondNestCapability);
                options.setCapability(names.get(0), nestCapability);
            } else {
                // Let's hope it won't be needed.
                throw new UnsupportedOperationException("At the moment nesting of more than 3 capabilities is not supported. "
                        + "If you come across a situation in which this is necessary, please notify the Carina Support team.");
            }
        }
    }

    /**
     * Parse capability type.<br>
     * Result type depends on:
     * 1. If name of the capability ends with [string], [boolean] or [integer], then the capability value will be cast to it.
     * 2. If we have no information about type in capability name, result value depends on value.
     *
     * @param capabilityName name of the capability, for example {@code platformName} or {zebrunner:options.enableVideo[boolean]}
     * @param capabilityValue capability value. Since we take it from the configuration file, it is immediately of type String
     * @return {@link MutablePair}, where left is the capability name and right is the value, or null if capability is not w3c
     */
    static @Nullable MutablePair<String, Object> parseCapabilityType(String capabilityName, String capabilityValue) {
        if(StringUtils.containsAnyIgnoreCase(capabilityName, "enableLog", "enableVideo", "enableVNC", "provider", "memory", "cpu", "newSessionWaitTimeout") && !StringUtils.contains(capabilityName, ":")) {
            LOGGER.warn("Capability '{}' will not be added to the session because it does not comply with the w3c style.", capabilityName);
            return null;
        }
        MutablePair<String, Object> pair = new MutablePair<>();
        Matcher matcher = CAPABILITY_WITH_TYPE_PATTERN.matcher(capabilityName);
        if (matcher.find()) {
            String name = matcher.group("name");
            String type = matcher.group("type");
            Object value = null;
            if ("[string]".equalsIgnoreCase(type)) {
                value = capabilityValue;
            } else if ("[boolean]".equalsIgnoreCase(type)) {
                if ("true".equalsIgnoreCase(capabilityValue)) {
                    value = true;
                } else if ("false".equalsIgnoreCase(capabilityValue)) {
                    value = false;
                } else {
                    throw new InvalidConfigurationException(
                            String.format("Provided boolean type for '%s' capability, but it is not contains true or false value.", name));
                }
            } else if ("[integer]".equalsIgnoreCase(type)) {
                try {
                    value = Integer.parseInt(capabilityValue);
                } catch (NumberFormatException e) {
                    throw new InvalidConfigurationException(
                            String.format("Provided integer type for '%s' capability, but it is not contains integer value.", name));
                }
            } else {
                throw new InvalidConfigurationException(String.format("Unsupported '%s' type of '%s' capability.", type, name));
            }
            pair.setLeft(name);
            pair.setRight(value);
        } else {
            pair.setLeft(capabilityName);
            if (W3C_STRING_CAPABILITIES.contains(capabilityName)) {
                pair.setRight(capabilityValue);
            } else if (W3C_BOOLEAN_CAPABILITIES.contains(capabilityName)) {
                if ("true".equalsIgnoreCase(capabilityValue)) {
                    pair.setRight(true);
                } else if ("false".equalsIgnoreCase(capabilityValue)) {
                    pair.setRight(false);
                } else {
                    throw new InvalidArgumentException(String.format("Invalid value '%s' for '%s' capability. It should be true or false.",
                            capabilityValue, capabilityName));
                }
            } else if (isNumber(capabilityValue)) {
                pair.setRight(Integer.parseInt(capabilityValue));
            } else if ("true".equalsIgnoreCase(capabilityValue)) {
                pair.setRight(true);
            } else if ("false".equalsIgnoreCase(capabilityValue)) {
                pair.setRight(false);
            } else {
                pair.setRight(capabilityValue);
            }
        }
        return pair;
    }

    protected static boolean isNumber(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return false;
        }
        return true;
    }
}
