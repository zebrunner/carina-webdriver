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
package com.zebrunner.carina.webdriver.device;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.android.AdbExecutor;
import com.zebrunner.carina.utils.android.recorder.utils.CmdLine;
import com.zebrunner.carina.utils.common.CommonUtils;
import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.factory.DeviceType.Type;
import com.zebrunner.carina.utils.report.ReportContext;
import com.zebrunner.carina.webdriver.IDriverPool;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.core.capability.CapabilityUtils;

import io.appium.java_client.internal.CapabilityHelpers;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.remote.MobilePlatform;

public class Device implements IDriverPool {
    // TODO Review grid capabilities functionality (for example, slotCapabilities)

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String name;
    private String type;
    private String os;
    private String osVersion;
    private String udid;
    private String remoteURL;
    private String vnc;
    private String proxyPort;
    private final AdbExecutor executor = new AdbExecutor();
    private Capabilities capabilities;
    /**
     * ENABLED only in case of availability of parameter - 'uninstall_related_apps'.
     * Store udids of devices where related apps were uninstalled
     */
    private static final List<String> CLEARED_DEVICE_UDIDS = new ArrayList<>();
    private boolean isAdbEnabled;

    public Device() {
        this("", "", "", "", "", "", "", "");
        this.isAdbEnabled = false;
    }

    public Device(String name, String type, String os, String osVersion, String udid, String remoteURL, String vnc, String proxyPort) {
        this.name = name;
        this.type = type;
        this.os = os;
        this.osVersion = osVersion;
        this.udid = udid;
        this.remoteURL = remoteURL;
        this.vnc = vnc;
        this.proxyPort = proxyPort;        
    }

