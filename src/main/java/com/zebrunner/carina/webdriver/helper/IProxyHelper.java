package com.zebrunner.carina.webdriver.helper;

import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.report.SessionContext;
import com.zebrunner.carina.webdriver.IDriverPool;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.listener.DriverListener;
import de.sstoehr.harreader.HarReader;
import de.sstoehr.harreader.HarReaderException;
import de.sstoehr.harreader.model.Har;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public interface IProxyHelper extends IDriverPool {
    Logger I_PROXY_HELPER_LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Download Har artifact to the session {@code downloads} folder with the specified name
     * 
     * @param fileName name of the file, for example {@code session}. Extension will be added {@code .har}, so the result name of the file:
     *            {@code session.har}
     * @return {@link Optional} of {@link Path} to the har if available, {@link Optional#empty()} otherwise
     */
    default Optional<Path> getHarArtifact(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("Parameter could not be null, blank or empty.");
        }
        try {
            URL endpoint = new URL(String.format("%s/%s/%s/", Configuration.getRequired(WebDriverConfiguration.Parameter.SELENIUM_URL)
                    .replace("wd/hub", "download"),
                    "har",
                    DriverListener.castDriver(getDriver(), RemoteWebDriver.class).getSessionId()));
            return Optional.of(PathUtils.copyFile(endpoint, SessionContext.getArtifactsFolder().resolve(fileName)));
        } catch (MalformedURLException e) {
            return ExceptionUtils.rethrow(e);
        } catch (IOException e) {
            I_PROXY_HELPER_LOGGER.debug("Exception when try to download har artifact: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Read har file
     * 
     * @param path {@link Path}
     * @return {@link Har}
     */
    default Har readHar(Path path) {
        try {
            return new HarReader().readFromFile(path.toFile());
        } catch (HarReaderException e) {
            return ExceptionUtils.rethrow(e);
        }
    }
}
