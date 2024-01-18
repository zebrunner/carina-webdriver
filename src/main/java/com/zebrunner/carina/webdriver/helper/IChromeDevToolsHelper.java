package com.zebrunner.carina.webdriver.helper;

import static com.github.kklisura.cdt.services.impl.WebSocketServiceImpl.WEB_SOCKET_CONTAINER_FACTORY_PROPERTY;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.github.kklisura.cdt.services.WebSocketService;
import com.github.kklisura.cdt.services.config.ChromeDevToolsServiceConfiguration;
import com.github.kklisura.cdt.services.exceptions.WebSocketServiceException;
import com.github.kklisura.cdt.services.impl.ChromeDevToolsServiceImpl;
import com.github.kklisura.cdt.services.impl.WebSocketServiceImpl;
import com.github.kklisura.cdt.services.invocation.CommandInvocationHandler;
import com.github.kklisura.cdt.services.utils.ProxyUtils;
import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.config.StandardConfigurationOption;
import com.zebrunner.carina.webdriver.IDriverPool;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.listener.DriverListener;

public interface IChromeDevToolsHelper extends IDriverPool {
    Logger I_CHROME_DEV_TOOLS_HELPER_LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Get Browser Developer Tools
     * 
     * @return {@link ChromeDevToolsService} for browser
     */
    default ChromeDevToolsService browserChromeDevTools() {
        return getChromeDevTools("/");
    }

    /**
     * Get current page Developer Tools. Mainly useful when only one browser tab is open
     * 
     * @return {@link ChromeDevToolsService} for current page
     */
    default ChromeDevToolsService pageChromeDevTools() {
        return getChromeDevTools("/page");
    }

    /**
     * Get specified page Developer Tools. Allows to connect to concrete browser tab
     * 
     * @param id target id
     * @return {@link ChromeDevToolsService} for specified page
     */
    default ChromeDevToolsService pageChromeDevTools(String id) {
        Objects.requireNonNull(id);
        return getChromeDevTools("/page/" + id);
    }

    private ChromeDevToolsService getChromeDevTools(String endpoint) {
        // todo think about setting system property only once
        System.setProperty(WEB_SOCKET_CONTAINER_FACTORY_PROPERTY,
                "com.zebrunner.carina.webdriver.helper.ZebrunnerWebSocketContainerFactory");
        if (I_CHROME_DEV_TOOLS_HELPER_LOGGER.isDebugEnabled()) {
            System.setProperty("org.glassfish.tyrus.server.tracingType", "ALL");
        }
        try {
            String url = String.format("%s%s%s", Configuration.getRequired(WebDriverConfiguration.Parameter.SELENIUM_URL,
                    StandardConfigurationOption.DECRYPT)
                    .replace("/wd/hub", "/devtools/")
                    // pattern for Selenium URL with/without credentials
                    .replaceFirst("(^.+@)|(http(s)?:\\/\\/)", "wss://"),
                    DriverListener.castDriver(getDriver(), RemoteWebDriver.class).getSessionId(),
                    endpoint);

            WebSocketService webSocketService;
            try {
                webSocketService = WebSocketServiceImpl
                        .create(new URI(url));
            } catch (WebSocketServiceException e) {
                I_CHROME_DEV_TOOLS_HELPER_LOGGER.warn("Grid does not support 'wss' connection for DevTools. Trying to use 'ws' instead...");
                webSocketService = WebSocketServiceImpl
                        .create(new URI(StringUtils.replaceOnce(url, "wss://", "ws://")));
            }
            CommandInvocationHandler commandInvocationHandler = new CommandInvocationHandler();
            Map<Method, Object> commandsCache = new ConcurrentHashMap<>();
            ChromeDevToolsService devtools = ProxyUtils.createProxyFromAbstract(
                    ChromeDevToolsServiceImpl.class,
                    new Class[] { WebSocketService.class, ChromeDevToolsServiceConfiguration.class },
                    new Object[] { webSocketService, new ChromeDevToolsServiceConfiguration() },
                    (unused, method, args) -> commandsCache.computeIfAbsent(
                            method,
                            key -> {
                                Class<?> returnType = method.getReturnType();
                                return ProxyUtils.createProxy(returnType, commandInvocationHandler);
                            }));
            commandInvocationHandler.setChromeDevToolsService(devtools);
            return devtools;
        } catch (WebSocketServiceException | URISyntaxException e) {
            return ExceptionUtils.rethrow(e);
        }
    }
}