    // todo refactor - read only from capabilities (original)
    public Device(Capabilities capabilities) {
        setName("");
        Optional.ofNullable(CapabilityHelpers.getCapability(capabilities, MobileCapabilityType.DEVICE_NAME, String.class))
                .ifPresent(this::setName);
        WebDriverConfiguration.getCapability(MobileCapabilityType.DEVICE_NAME).ifPresent(this::setName);

        // TODO: should we register default device type as phone?
        setType(SpecialKeywords.PHONE);
        WebDriverConfiguration.getCapability("deviceType").ifPresent(this::setType);
        CapabilityUtils.getZebrunnerCapability(capabilities, "deviceType", String.class).ifPresent(this::setType);

        setOs(WebDriverConfiguration.getCapability(CapabilityType.PLATFORM_NAME).orElse("*"));

        setOsVersion("");
        Optional.ofNullable(CapabilityHelpers.getCapability(capabilities, MobileCapabilityType.PLATFORM_VERSION, String.class))
                .ifPresent(this::setOsVersion);
        WebDriverConfiguration.getCapability(MobileCapabilityType.PLATFORM_VERSION).ifPresent(this::setOsVersion);

        setUdid("");
        WebDriverConfiguration.getCapability(MobileCapabilityType.UDID).ifPresent(this::setUdid);
        Optional.ofNullable(CapabilityHelpers.getCapability(capabilities, MobileCapabilityType.UDID, String.class))
                .ifPresent(this::setUdid);

        setProxyPort("");
        WebDriverConfiguration.getCapability("proxyPort").ifPresent(this::setProxyPort);
        CapabilityUtils.getZebrunnerCapability(capabilities, "proxyPort", String.class).ifPresent(this::setProxyPort);
        
        // try to read extra information from slot capabilities object
        @SuppressWarnings("unchecked")
        Map<String, Object> slotCap = (Map<String, Object>) CapabilityUtils
                .getZebrunnerCapability(capabilities, SpecialKeywords.SLOT_CAPABILITIES, Object.class)
                .orElse(null);
        try {
            if (slotCap != null && slotCap.containsKey(MobileCapabilityType.UDID)) {

                // restore device information from custom slotCapabilities map
                /*
                 * {deviceType=Phone, proxy_port=9000,
                 * server:CONFIG_UUID=24130dde-59d4-4310-95ba-6f57b9d265c3,
                 * seleniumProtocol=WebDriver, adb_port=5038,
                 * vnc=wss://stage.qaprosoft.com:7410/websockify,
                 * deviceName=Nokia_6_1, version=8.1.0, platform=ANDROID,
                 * platformVersion=8.1.0, automationName=uiautomator2,
                 * browserName=Nokia_6_1, maxInstances=1, platformName=ANDROID,
                 * udid=PL2GAR9822804910}
                 */

                // That's a trusted information from Zebrunner Device Farm so we can override all values
                setName((String) slotCap.get(MobileCapabilityType.DEVICE_NAME));
                setOs((String) slotCap.get(CapabilityType.PLATFORM_NAME));
                setOsVersion((String) slotCap.get(MobileCapabilityType.PLATFORM_VERSION));
                setType((String) slotCap.get("deviceType"));
                setUdid((String) slotCap.get(MobileCapabilityType.UDID));
                if (slotCap.containsKey("vnc")) {
                    setVnc((String) slotCap.get("vnc"));
                }
                if (slotCap.containsKey("proxyPort")) {
                    setProxyPort(String.valueOf(slotCap.get("proxyPort")));
                }
                if (slotCap.containsKey("remoteURL")) {
                    setRemoteURL(String.valueOf(slotCap.get("remoteURL")));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unable to get device info!", e);
        }
        setCapabilities(capabilities);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = (null == name) ? "" : name;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getUdid() {
        return udid;
    }

    public void setUdid(String udid) {
        this.udid = (null == udid) ? "" : udid;
    }

    public String getRemoteURL() {
        return remoteURL;
    }

    public void setRemoteURL(String remoteURL) {
        this.remoteURL = remoteURL;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getVnc() {
        return vnc;
    }

    public void setVnc(String vnc) {
        this.vnc = vnc;
    }

    public String getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
    }
    
    public Capabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Capabilities capabilities) {
        this.capabilities = capabilities;
    }

    public boolean isPhone() {
        return getType().equalsIgnoreCase(SpecialKeywords.PHONE);
    }

    public boolean isTablet() {
        return getType().equalsIgnoreCase(SpecialKeywords.TABLET);
    }

    public boolean isTv() {
        return getType().equalsIgnoreCase(SpecialKeywords.TV) || getType().equalsIgnoreCase(SpecialKeywords.ANDROID_TV) || getType().equalsIgnoreCase(SpecialKeywords.TVOS);
    }

    public Type getDeviceType() {
        if (isNull()) {
            // if no device initialized it means that desktop UI automation is used
            return Type.DESKTOP;
        }

        if (SpecialKeywords.ANDROID.equalsIgnoreCase(getOs())) {
            if (isTablet()) {
                return Type.ANDROID_TABLET;
            }
            if (isTv()) {
                return Type.ANDROID_TV;
            }
            return Type.ANDROID_PHONE;
        } else if (SpecialKeywords.IOS.equalsIgnoreCase(getOs()) || SpecialKeywords.MAC.equalsIgnoreCase(getOs()) ||
                SpecialKeywords.TVOS.equalsIgnoreCase(getOs())) {
            if (isTablet()) {
                return Type.IOS_TABLET;
            }
            if (isTv()) {
                return Type.APPLE_TV;
            }
                return Type.IOS_PHONE;
        }
        throw new RuntimeException("Incorrect driver type. Please, check config file for " + toString());
    }

    public String toString() {
        return String.format("name: %s; type: %s; os: %s; osVersion: %s; udid: %s; remoteURL: %s; vnc: %s; proxyPort: %s", getName(),
                getType(), getOs(), getOsVersion(), getUdid(), getRemoteURL(), getVnc(), getProxyPort());
    }

    public boolean isNull() {
        return StringUtils.isEmpty(getName());
    }

    public void connectRemote() {
        if (isNull())
            return;

        if (isIOS())
            return;

        String connectUrl = getAdbName();
        if (StringUtils.isEmpty(connectUrl)) {
            LOGGER.error("Unable to use adb as ADB remote url is not available!");
            return;
        }

        LOGGER.debug("adb connect {}", connectUrl);
        String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "connect", connectUrl);
        executor.execute(cmd);
        CommonUtils.pause(1);

        // TODO: verify that device connected and raise an error if not and disabled adb integration
        String[] cmd2 = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "devices");
        // Possible result of the command:

        // List of devices attached
        // <device_udid_or_remote_url> unauthorized
        // <device_udid_or_remote_url> unauthorized
        String deviceStatus = executor.execute(cmd2).stream()
                .filter(line -> line.contains(getAdbName()))
                .map(line -> StringUtils.remove(line, getAdbName()))
                .map(StringUtils::trim)
                .findAny()
                .orElseThrow(() -> new RuntimeException(
                        String.format("There are no device with udid (or remote url) '%s' in the list of adb devices.", getAdbName())));
        if (!"device".equals(deviceStatus)) {
            throw new RuntimeException(String.format("Device adb status is invalid: '%s'", deviceStatus));
        }

        isAdbEnabled = true;
    }

