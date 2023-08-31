package com.zebrunner.carina.webdriver.locator;

import org.openqa.selenium.support.FindBy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PlatformsCondition extends BasePlatformCondition implements FindCondition<FindByPlatform.FindByPlatforms> {

    private final Set<FindByPlatform.Type> ALREADY_DEFINED_PLATFORMS = new HashSet<>();

    public PlatformsCondition(LocatorCreatorContext locatorCreatorContext) {
        super(locatorCreatorContext);
    }

    @Override
    public void assertValidAnnotations(FindByPlatform.FindByPlatforms annotation) {
        for (FindByPlatform findByPlatform : annotation.value()) {
            super.assertValidAnnotations(findByPlatform.value());

            for (FindByPlatform.Type type : findByPlatform.value()) {
                if (ALREADY_DEFINED_PLATFORMS.contains(type)) {
                    throw new IllegalArgumentException("If you use a '@FindByPlatform' annotation, you must use unique platforms only");
                }
            }

            ALREADY_DEFINED_PLATFORMS.addAll(Arrays.asList(findByPlatform.value()));
        }
    }

    @Override
    public boolean isConditionApply(FindByPlatform.FindByPlatforms annotation) {
        return Arrays.stream(annotation.value())
                .anyMatch(findByPlatform -> super.isConditionApply(findByPlatform.value()));
    }

    @Override
    public FindBy getFindBy(FindByPlatform.FindByPlatforms annotation) {
        return Arrays.stream(annotation.value())
                .filter(findByPlatform -> isConditionApply(findByPlatform.value()))
                .findFirst()
                .map(findByPlatform -> getFindBy(findByPlatform.findBy()))
                .orElse(null);
    }
}
