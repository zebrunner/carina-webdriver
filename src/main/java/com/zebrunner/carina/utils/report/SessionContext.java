package com.zebrunner.carina.utils.report;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.agent.core.config.ConfigurationHolder;
import com.zebrunner.agent.core.registrar.Artifact;
import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.listener.DriverListener;

public final class SessionContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String ARTIFACTS_ENDPOINT = "download";

    private SessionContext() {
        // hide
    }

    /**
     * Get artifacts folder.<br>
     * By default, artifacts folder is the folder in the test directory with the 'downloads' name.
     *
     * @return {@link Path}
     */
    public static synchronized Path getArtifactsFolder() {
        LOGGER.debug("Trying to get artifacts folder.");
        try {
            // renamed to downloads to avoid automatic upload on our old Zebrunner ci-pipeline versions
            Path directory = ReportContext.getTestDirectory()
                    .resolve("downloads");
            // artifacts directory should use canonical path otherwise auto download feature is broken in browsers
            Optional<String> customArtifactsFolder = Configuration.get(WebDriverConfiguration.Parameter.CUSTOM_ARTIFACTS_FOLDER);
            if (customArtifactsFolder.isPresent()) {
                LOGGER.debug("Parameter '{}' found with value '{}', so this path will be used as artifacts folder.",
                        WebDriverConfiguration.Parameter.CUSTOM_ARTIFACTS_FOLDER.getKey(), customArtifactsFolder.get());
                directory = Path.of(customArtifactsFolder.get());
            }
            if (Files.notExists(directory)) {
                LOGGER.debug("The artifacts folder does not exist, so it will be created.");
                Files.createDirectory(directory);
                LOGGER.debug("The artifacts folder created. Path: '{}'", directory);
            }

            if (!Files.isDirectory(directory)) {
                throw new IOException(String.format("Path to the artifacts folder '%s' is not a directory.", directory));
            }
            return directory;
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Cannot get artifacts folder. Message: '%s'", e.getMessage()), e);
        }
    }

    /**
     * Get consolidated list of filenames from the local and remote (if available) auto-download folders
     *
     * @param driver {@link WebDriver}
     * @return {@link List} of filenames with the extensions
     */
    public static List<String> listArtifacts(WebDriver driver) {
        LOGGER.debug("Trying to get list of artifacts.");
        // at first, we get all local artifacts
        List<String> artifactFileNames;
        try (Stream<Path> files = Files.list(getArtifactsFolder())) {
            artifactFileNames = files.map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
            LOGGER.debug("Local artifacts: {}", artifactFileNames);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Cannot get list of local artifacts Message: '%s'.", e.getMessage()), e);
        }

        // then, we try to create connection with the remote session and get list of it's artifacts
        URL endpoint = getEndpoint(driver, ARTIFACTS_ENDPOINT, null);
        if (!isEndpointAvailable(endpoint)) {
            return artifactFileNames;
        }

        try {
            HttpURLConnection con = (HttpURLConnection) endpoint.openConnection();
            // explicitly define as true because default value doesn't work and return 301 status
            con.setInstanceFollowRedirects(true);
            con.setRequestMethod("GET");
            getAuthenticator(endpoint).ifPresent(con::setAuthenticator);
            int responseCode = con.getResponseCode();
            try (InputStream connectionStream = con.getInputStream()) {
                List<String> remoteArtifacts = new ArrayList<>();
                String responseBody = readStream(connectionStream);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Matcher matcher = Pattern.compile("href=([\"'])((?:(?!\\1)[^\\\\]|(?:\\\\\\\\)*\\\\[^\\\\])*)\\1").matcher(responseBody);
                    while (matcher.find()) {
                        remoteArtifacts.add(matcher.group(2));
                    }
                    LOGGER.debug("Remote artifacts: {}", remoteArtifacts);
                    artifactFileNames.addAll(remoteArtifacts);
                } else {
                    throw new IOException(
                            String.format("Cannot get list of remote artifacts.%n Response code: '%s'%nResponse content: '%s'.",
                                    responseCode, responseBody));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(
                    String.format("Cannot create connection for getting list of remote artifacts. Message: '%s'", e.getMessage()), e);
        }
        return artifactFileNames;
    }

    /**
     * Get artifacts from local and remote (if available) auto download folders by pattern.
     * Remote artifacts (found by pattern) will be downloaded to the local artifacts folder.
     *
     * @param driver {@link WebDriver}
     * @param pattern the pattern that will be used to search for artifacts, for example {@code ^.+\.csv}
     * @return {@link List} of {@link Path} to the artifacts
     */
    public static List<Path> getArtifacts(WebDriver driver, String pattern) {
        LOGGER.debug("Getting artifacts by pattern: '{}'.", pattern);
        return listArtifacts(driver)
                .stream()
                // ignore directories
                .filter(name -> !name.endsWith("/"))
                .filter(name -> name.matches(pattern))
                .map(name -> getArtifact(driver, name).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get artifact from the local or remote (if available) auto download folder by name.
     * Remote artifact will be downloaded to the local artifacts folder.
     *
     * @param driver {@link WebDriver}
     * @param name filename with extension
     * @return {@link Optional} of {@link Path}
     */
    public static Optional<Path> getArtifact(WebDriver driver, String name) {
        return getArtifact(driver, name, null);
    }

    /**
     * Get artifact from the local or remote (if available) auto download folder by name.
     * Remote artifact will be downloaded to the local artifacts folder.
     *
     * @param driver {@link WebDriver}
     * @param name filename with extension
     * @param timeout timeout during which the presence of the artifact will be checked
     * @return {@link Optional} of {@link Path}
     */
    public static Optional<Path> getArtifact(WebDriver driver, String name, @Nullable Duration timeout) {
        LOGGER.debug("Trying to get artifact with name: '{}'.", name);
        if (timeout != null) {
            if (timeout.toSeconds() < 1) {
                throw new IllegalArgumentException("Timeout for getting artifact could not be less than one second.");
            }
            FluentWait<WebDriver> wait = new FluentWait<>(driver)
                    .pollingEvery(Duration.ofSeconds(1))
                    .withTimeout(timeout);
            boolean isArtifactPresent = isArtifactPresent(wait, name);
            if (!isArtifactPresent) {
                return Optional.empty();
            }
        }

        Path file = getArtifactsFolder().resolve(name);
        if (Files.exists(file)) {
            LOGGER.debug("Found local artifact. Path: '{}'", file);
            return Optional.of(file);
        }

        URL endpoint = getEndpoint(driver, ARTIFACTS_ENDPOINT, name);
        if (!isEndpointAvailable(endpoint)) {
            LOGGER.debug("Remote artifacts folder is not available. URL: '{}'", endpoint);
            return Optional.empty();
        }

        try {
            LOGGER.debug("Trying download artifact from the URL: '{}'.", endpoint);
            PathUtils.copyFile(endpoint, file);
            LOGGER.debug("Successfully downloaded artifact: '{}'.", file.toAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Cannot download artifact from the remote artifact folder. Message: '%s'.", e.getMessage()),
                    e);
        }

        if (ConfigurationHolder.isReportingEnabled()) {
            // if an error occurs in the agent, it should not crash the test
            try {
                Artifact.attachToTest(name, file);
            } catch (Exception e) {
                LOGGER.error("Cannot attach '{}' artifact with path '{}' to the test.", name, file);
            }
        }
        return Optional.of(file);
    }

    /**
     * Create a file int the artifacts folder and write from the InputStream. If file already exists, it will be removed.<br>
     * File will be registered in the Zebrunner Reporting.
     *
     * @param name the name to be given to the file
     * @param source {@link InputStream}
     * @return {@link Path} to the file in the artifacts folder
     */
    public static Path saveArtifact(String name, InputStream source) {
        Path file = getArtifactsFolder().resolve(name);
        try {
            if (Files.exists(file)) {
                LOGGER.warn("Artifact already exists and it will be removed. Path: '{}'", file);
                Files.delete(file);
            }
            Files.write(file, source.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Cannot save artifact '%s' from the InputStream.", name), e);
        }
        if (ConfigurationHolder.isReportingEnabled()) {
            // if an error occurs in the agent, it should not crash the test
            try {
                Artifact.attachToTest(name, file);
            } catch (Exception e) {
                LOGGER.error("Cannot attach '{}' artifact with path '{}' to the test.", name, file);
            }
        }
        return file;
    }

    /**
     * Copy file into the artifacts folder. If file already exists in the artifacts folder, it will be removed.<br>
     * File will be registered in the Zebrunner Reporting.
     *
     * @param source {@link Path}
     * @return {@link Path} to the file in the artifacts folder
     */
    public static Path saveArtifact(Path source) {
        Path file = getArtifactsFolder().resolve(source.getFileName().toString());

        try {
            if (Files.exists(file)) {
                LOGGER.warn("Artifact already exists and it will be removed. Path: '{}'", file);
                Files.delete(file);
            }
            Files.copy(source, file);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Cannot copy file to the artifacts folder from '%s' to the '%s'. Message: '%s'.",
                    source, file, e.getMessage()), e);
        }

        if (ConfigurationHolder.isReportingEnabled()) {
            // if an error occurs in the agent, it should not crash the test
            try {
                Artifact.attachToTest(file.getFileName().toString(), file);
            } catch (Exception e) {
                LOGGER.error("Cannot attach '{}' artifact with path '{}' to the test.", file.getFileName(), file);
            }
        }
        return file;
    }

    // Converting InputStream to String
    private static String readStream(InputStream in) {
        StringBuilder response = new StringBuilder();
        try (
                InputStreamReader istream = new InputStreamReader(in);
                BufferedReader reader = new BufferedReader(istream)) {
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            // do noting
        }
        return response.toString();
    }

    /**
     * Get {@link Authenticator} using Selenium URL credentials
     *
     * @param seleniumURL Selenium URL
     * @return {@link Optional} of {@link Authenticator}
     */
    private static Optional<Authenticator> getAuthenticator(URL seleniumURL) {
        String userInfo = seleniumURL.getUserInfo();
        // if there are no username or password, do not create Authenticator
        if (userInfo.isEmpty() || ArrayUtils.getLength(userInfo.split(":")) < 2) {
            return Optional.empty();
        }
        // 1-st username, 2-nd password
        String[] credentials = userInfo.split(":");
        return Optional.of(new CustomAuthenticator(credentials[0], credentials[1]));
    }

    /**
     * Get Endpoint URL
     *
     * todo move to the utils class and reuse for other endpoints
     *
     * @param driver {@link WebDriver}
     * @param endpointName for example, {@code download}
     * @param method for example, {@code fileName}
     * @return {@link URL}
     */
    private static URL getEndpoint(WebDriver driver, String endpointName, @Nullable String method) {
        LOGGER.debug("Trying to create URL for endpoint '{}' with method '{}'", endpointName, method);
        String endpoint = String.format("%s/%s/", Configuration.getRequired(WebDriverConfiguration.Parameter.SELENIUM_URL)
                .replace("wd/hub", endpointName),
                DriverListener.castDriver(driver, RemoteWebDriver.class).getSessionId());
        if (method != null) {
            endpoint += method;
        }
        LOGGER.debug("Created endpoint url: {}", endpoint);
        try {
            return new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Pings a HTTP URL. This effectively sends a HEAD request and returns <code>true</code> if the response code is in
     * the 200-399 range.
     * 
     * @param url The HTTP URL to be pinged.
     * @return <code>true</code> if the given HTTP URL has returned response code 200-399 on a HEAD request within the
     *         given timeout, otherwise <code>false</code>.
     */
    private static boolean isEndpointAvailable(URL url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            LOGGER.debug("Endpoint response code:'{}'.", responseCode);
            return (200 <= responseCode && responseCode <= 399);
        } catch (IOException e) {
            LOGGER.debug("Endpoint is not available. Message: '{}'.", e.getMessage(), e);
            return false;
        }
    }

    private static boolean isArtifactPresent(FluentWait<WebDriver> wait, String name) {
        boolean isFound = false;
        try {
            isFound = wait.until(dr -> {
                List<String> list = SessionContext.listArtifacts(dr);
                if (list.contains(name)) {
                    return true;
                }
                return null;
            });
        } catch (TimeoutException e) {
            // do nothing
        }
        return isFound;
    }

    private static class CustomAuthenticator extends Authenticator {

        String username;
        String password;

        public CustomAuthenticator(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password.toCharArray());
        }
    }
}
