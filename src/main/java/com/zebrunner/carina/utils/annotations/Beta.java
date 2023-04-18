package com.zebrunner.carina.utils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Signifies that a public API (public class, method or field) is subject to
 * incompatible changes, or even removal, in a future release.
 *
 * todo move to the carina-utils module
 *
 * @author Andrei Kamarouski
 * @since 1.0.3
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface Beta {
}
