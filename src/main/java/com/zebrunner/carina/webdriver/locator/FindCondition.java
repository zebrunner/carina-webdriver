package com.zebrunner.carina.webdriver.locator;

import org.openqa.selenium.support.FindBy;

import java.lang.annotation.Annotation;

public interface FindCondition<T extends Annotation> {

    void assertValidAnnotations(T annotation);

    boolean isConditionApply(T annotation);

    FindBy getFindBy(T annotation);

}
