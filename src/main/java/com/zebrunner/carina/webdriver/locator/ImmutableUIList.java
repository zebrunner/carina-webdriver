package com.zebrunner.carina.webdriver.locator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.openqa.selenium.Beta;

/**
 * For a list of elements that are known to be the same size and in the same order.
 */
@Beta
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface ImmutableUIList {
}
