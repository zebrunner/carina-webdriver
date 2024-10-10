package com.zebrunner.carina.webdriver.listener;

import com.zebrunner.carina.commons.artifact.IArtifactManager;
import com.zebrunner.carina.utils.common.CommonUtils;
import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.mobile.ArtifactProvider;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import io.appium.java_client.AppiumClientConfig;
import io.appium.java_client.internal.CapabilityHelpers;
import io.appium.java_client.remote.AppiumCommandExecutor;
import io.appium.java_client.remote.AppiumNewSessionCommandPayload;
import io.appium.java_client.remote.options.SupportsAppOption;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.CommandInfo;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.service.DriverService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EventFiringAppiumCommandExecutor triggers event listener before/after execution of the command.
 *
 * @author akhursevich
 */
public final class EventFiringAppiumCommandExecutor extends AppiumCommandExecutor {
    private static final AtomicInteger CURRENT_SESSIONS_AMOUNT = new AtomicInteger(0);
    private static final Map<String, Duration> EXCEPTION_TIMEOUTS = new ConcurrentHashMap<>();
    private static final BlockingQueue<String> BLOCKING_QUEUE = new LinkedBlockingQueue<>(
            Configuration.getRequired(WebDriverConfiguration.Parameter.MAX_NEW_SESSION_QUEUE, Integer.class));

    private static final LazyInitializer<IArtifactManager> ARTIFACT_PROVIDERS = new LazyInitializer<>() {
        @Override
        protected IArtifactManager initialize() throws ConcurrentException {
            return ArtifactProvider.getInstance();
        }
    };

    private final AtomicBoolean retry = new AtomicBoolean(false);
    private final Integer newSessionPause;
    private Capabilities capabilities;

    public EventFiringAppiumCommandExecutor(
            @Nonnull Map<String, CommandInfo> additionalCommands,
            @Nullable DriverService service,
            @Nullable HttpClient.Factory httpClientFactory,
            @Nonnull AppiumClientConfig appiumClientConfig) {
        super(additionalCommands, service, httpClientFactory, appiumClientConfig);
        newSessionPause = Configuration.getRequired(WebDriverConfiguration.Parameter.MAX_NEW_SESSION_QUEUE, Integer.class) * 3;
    }

    public EventFiringAppiumCommandExecutor(Map<String, CommandInfo> additionalCommands, AppiumClientConfig appiumClientConfig) {
        this(additionalCommands, null, null, appiumClientConfig);
    }

    public void setCapabilities(Capabilities capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public Response execute(Command command) throws WebDriverException {
        boolean isNewSessionCommand = DriverCommand.NEW_SESSION.equals(command.getName());
        Response response = null;
        do {
            if (isNewSessionCommand) {
                try {
                    BLOCKING_QUEUE.put(UUID.randomUUID().toString() + System.currentTimeMillis());
                    String app = CapabilityHelpers.getCapability(capabilities, SupportsAppOption.APP_OPTION, String.class);
                    if (app != null) {
                        MutableCapabilities appCaps = new MutableCapabilities().merge(capabilities);
                        appCaps.setCapability("appium:" + SupportsAppOption.APP_OPTION, ARTIFACT_PROVIDERS.get()
                                .getDirectLink(app));
                        FieldUtils.writeField(FieldUtils.getField(Command.class, "payload", true),
                                command,
                                new AppiumNewSessionCommandPayload(capabilities.merge(appCaps)),
                                true);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ConcurrentException | IllegalAccessException e) {
                    return ExceptionUtils.rethrow(e);
                }

            }
            try {
                if (DriverCommand.QUIT.equalsIgnoreCase(command.getName())) {
                    CURRENT_SESSIONS_AMOUNT.getAndDecrement();
                }
                response = super.execute(command);
                if (isNewSessionCommand) {
                    CURRENT_SESSIONS_AMOUNT.getAndIncrement();
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
                CommonUtils.pause(RandomUtils.nextInt(1, newSessionPause + 1));
                setCommandCodec(null);
            } finally {
                if (isNewSessionCommand) {
                    try {
                        BLOCKING_QUEUE.take();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } while (retry.get());
        return response;
    }
}
