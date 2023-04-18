package com.zebrunner.carina.utils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Program elements annotated by {@link Internal} for Carina Framework internal use only.
 *
 * Such elements are not public by design and likely to be removed, have their
 * signature change, or have their access level decreased from public to
 * protected, package, or private in future versions of Carina Framework without notice.
 * 
 * todo move to the carina-utils module
 *
 * @author Andrei Kamarouski
 * @since 1.0.3
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Internal {
    String value() default "";

    /**
     * The Carina Framework version when an element was declared internal.
     */
    String since() default "";
}
