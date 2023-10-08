package com.zebrunner.carina.webdriver.helper;

import com.zebrunner.carina.utils.common.CommonUtils;

public interface ICommonsHelper {

    /**
     * Pause for specified timeout.
     *
     * @param timeout in seconds.
     */
    default void pause(long timeout) {
        CommonUtils.pause(timeout);
    }

    default void pause(Double timeout) {
        CommonUtils.pause(timeout);
    }

}
