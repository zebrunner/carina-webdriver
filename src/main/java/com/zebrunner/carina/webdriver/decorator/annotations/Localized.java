/*******************************************************************************
 * Copyright 2020-2022 Zebrunner Inc (https://www.zebrunner.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.zebrunner.carina.webdriver.decorator.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Checks/generates locale for marked element
 *
 * focus() - L10N will search/generate locale key with certain pattern.
 * CLASS_DECLARE (default): Component.element or Page.element
 * ELEMENT: element
 * FULL_PATH: Page.Component.Component.element
 *
 * localeName() - override L10N search/generate elements name.
 * CLASS_DECLARE (default): Component.customName or Page.customName
 * ELEMENT: customName
 * FULL_PATH: Page.Component.Component.customName
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Localized {

    NameFocus focus() default NameFocus.CLASS_DECLARE;

    String localeName() default "";

    enum NameFocus {
        FULL_PATH,
        CLASS_DECLARE,
        ELEMENT;
    }
}
