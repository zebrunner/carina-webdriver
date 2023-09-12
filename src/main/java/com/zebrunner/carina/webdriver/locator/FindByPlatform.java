package com.zebrunner.carina.webdriver.locator;

import org.openqa.selenium.support.FindBy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
@FindConditional(byCondition = PlatformCondition.class)
@Repeatable(FindByPlatform.FindByPlatforms.class)
public @interface FindByPlatform {

    FindBy findBy();

    Type[] value();

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    @FindConditional(byCondition = PlatformsCondition.class)
    @interface FindByPlatforms {

        FindByPlatform[] value();

    }

    enum Type {
        DESKTOP(""),
        IOS("iOS"),
        TVOS("tvOS"),
        ANDROID("Android"),
        ANDROID_TV("android_tv");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
