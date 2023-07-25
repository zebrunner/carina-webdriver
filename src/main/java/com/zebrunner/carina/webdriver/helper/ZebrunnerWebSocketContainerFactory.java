package com.zebrunner.carina.webdriver.helper;

import static com.github.kklisura.cdt.services.utils.ConfigurationUtils.systemProperty;

import java.lang.invoke.MethodHandles;

import javax.websocket.WebSocketContainer;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.container.grizzly.client.GrizzlyClientContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kklisura.cdt.services.factory.WebSocketContainerFactory;

/**
 * Zebrunner WebSocketContainer factory for DevTools ({@link IChromeDevToolsHelper}).<br>
 * <b>For internal usage only</b>
 */
@SuppressWarnings("unused")
public class ZebrunnerWebSocketContainerFactory implements WebSocketContainerFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String WEBSOCKET_INCOMING_BUFFER_PROPERTY = "com.github.kklisura.cdt.services.config.incomingBuffer";
    private static final int KB = 1024;
    private static final int MB = 1024 * KB;
    private static final int DEFAULT_INCOMING_BUFFER_SIZE = 8 * MB;
    private static final String INCOMING_BUFFER_SIZE_PROPERTY = "org.glassfish.tyrus.incomingBufferSize";
    private static final long INCOMING_BUFFER_SIZE = systemProperty(WEBSOCKET_INCOMING_BUFFER_PROPERTY, DEFAULT_INCOMING_BUFFER_SIZE);

    @Override
    public WebSocketContainer getWebSocketContainer() {
        final ClientManager client = ClientManager.createClient(GrizzlyClientContainer.class.getName());
        client.getProperties().put(INCOMING_BUFFER_SIZE_PROPERTY, INCOMING_BUFFER_SIZE);
        if (LOGGER.isDebugEnabled()) {
            client.getProperties().put(ClientProperties.LOG_HTTP_UPGRADE, true);
        }
        client.getProperties().put(ClientProperties.REDIRECT_ENABLED, true);
        return client;
    }

}