    public void disconnectRemote() {
        if (!isAdbEnabled)
            return;
        
        if (isNull())
            return;

        String connectUrl = getAdbName();
        if (StringUtils.isEmpty(connectUrl)) {
            LOGGER.error("Unable to use adb as ADB remote url is not available!");
            return;
        }

        // [VD] No need to do adb command as stopping STF session do it correctly
        // in new STF we have huge problems with sessions disconnect
        LOGGER.debug("adb disconnect {}", getRemoteURL());
        String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "disconnect", connectUrl);
        executor.execute(cmd);
        isAdbEnabled = false;
    }

    public String getFullPackageByName(final String name) {

        List<String> packagesList = getInstalledPackages();
        LOGGER.debug("Found packages: {}", packagesList);
        String resultPackage = null;
        for (String packageStr : packagesList) {
            if (packageStr.matches(String.format(".*%s.*", name))) {
                LOGGER.info("Package was found: {}", packageStr);
                resultPackage = packageStr;
                break;
            }
        }
        if (null == resultPackage) {
            LOGGER.info("Package wasn't found using following name: {}", name);
            resultPackage = "not found";
        }
        return resultPackage;
    }

    public List<String> getInstalledPackages() {
        String deviceUdid = getAdbName();
        LOGGER.debug("Device udid: {}", deviceUdid);
        String[] cmd = CmdLine.createPlatformDependentCommandLine("adb", "-s", deviceUdid, "shell", "pm", "list", "packages");
        String cmdAsString = Arrays.toString(cmd);
        LOGGER.debug("Following cmd will be executed: {}", cmdAsString);
        return executor.execute(cmd);
    }

    public boolean isAppInstall(final String packageName) {
        return !getFullPackageByName(packageName).contains("not found");
    }

    public void pressKey(int key) {
        if (isNull())
            return;

        String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "-s", getAdbName(), "shell", "input",
                "keyevent", String.valueOf(key));
        executor.execute(cmd);
    }

    public void pause(long timeout) {
        CommonUtils.pause(timeout);
    }

    public void clearAppData() {
        clearAppData(CapabilityHelpers.getCapability(getCapabilities(), MobileCapabilityType.APP, String.class));
    }

    public void clearAppData(String app) {
        if (!SpecialKeywords.ANDROID.equalsIgnoreCase((String) getCapabilities().getCapability(CapabilityType.PLATFORM_NAME))) {
            return;
        }
        if (isNull())
            return;
        // adb -s UDID shell pm clear com.myfitnesspal.android
        String packageName = getApkPackageName(app);
        String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "-s", getAdbName(), "shell", "pm", "clear", packageName);
        executor.execute(cmd);
    }

    public String getApkPackageName(String apkFile) {
        // aapt dump badging <apk_file> | grep versionCode
        // aapt dump badging <apk_file> | grep versionName
        // output:
        // package: name='com.myfitnesspal.android' versionCode='9025' versionName='develop-QA' platformBuildVersionName='6.0-2704002'

        String packageName = "";

        String[] cmd = CmdLine.insertCommandsAfter("aapt dump badging".split(" "), apkFile);
        List<String> output = executor.execute(cmd);
        // parse output command and get appropriate data

        for (String line : output) {
            if (line.contains("versionCode") && line.contains("versionName")) {
                LOGGER.debug(line);
                String[] outputs = line.split("'");
                packageName = outputs[1]; // package
            }
        }

        return packageName;
    }

    public void uninstallApp(String packageName) {
        if (isNull())
            return;

        // adb -s UDID uninstall com.myfitnesspal.android
        String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "-s", getAdbName(), "uninstall", packageName);
        executor.execute(cmd);
    }

    public void installApp(String apkPath) {
        if (isNull())
            return;

        // adb -s UDID install com.myfitnesspal.android
        String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "-s", getAdbName(), "install", "-r", apkPath);
        executor.execute(cmd);
    }

    public synchronized void installAppSync(String apkPath) {
        if (isNull())
            return;

        // adb -s UDID install com.myfitnesspal.android
        String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "-s", getAdbName(), "install", "-r", apkPath);
        executor.execute(cmd);
    }

    public String[] getInstalledApkVersion(String packageName) {
        // adb -s UDID shell dumpsys package PACKAGE | grep versionCode
        if (isNull())
            return null;

        String[] res = new String[3];
        res[0] = packageName;

        String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "-s", getAdbName(), "shell", "dumpsys", "package", packageName);
        List<String> output = executor.execute(cmd);

        for (String line : output) {
            LOGGER.debug(line);
            if (line.contains("versionCode")) {
                // versionCode=17040000 targetSdk=25
                LOGGER.info("Line for parsing installed app: {}", line);
                String[] outputs = line.split("=");
                String tmp = outputs[1]; // everything after '=' sign
                res[1] = tmp.split(" ")[0];
            }

            if (line.contains("versionName")) {
                // versionName=8.5.0
                LOGGER.info("Line for parsing installed app: {}", line);
                String[] outputs = line.split("=");
                res[2] = outputs[1];
            }
        }

        if (res[0] == null && res[1] == null && res[2] == null) {
            return null;
        }
        return res;
    }

    public String[] getApkVersion(String apkFile) {
        // aapt dump badging <apk_file> | grep versionCode
        // aapt dump badging <apk_file> | grep versionName
        // output:
        // package: name='com.myfitnesspal.android' versionCode='9025' versionName='develop-QA' platformBuildVersionName='6.0-2704002'

        String[] res = new String[3];
        res[0] = "";
        res[1] = "";
        res[2] = "";

        String[] cmd = CmdLine.insertCommandsAfter("aapt dump badging".split(" "), apkFile);
        List<String> output = executor.execute(cmd);
        // parse output command and get appropriate data

        for (String line : output) {
            if (line.contains("versionCode") && line.contains("versionName")) {
                LOGGER.debug(line);
                String[] outputs = line.split("'");
                res[0] = outputs[1]; // package
                res[1] = outputs[3]; // versionCode
                res[2] = outputs[5]; // versionName
            }
        }
        return res;
    }

    public List<String> execute(String[] cmd) {
        return executor.execute(cmd);
    }

    public void setProxy(final String host, final String port, final String ssid, final String password) {
        if (!getOs().equalsIgnoreCase(Type.ANDROID_PHONE.getFamily())) {
            LOGGER.error("Proxy configuration is available for Android ONLY");
            throw new RuntimeException("Proxy configuration is available for Android ONLY");
        }
        if (!isAppInstall(SpecialKeywords.PROXY_SETTER_PACKAGE)) {
            final String proxySetterFileName = "./proxy-setter-temp.apk";
            File targetFile = new File(proxySetterFileName);
            downloadFileFromJar(SpecialKeywords.PROXY_SETTER_RES_PATH, targetFile);
            installApp(proxySetterFileName);
        }
        String deviceUdid = getAdbName();
        LOGGER.debug("Device udid: {}", deviceUdid);
        String[] cmd = CmdLine.createPlatformDependentCommandLine("adb", "-s", deviceUdid, "shell", "am", "start", "-n",
                "tk.elevenk.proxysetter/.MainActivity", "-e", "host", host, "-e", "port", port, "-e", "ssid", ssid, "-e", "key", password);
        String cmdAsString = Arrays.toString(cmd);
        LOGGER.debug("Following cmd will be executed: {}", cmdAsString);
        executor.execute(cmd);
    }

    private void downloadFileFromJar(final String path, final File targetFile) {
        try (InputStream initialStream = Device.class.getClassLoader().getResourceAsStream(path)) {
            FileUtils.copyInputStreamToFile(initialStream, targetFile);
        } catch (IOException e) {
            LOGGER.error("Error during copying of file from the resources. ".concat(e.getMessage()));
        }
    }

    public String getAdbName() {
        if (!StringUtils.isEmpty(getRemoteURL())) {
            return getRemoteURL();
        } else if (!StringUtils.isEmpty(getUdid())) {
            return getUdid();
        } else {
            return "";
        }
    }

    /**
     * Related apps will be uninstall just once for a test launch.
     */
    public void uninstallRelatedApps() {
        if (MobilePlatform.ANDROID.equalsIgnoreCase(getOs())
                && Configuration.get(WebDriverConfiguration.Parameter.UNINSTALL_RELATED_APPS, Boolean.class).orElse(false)
                && !CLEARED_DEVICE_UDIDS.contains(getUdid())) {
            String mobileApp = CapabilityHelpers.getCapability(getCapabilities(), MobileCapabilityType.APP, String.class);
            if (mobileApp == null) {
                mobileApp = "";
            }
            LOGGER.debug("Current mobile app: {}", mobileApp);
            String tempPackage;
            try {
                tempPackage = getApkPackageName(mobileApp);
            } catch (Exception e) {
                LOGGER.info("Error during extraction of package using aapt. It will be extracted from config");
                tempPackage = WebDriverConfiguration.getAppiumCapability("appPackage").orElse("");
            }
            final String mobilePackage = tempPackage;
            LOGGER.debug("Current mobile package: {}", mobilePackage);
            // in general it has following naming convention:
            // com.projectname.app
            // so we need to remove all apps realted to 1 project
            String projectName = mobilePackage.split("\\.")[1];
            LOGGER.debug("Apps related to current project will be uninstalled. Extracted project: {}", projectName);
            List<String> installedPackages = getInstalledPackages();
            // extracted package syntax: package:com.project.app
            installedPackages.parallelStream()
                    .filter(packageName -> (packageName.matches(String.format(".*\\.%s\\..*", projectName))
                            && !packageName.equalsIgnoreCase(String.format("package:%s", mobilePackage))))
                    .collect(Collectors.toList()).forEach(k -> uninstallApp(k.split(":")[1]));
            CLEARED_DEVICE_UDIDS.add(getUdid());
            LOGGER.debug("Udids of devices where applciation was already reinstalled: {}", CLEARED_DEVICE_UDIDS);
        } else {
            LOGGER.debug("Related apps had been already uninstalled or flag uninstall_related_apps is disabled.");
        }

    }
    
    /**
     * Save xml layout of the application 
     * @param screenshotName - png file name to generate appropriate uix  
     * @return saved file
     */
    public Optional<Path> generateUiDump(String screenshotName) {
        if (isNull()) {
            return Optional.empty();
        }
        
//        TODO: investigate with iOS: how does it work with iOS
		if (!isConnected()) {
		    LOGGER.debug("Device isConnected() returned false. Dump file won't be generated.");
			//do not use new features if execution is not inside approved cloud
            return Optional.empty();
		}
        
        if (IDriverPool.getDrivers().size() == 0) {
            LOGGER.debug("There is no active drivers in the pool.");
            return Optional.empty();
        }
        // TODO: investigate how to connect screenshot with xml dump: screenshot
        // return File -> Zip png and uix or move this logic to zafira
        
        try {
            WebDriver driver = getDriver(this);
            if (driver == null) {
                LOGGER.debug("There is no active driver for device: {}", getName());
                return Optional.empty();
            }
            
            LOGGER.debug("UI dump generation...");

            String pageSource = driver.getPageSource();
            pageSource = pageSource.replaceAll(SpecialKeywords.ANDROID_START_NODE, SpecialKeywords.ANDROID_START_UIX_NODE).
                    replaceAll(SpecialKeywords.ANDROID_END_NODE, SpecialKeywords.ANDROID_END_UIX_NODE);
            Path dumpFile = ReportContext.getTestDirectory()
                    .resolve(String.format("%s.uix", screenshotName.replace(".png", "")));
            Path file = null;
            try {
                file = Files.writeString(dumpFile, pageSource, StandardCharsets.US_ASCII);
            } catch (IOException e) {
                LOGGER.warn("Error has been met during attempt to extract xml tree.", e);
            }
            LOGGER.debug("XML file path: {}", dumpFile);
            return Optional.ofNullable(file);
        } catch (Exception e) {
            LOGGER.error("Undefined failure during UiDump generation for Android device!", e);
        }
        return Optional.empty();
    }
    
    private boolean isIOS() {
        return SpecialKeywords.IOS.equalsIgnoreCase(getOs()) || SpecialKeywords.TVOS.equalsIgnoreCase(getOs());
    }

    private boolean isConnected() {
        try {
            if (getOs().equalsIgnoreCase(Type.ANDROID_PHONE.getFamily())) {
                return getConnectedDevices().stream().parallel().anyMatch(m -> m.contains(getAdbName()));
            } else {
                return false;
            }
        } catch (Exception e) {
            //do nothing for now
            return false;
        }
    }
    
    private List<String> getConnectedDevices() {
        // regexp for connected device. Syntax: udid device
        String deviceUDID = "(.*)\\tdevice$";
        String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "devices");
        List<String> cmdOutput = executor.execute(cmd);
        List<String> connectedDevices = cmdOutput.stream().parallel().filter(d -> d.matches(deviceUDID)).collect(Collectors.toList());
        LOGGER.debug("Connected devices: {}", connectedDevices);
        return connectedDevices;
    }

}
