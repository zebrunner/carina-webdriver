package com.zebrunner.carina.webdriver;

import static io.appium.java_client.MobileCommand.prepareArguments;
import static org.openqa.selenium.remote.DriverCommand.EXECUTE_SCRIPT;

import java.util.Collections;
import java.util.Map;

import org.openqa.selenium.Alert;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.html5.RemoteLocationContext;

import com.google.common.collect.ImmutableMap;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.HasAppStrings;
import io.appium.java_client.HasDeviceTime;
import io.appium.java_client.HasOnScreenKeyboard;
import io.appium.java_client.HidesKeyboard;
import io.appium.java_client.HidesKeyboardWithKeyName;
import io.appium.java_client.InteractsWithApps;
import io.appium.java_client.LocksDevice;
import io.appium.java_client.PerformsTouchActions;
import io.appium.java_client.PullsFiles;
import io.appium.java_client.PushesFiles;
import io.appium.java_client.SupportsLegacyAppManagement;
import io.appium.java_client.battery.HasBattery;
import io.appium.java_client.ios.HasIOSClipboard;
import io.appium.java_client.ios.HasIOSSettings;
import io.appium.java_client.ios.IOSBatteryInfo;
import io.appium.java_client.ios.ListensToSyslogMessages;
import io.appium.java_client.ios.PerformsTouchID;
import io.appium.java_client.ios.ShakesDevice;
import io.appium.java_client.remote.MobilePlatform;
import io.appium.java_client.remote.SupportsContextSwitching;
import io.appium.java_client.remote.SupportsLocation;
import io.appium.java_client.remote.SupportsRotation;
import io.appium.java_client.screenrecording.CanRecordScreen;
import io.appium.java_client.ws.StringWebSocketClient;

/**
 * Copy of {@link io.appium.java_client.ios.IOSDriver}, but with default platform name TvOS.<br>
 * <b>For internal usage only</b>
 */
public class TVOSDriver extends AppiumDriver implements
        SupportsContextSwitching,
        SupportsRotation,
        SupportsLocation,
        HidesKeyboard,
        HasDeviceTime,
        PullsFiles,
        InteractsWithApps,
        SupportsLegacyAppManagement,
        HasAppStrings,
        PerformsTouchActions,
        HidesKeyboardWithKeyName,
        ShakesDevice,
        HasIOSSettings,
        HasOnScreenKeyboard,
        LocksDevice,
        PerformsTouchID,
        PushesFiles,
        CanRecordScreen,
        HasIOSClipboard,
        ListensToSyslogMessages,
        HasBattery<IOSBatteryInfo> {
    // The only thing that changed from IOSDriver
    private static final String PLATFORM_NAME = MobilePlatform.TVOS;

    private StringWebSocketClient syslogClient;


    /**
     * Creates a new instance based on command {@code executor} and {@code capabilities}.
     *
     * @param executor is an instance of {@link HttpCommandExecutor}
     *            or class that extends it. Default commands or another vendor-specific
     *            commands may be specified there.
     * @param capabilities take a look at {@link Capabilities}
     */
    public TVOSDriver(HttpCommandExecutor executor, Capabilities capabilities) {
        super(executor, ensurePlatformName(capabilities, PLATFORM_NAME));
    }

    protected static Capabilities ensurePlatformName(
            Capabilities originalCapabilities, String defaultName) {
        return originalCapabilities.merge(new ImmutableCapabilities(CapabilityType.PLATFORM_NAME, defaultName));
    }

    @Override
    public TargetLocator switchTo() {
        return new InnerTargetLocator();
    }

    @SuppressWarnings("unchecked")
    @Override
    public IOSBatteryInfo getBatteryInfo() {
        return new IOSBatteryInfo((Map<String, Object>) execute(EXECUTE_SCRIPT, ImmutableMap.of(
                "script", "mobile: batteryInfo", "args", Collections.emptyList())).getValue());
    }

    private class InnerTargetLocator extends RemoteTargetLocator {
        @Override
        public Alert alert() {
            return new IOSAlert(super.alert());
        }
    }

    class IOSAlert implements Alert {

        private final Alert alert;

        IOSAlert(Alert alert) {
            this.alert = alert;
        }

        @Override
        public void dismiss() {
            execute(DriverCommand.DISMISS_ALERT);
        }

        @Override
        public void accept() {
            execute(DriverCommand.ACCEPT_ALERT);
        }

        @Override
        public String getText() {
            Response response = execute(DriverCommand.GET_ALERT_TEXT);
            return response.getValue().toString();
        }

        @Override
        public void sendKeys(String keysToSend) {
            execute(DriverCommand.SET_ALERT_VALUE, prepareArguments("value", keysToSend));
        }

    }

    @Override
    public RemoteLocationContext getLocationContext() {
        return locationContext;
    }

    @Override
    public synchronized StringWebSocketClient getSyslogClient() {
        if (syslogClient == null) {
            syslogClient = new StringWebSocketClient();
        }
        return syslogClient;
    }
}
