package com.zebrunner.carina.webdriver.locator;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public abstract class BasePlatformCondition {

    private final LocatorCreatorContext locatorCreatorContext;

    protected BasePlatformCondition(LocatorCreatorContext locatorCreatorContext) {
        this.locatorCreatorContext = locatorCreatorContext;
    }

    public void assertValidAnnotations(FindByPlatform.Type[] values) {
        // do nothing
    }

    public boolean isConditionApply(FindByPlatform.Type[] values) {
        return Arrays.stream(values)
                .anyMatch(this::isPlatformApplicable);
    }

    private boolean isPlatformApplicable(FindByPlatform.Type type) {
        boolean applicable = false;
        switch (type) {
            case IOS:
            case TVOS:
            case ANDROID:
            case ANDROID_TV:
                applicable = type.getName().equalsIgnoreCase(locatorCreatorContext.getPlatform());
                break;
            case DESKTOP:
                applicable = !StringUtils.isBlank(locatorCreatorContext.getBrowserName());
                break;
            default:
                break;
        }
        return applicable;
    }
}
