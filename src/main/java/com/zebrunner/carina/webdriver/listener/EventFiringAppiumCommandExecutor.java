package com.zebrunner.carina.webdriver.listener;

import com.zebrunner.carina.utils.common.CommonUtils;
import com.zebrunner.carina.utils.config.Configuration;
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
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EventFiringAppiumCommandExecutor triggers event listener before/after execution of the command.
 *
 * @author akhursevich
 */
public class EventFiringAppiumCommandExecutor extends AppiumCommandExecutor implements IDriverPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final AtomicInteger DRIVERS_QUEUE_NOT_STARTED_AMOUNT = new AtomicInteger(0);
    private static final AtomicInteger CURRENT_SESSIONS_AMOUNT = new AtomicInteger(0);
    private static final Map<String, Duration> EXCEPTION_TIMEOUTS = new ConcurrentHashMap<>();

    private final AtomicBoolean retry = new AtomicBoolean(false);

    public EventFiringAppiumCommandExecutor(
            @Nonnull Map<String, CommandInfo> additionalCommands,
            @Nullable DriverService service,
            @Nullable HttpClient.Factory httpClientFactory,
            @Nonnull AppiumClientConfig appiumClientConfig) {
        super(additionalCommands, service, httpClientFactory, appiumClientConfig);
        DRIVERS_QUEUE_NOT_STARTED_AMOUNT.getAndIncrement();
    }

    public EventFiringAppiumCommandExecutor(Map<String, CommandInfo> additionalCommands, AppiumClientConfig appiumClientConfig) {
        this(additionalCommands, null, null, appiumClientConfig);
    }

    @Override
    public Response execute(Command command) throws WebDriverException {
        boolean isNewSessionCommand = DriverCommand.NEW_SESSION.equals(command.getName());
        Response response = null;
        do {
            try {
                if (isNewSessionCommand && DRIVERS_QUEUE_NOT_STARTED_AMOUNT.get() > 10) {
                    CommonUtils.pause(
                            RandomUtils.nextInt(1, Configuration.getRequired(WebDriverConfiguration.Parameter.INIT_RETRY_INTERVAL, Integer.class)));
                }
                if (DriverCommand.QUIT.equalsIgnoreCase(command.getName())) {
                    CURRENT_SESSIONS_AMOUNT.getAndDecrement();
                }
                response = super.execute(command);
                if (isNewSessionCommand) {
                    CURRENT_SESSIONS_AMOUNT.getAndIncrement();
                    DRIVERS_QUEUE_NOT_STARTED_AMOUNT.getAndDecrement();
                    retry.set(false);
                }
            } catch (Throwable e) {
                if (!isNewSessionCommand) {
                    throw e;
                }
                Optional<String> error = WebDriverConfiguration.getIgnoredNewSessionErrorMessages()
                        .keySet()
                        .stream()
                        .filter(message -> StringUtils.containsIgnoreCase(ExceptionUtils.getRootCauseMessage(e), message))
                        .findAny();
                if (error.isEmpty()) {
                    throw e;
                }
                LOGGER.warn("{} - {}", error.get(), ExceptionUtils.getRootCauseMessage(e));
                retry.set(true);
                WebDriverConfiguration.getIgnoredNewSessionErrorMessages()
                        .compute(error.get(), (message, waitTime) -> {
                            if (waitTime.isZero()) {
                                return waitTime;
                            }

                            if (CURRENT_SESSIONS_AMOUNT.get() > 0) {
                                EXCEPTION_TIMEOUTS.clear();
                                return waitTime;
                            }

                            if (waitTime.isNegative()) {
                                retry.set(false);
                                return waitTime;
                            }

                            Duration currentTime = Duration.ofMillis(System.currentTimeMillis());
                            if (!EXCEPTION_TIMEOUTS.containsKey(message)) {
                                EXCEPTION_TIMEOUTS.put(message, currentTime.plus(waitTime));
                                return waitTime;
                            } else {
                                if (EXCEPTION_TIMEOUTS.get(message).compareTo(currentTime) <= 0) {
                                    // expired
                                    EXCEPTION_TIMEOUTS.remove(message);
                                    retry.set(false);
                                    return Duration.ofSeconds(1)
                                            .negated();
                                }
                                return waitTime;
                            }
                        });
                if (!retry.get()) {
                    throw e;
                }
                setCommandCodec(null);
            }
        } while (retry.get());
        return response;
    }
}
