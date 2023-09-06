package com.zebrunner.carina.webdriver.locator;

import org.openqa.selenium.support.FindBy;

public class PlatformCondition extends BasePlatformCondition implements FindCondition<FindByPlatform> {

    public PlatformCondition(LocatorCreatorContext locatorCreatorContext) {
        super(locatorCreatorContext);
    }

    @Override
    public void assertValidAnnotations(FindByPlatform annotation) {
        super.assertValidAnnotations(annotation.value());
    }

    @Override
    public boolean isConditionApply(FindByPlatform annotation) {
        return super.isConditionApply(annotation.value());
    }

    @Override
    public FindBy getFindBy(FindByPlatform annotation) {
        return annotation.findBy();
    }
}
