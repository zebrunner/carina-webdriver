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
package com.zebrunner.carina.webdriver.listener;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.http.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.common.CommonUtils;
import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;

/**
 * EventFiringSeleniumCommandExecutor triggers event listener before/after execution of the command.
 */
public class EventFiringSeleniumCommandExecutor extends HttpCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String CONNECTION_TIMED_OUT_EXCEPTION = "connection timed out";

    public EventFiringSeleniumCommandExecutor(ClientConfig clientConfig) {
        super(clientConfig);
    }

    @Override
    public Response execute(Command command) throws IOException {
        Response response = null;
        int retry = 2; // extra retries to execute command
        Number pause = Configuration.getRequired(WebDriverConfiguration.Parameter.EXPLICIT_TIMEOUT, Integer.class) / retry;
        while (retry >= 0) {
            response = super.execute(command);
            if (response.getValue() instanceof WebDriverException) {
                LOGGER.debug("CarinaCommandExecutor catched: {}", response.getValue());
                if (DriverCommand.QUIT.equals(command.getName())) {
                    // do not retry on quit command (grid will close it forcibly anyway)
                    break;
                }

                String msg = response.getValue().toString();
                if (msg.contains(SpecialKeywords.DRIVER_CONNECTION_REFUSED) ||
                        msg.contains(SpecialKeywords.DRIVER_CONNECTION_REFUSED2) ||
                        msg.contains(SpecialKeywords.DRIVER_TARGET_FRAME_DETACHED) ||
                        msg.contains(CONNECTION_TIMED_OUT_EXCEPTION)) {
                    LOGGER.warn("Enabled command executor retries: {}", msg);
                    CommonUtils.pause(pause);
                } else {
                    // do not retry for non "driver connection refused" errors!
                    break;
                }
            } else {
                // do nothing as response already contains all the information we need
                break;
            }
            retry--;
        }

        return response;
    }

}
