package com.zebrunner.carina.utils;

import com.zebrunner.carina.utils.common.CommonUtils;

public interface ICommonUtils {

    /**
     * Pause for specified timeout.
     *
     * @param timeout in seconds.
     */
    default void pause(long timeout) {
        CommonUtils.pause(timeout);
    }

    default void pause(double timeout) {
        CommonUtils.pause(timeout);
    }

    default void pause(Double timeout) {
        CommonUtils.pause(timeout);
    }
}
