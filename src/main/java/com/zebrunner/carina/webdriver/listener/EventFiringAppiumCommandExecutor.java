package com.zebrunner.carina.webdriver.listener;

import com.zebrunner.carina.utils.common.CommonUtils;
import com.zebrunner.carina.webdriver.IDriverPool;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import io.appium.java_client.AppiumClientConfig;
import io.appium.java_client.remote.AppiumCommandExecutor;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.CommandInfo;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.service.DriverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * EventFiringAppiumCommandExecutor triggers event listener before/after execution of the command.
 *
 * @author akhursevich
 */
public class EventFiringAppiumCommandExecutor extends AppiumCommandExecutor implements IDriverPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public EventFiringAppiumCommandExecutor(
            @Nonnull Map<String, CommandInfo> additionalCommands,
            @Nullable DriverService service,
            @Nullable HttpClient.Factory httpClientFactory,
            @Nonnull AppiumClientConfig appiumClientConfig) {
        super(additionalCommands, service, httpClientFactory, appiumClientConfig);
    }

    public EventFiringAppiumCommandExecutor(Map<String, CommandInfo> additionalCommands, AppiumClientConfig appiumClientConfig) {
        this(additionalCommands, null, null, appiumClientConfig);
    }

    @Override
    public Response execute(Command command) throws WebDriverException {
        boolean retry = false;
        Response response = null;
        do {
            try {
                response = super.execute(command);
                retry = false;
            } catch (Throwable e) {
                if (DriverCommand.NEW_SESSION.equals(command.getName()) &&
                        WebDriverConfiguration.getIgnoredNewSessionErrorMessages()
                                .stream()
                                .anyMatch(message -> StringUtils.containsIgnoreCase(ExceptionUtils.getRootCauseMessage(e), message))) {
                    LOGGER.debug("NEW_SESSION exception (retry): {}", ExceptionUtils.getRootCauseMessage(e));
                    setCommandCodec(null);
                    retry = true;
                    int timeout = RandomUtils.nextInt(30, 70);
                    CommonUtils.pause(timeout);
                } else {
                    throw e;
                }
            }
        } while (retry);
        return response;
    }
}
