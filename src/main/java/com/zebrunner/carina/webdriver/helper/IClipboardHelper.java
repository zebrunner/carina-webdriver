package com.zebrunner.carina.webdriver.helper;

import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.webdriver.IDriverPool;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.decorators.Decorated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface IClipboardHelper extends IDriverPool {
    Logger I_CLIPBOARD_HELPER_LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Get clipboard text
     *
     * @return String saved in clipboard
     */
    default String getClipboardText() {
        try {
            I_CLIPBOARD_HELPER_LOGGER.debug("Trying to get clipboard from remote machine with hub...");
            String url = getSelenoidClipboardUrl(getDriver());
            String username = getField(url, 1);
            String password = getField(url, 2);

            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("GET");

            if (!username.isEmpty() && !password.isEmpty()) {
                String usernameColonPassword = username + ":" + password;
                String basicAuthPayload = "Basic " + Base64.getEncoder().encodeToString(usernameColonPassword.getBytes());
                con.addRequestProperty("Authorization", basicAuthPayload);
            }

            String clipboardText = "";
            int status = con.getResponseCode();
            if (200 <= status && status <= 299) {
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder sb = new StringBuilder();
                while ((inputLine = br.readLine()) != null) {
                    sb.append(inputLine);
                }
                br.close();
                clipboardText = sb.toString();
            } else {
                I_CLIPBOARD_HELPER_LOGGER.debug("Trying to get clipboard from local java machine...");
                clipboardText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            }

            clipboardText = clipboardText.replace("\n", "");
            I_CLIPBOARD_HELPER_LOGGER.info("Clipboard: {}", clipboardText);
            return clipboardText;
        } catch (Exception e) {
            throw new RuntimeException("Error when try to get clipboard text.", e);
        }
    }

    /**
     * Set text to clipboard
     *
     * @param text text
     * @return true if successful, false otherwise
     */
    default boolean setClipboardText(String text) {
        boolean isSuccessful = false;
        try {
            I_CLIPBOARD_HELPER_LOGGER.debug("Trying to set text to clipboard on the remote machine with hub...");
            String url = getSelenoidClipboardUrl(getDriver());
            String username = getField(url, 1);
            String password = getField(url, 2);

            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) new URL(url)
                    .openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);

            if (!username.isEmpty() && !password.isEmpty()) {
                String usernameColonPassword = username + ":" + password;
                String basicAuthPayload = "Basic " + Base64.getEncoder().encodeToString(usernameColonPassword.getBytes());
                con.addRequestProperty("Authorization", basicAuthPayload);
            }

            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(text);
            wr.flush();
            wr.close();

            int status = con.getResponseCode();
            if (!(200 <= status && status <= 299)) {
                throw new IOException("Response status code is not successful");
            }
            isSuccessful = true;
        } catch (Exception e) {
            I_CLIPBOARD_HELPER_LOGGER.error("Error occurred when try to set clipboard to remote machine with hub", e);
            try {
                I_CLIPBOARD_HELPER_LOGGER.debug("Trying to set clipboard to the local java machine...");
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
                isSuccessful = true;
            } catch (Exception ex) {
                I_CLIPBOARD_HELPER_LOGGER.error("Error occurred when try to set clipboard to the local machine", ex);
            }
        }
        return isSuccessful;
    }

    private String getSelenoidClipboardUrl(WebDriver driver) {
        String seleniumHost = Configuration.getRequired(WebDriverConfiguration.Parameter.SELENIUM_URL).replace("wd/hub", "clipboard/");
        WebDriver drv = (driver instanceof Decorated<?>) ? (WebDriver) ((Decorated<?>) driver).getOriginal() : driver;
        String sessionId = ((RemoteWebDriver) drv).getSessionId().toString();
        String url = seleniumHost + sessionId;
        I_CLIPBOARD_HELPER_LOGGER.debug("url: {}", url);
        return url;
    }

    private String getField(String url, int position) {
        Pattern pattern = Pattern.compile(".*:\\/\\/(.*):(.*)@");
        Matcher matcher = pattern.matcher(url);

        return matcher.find() ? matcher.group(position) : "";
    }
}
