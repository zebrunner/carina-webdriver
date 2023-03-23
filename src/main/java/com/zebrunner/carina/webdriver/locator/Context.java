package com.zebrunner.carina.webdriver.locator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines specific context for annotated element, based on dependsOn element.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.TYPE })
public @interface Context {
    /**
     * Should be passed element's name.
     * Defines based on which element the context of the annotated element will be instantiated.
     */
    String dependsOn();
}
