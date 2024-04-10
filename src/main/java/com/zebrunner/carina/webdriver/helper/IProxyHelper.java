package com.zebrunner.carina.webdriver.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apiguardian.api.API;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.report.SessionContext;
import com.zebrunner.carina.webdriver.IDriverPool;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.listener.DriverListener;
import com.zebrunner.carina.webdriver.proxy.ZebrunnerProxyBuilder;

/**
 * Provides methods for interacting with Zebrunner proxy ({@link WebDriverConfiguration.Parameter#PROXY_TYPE} = <b>Zebrunner</b>)<br>
 * <b>Also this methods works only in 'extended' proxy mode {@link ZebrunnerProxyBuilder#useExtendedProxy()}</b>
 */
@API(status = API.Status.EXPERIMENTAL)
public interface IProxyHelper extends IDriverPool {
    Logger I_PROXY_HELPER_LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Download Har artifact to the session {@code downloads} folder with the specified name
     *
     * @param fileName name of the file, for example {@code session}. Extension will be added {@code .har}, so the result name of the file:
     *            {@code session.har}
     * @return {@link Optional} of {@link Path} to the har if available, {@link Optional#empty()} otherwise
     */
    default Optional<Path> getProxyHarArtifact(String fileName) {
        return getProxyHarArtifact(fileName, "@all");
    }

    /**
     * Download Har artifact to the session {@code downloads} folder with the specified name.<br>
     *
     * @param fileName name of the file, for example {@code session}. Extension will be added {@code .har}, so the result name of the file:
     *            {@code session.har}
     * @param flow todo add description.
     * @return {@link Optional} of {@link Path} to the har if available, {@link Optional#empty()} otherwise
     */
    default Optional<Path> getProxyHarArtifact(String fileName, String flow) {
        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("Parameter could not be null, blank or empty.");
        }
        try {
            URL endpoint = new URL(String.format("%s/%s/%s/%s/%s", Configuration.getRequired(WebDriverConfiguration.Parameter.SELENIUM_URL)
                    .replace("wd/hub", "proxy"),
                    DriverListener.castDriver(getDriver(), RemoteWebDriver.class).getSessionId(),
                    "download",
                    "har",
                    flow));
            return Optional.of(PathUtils.copyFile(endpoint,
                    SessionContext.getArtifactsFolder().resolve(StringUtils.appendIfMissingIgnoreCase(fileName, ".har"))));
        } catch (MalformedURLException e) {
            return ExceptionUtils.rethrow(e);
        } catch (IOException e) {
            I_PROXY_HELPER_LOGGER.error("Could not download har artifact: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Download dump artifact to the session {@code downloads} folder with the specified name
     *
     * @param fileName name of the file, for example {@code session}. Extension will be added {@code .dump}, so the result name of the file:
     *            {@code session.dump}
     * @return {@link Optional} of {@link Path} to the dump if available, {@link Optional#empty()} otherwise
     */
    default Optional<Path> getProxyDumpArtifact(String fileName) {
        return getProxyDumpArtifact(fileName, "@all");
    }

    /**
     * Download dump artifact to the session {@code downloads} folder with the specified name.<br>
     *
     * @param fileName name of the file, for example {@code session}. Extension will be added {@code .dump}, so the result name of the file:
     *            {@code session.dump}
     * @param flow todo add description.
     * @return {@link Optional} of {@link Path} to the dump if available, {@link Optional#empty()} otherwise
     */
    default Optional<Path> getProxyDumpArtifact(String fileName, String flow) {
        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("Parameter could not be null, blank or empty.");
        }
        try {
            URL endpoint = new URL(String.format("%s/%s/%s/%s/%s", Configuration.getRequired(WebDriverConfiguration.Parameter.SELENIUM_URL)
                    .replace("wd/hub", "proxy"),
                    DriverListener.castDriver(getDriver(), RemoteWebDriver.class).getSessionId(),
                    "download",
                    "dump",
                    flow));
            return Optional.of(PathUtils.copyFile(endpoint,
                    SessionContext.getArtifactsFolder().resolve(StringUtils.appendIfMissingIgnoreCase(fileName, ".dump"))));
        } catch (MalformedURLException e) {
            return ExceptionUtils.rethrow(e);
        } catch (IOException e) {
            I_PROXY_HELPER_LOGGER.error("Could not download dump artifact: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Clear proxy state
     */
    default void clearProxyFlows() {
        try {
            String url = String.format("%s/%s/%s", Configuration.getRequired(WebDriverConfiguration.Parameter.SELENIUM_URL)
                    .replace("wd/hub", "proxy"),
                    DriverListener.castDriver(getDriver(), RemoteWebDriver.class).getSessionId(),
                    "clear-flows");
            String username = getField(url, 1);
            String password = getField(url, 2);

            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("DELETE");

            if (!username.isEmpty() && !password.isEmpty()) {
                String usernameColonPassword = username + ":" + password;
                String basicAuthPayload = "Basic " + Base64.getEncoder().encodeToString(usernameColonPassword.getBytes());
                con.addRequestProperty("Authorization", basicAuthPayload);
            }
            int status = con.getResponseCode();
            String response = "";
            if (200 <= status && status <= 299) {
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder sb = new StringBuilder();
                while ((inputLine = br.readLine()) != null) {
                    sb.append(inputLine);
                }
                br.close();
                response = sb.toString();
            }
            I_PROXY_HELPER_LOGGER.info("Response: {}", response);
        } catch (Exception e) {
            I_PROXY_HELPER_LOGGER.error("Could not clear proxy flow: {}", e.getMessage(), e);
        }
    }

    private String getField(String url, int position) {
        Pattern pattern = Pattern.compile(".*:\\/\\/(.*):(.*)@");
        Matcher matcher = pattern.matcher(url);

        return matcher.find() ? matcher.group(position) : "";
    }

}
