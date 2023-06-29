package com.zebrunner.carina.proxy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.openqa.selenium.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.exception.InvalidConfigurationException;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;

public final class ProxyUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ProxyUtils() {
        // hide
    }

    /**
     * Get Selenium proxy object<br>
     *
     * @return {@link Proxy} in {@link Optional} if according to the configuration it should have been created, {@link Optional#empty()} otherwise
     * @throws InvalidConfigurationException if the proxy configuration is incorrect
     */
    public static Optional<Proxy> getSeleniumProxy() {
        Optional<Proxy.ProxyType> proxyType = Configuration.get(WebDriverConfiguration.Parameter.PROXY_TYPE)
                .map(Proxy.ProxyType::valueOf);
        if (proxyType.isEmpty()) {
            return Optional.empty();
        }
        Proxy proxy = new Proxy();
        switch (proxyType.get()) {
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
        return Optional.of(proxy);
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

}
