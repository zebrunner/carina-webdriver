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
package com.zebrunner.carina.utils.factory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.openqa.selenium.WebDriver;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.exception.RequiredCtorNotFoundException;
import com.zebrunner.carina.utils.factory.DeviceType.Type;
import com.zebrunner.carina.webdriver.IDriverPool;
import com.zebrunner.carina.webdriver.config.WebDriverConfiguration;
import com.zebrunner.carina.webdriver.device.Device;
import com.zebrunner.carina.webdriver.gui.AbstractPage;

public interface ICustomTypePageFactory extends IDriverPool {

    Logger PAGEFACTORY_LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    String VERSION_SPLITTER = "\\.";
    String INTEGER_STR = "class java.lang.Integer";
    String INT_STR = "int";
    String LONG_OBJ_STR = "class java.lang.Long";
    String LONG_STR = "long";
    String DOUBLE_OBJ_STR = "class java.lang.Double";
    String DOUBLE_STR = "double";
    Reflections REFLECTIONS = Configuration.getRequired(WebDriverConfiguration.Parameter.PAGE_RECURSIVE_REFLECTION, Boolean.class)
            ? new Reflections(new ConfigurationBuilder().addUrls(Arrays.stream(Package.getPackages())
                    .map(Package::getName)
                    .map(s -> s.split("\\.")[0])
                    .distinct()
                    .map(ClasspathHelper::forPackage).reduce((c1, c2) -> {
                        Collection<URL> c3 = new HashSet<>();
                        c3.addAll(c1);
                        c3.addAll(c2);
                        return c3;
                    }).orElseThrow())
                    .addScanners(Scanners.SubTypes))
            : new Reflections(new ConfigurationBuilder()
                    .setScanners(Scanners.SubTypes)
                    .setUrls(ClasspathHelper.forJavaClassPath()));

    default <T extends AbstractPage> T initPage(Class<T> parentClass, Object... parameters) {
        return initPage(getDriver(), parentClass, parameters);
    }

    default <T extends AbstractPage> T initPage(WebDriver driver, Class<T> parentClass, Object... parameters) {
        Objects.requireNonNull(driver, String.format("Page isn't created because driver is null. Thread id: %s", Thread.currentThread().getId()));
        Set<Class<? extends T>> setClasses = REFLECTIONS.getSubTypesOf(parentClass);
        PAGEFACTORY_LOGGER.debug("Relatives classes count: {}", setClasses.size());
        Class<? extends T> versionClass = null;
        Class<? extends T> majorVersionClass = null;
        Class<? extends T> deviceClass = null;
        Class<? extends T> familyClass = null;
        Class<? extends T> requiredClass = null;
        Device device = getDevice(driver);
        Type screenType = device.getDeviceType();

        // default version in case if it is desktop driver
        String deviceVersion = "1";
        if (!device.getOsVersion().isEmpty()) {
            deviceVersion = device.getOsVersion();
        }
        String majorVersionNumber = deviceVersion.split(VERSION_SPLITTER)[0];
        PAGEFACTORY_LOGGER.debug("Major version of device OS: {}", majorVersionNumber);
        for (Class<? extends T> clazz : setClasses) {
            if (clazz.getAnnotation(DeviceType.class) == null || clazz.getAnnotation(DeviceType.class).parentClass() != parentClass) {
                PAGEFACTORY_LOGGER.debug("Removing as parentClass ({}) is not satisfied or due to absence of @DeviceType annotation on class: {}",
                        parentClass.getName(), clazz.getName());
                continue;
            }
            DeviceType dt = clazz.getAnnotation(DeviceType.class);

            PAGEFACTORY_LOGGER.debug("Expected screenType: {}, Actual screenType: {}", screenType, dt.pageType());
            if (dt.pageType().equals(screenType)) {
                String[] versions = dt.version();
                if (Arrays.asList(versions).contains(deviceVersion)) {
                    PAGEFACTORY_LOGGER.debug("Expected version: {}", deviceVersion);
                    String versionsAsString = Arrays.toString(versions);
                    PAGEFACTORY_LOGGER.debug("Actual versions: {}", versionsAsString);
                    versionClass = clazz;
                    break;
                }

                for (String version : dt.version()) {
                    if (version.split(VERSION_SPLITTER)[0].equals(majorVersionNumber)) {
                        majorVersionClass = clazz;
                        PAGEFACTORY_LOGGER.debug("Class was chosen by major version number of device");
                        break;
                    }
                }

                deviceClass = clazz;
                continue;
            }
            if (dt.pageType().getFamily().equals(screenType.getFamily())) {
                PAGEFACTORY_LOGGER.debug("Family class '{}' correspond to required page.", screenType.getFamily());
                familyClass = clazz;
            }
        }
        try {
            if (versionClass != null) {
                PAGEFACTORY_LOGGER.debug("Instance by version and platform will be created.");
                requiredClass = versionClass;
            } else if (majorVersionClass != null) {
                PAGEFACTORY_LOGGER.debug("Instance by major version and platform will be created.");
                requiredClass = majorVersionClass;
            } else if (deviceClass != null) {
                PAGEFACTORY_LOGGER.debug("Instance by platform will be created.");
                requiredClass = deviceClass;
            } else if (familyClass != null) {
                PAGEFACTORY_LOGGER.debug("Instance by family will be created.");
                requiredClass = familyClass;
            } else {
                throw new RuntimeException(
                        String.format("There is no any class that satisfy to required conditions: [parent class - %s], [device type - %s]",
                                parentClass.getName(), screenType));
            }
            // handle cases where we have only WebDriver as ctor parameter
            if (parameters.length == 0) {
                parameters = new Object[] { driver };
            }
            PAGEFACTORY_LOGGER.debug("Invoking constructor for {}", requiredClass);
            Constructor<? extends T> requiredCtor = getConstructorByParams(requiredClass, parameters);

            return requiredCtor.newInstance(parameters);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            PAGEFACTORY_LOGGER.debug(
                    "Discovered one of the InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException");
            if (e.getCause() == null) {
                throw new RuntimeException("Unable to instantiate page!\n" + e.getMessage(), e);
            } else {
                throw new RuntimeException("Unable to instantiate page! " + e.getCause().getMessage(), e.getCause());
            }
        }
    }

    /**
     * Get constructor from clazz that satisfy specific range of parameters
     * (using Reflection)
     *
     * @param <T>        This is the type parameter
     * @param clazz      Class clazz
     * @param parameters Object... parameters
     * @return constructor
     */
    @SuppressWarnings("unchecked")
    default <T extends AbstractPage> Constructor<? extends T> getConstructorByParams(Class<T> clazz, Object... parameters) {
        PAGEFACTORY_LOGGER.debug("Attempt to find costructor that satisfy to following parameters: {}", Arrays.toString(parameters));
        Class<?>[] parametersTypes;
        List<Class<?>> parametersTypesList = new ArrayList<>();
        for (Object param : parameters) {
            parametersTypesList.add(param.getClass());
        }
        parametersTypes = parametersTypesList.toArray(new Class<?>[parametersTypesList.size()]);
        Constructor<?> requiredCtor = null;
        Constructor<?>[] ctors = clazz.getDeclaredConstructors();
        PAGEFACTORY_LOGGER.debug("Class {} contains {} ctors ", clazz, ctors.length);
        for (Constructor<?> constructor : ctors) {
            PAGEFACTORY_LOGGER.debug("Constructor: {}", constructor);
        }
        for (Constructor<?> constructor : ctors) {
            Class<?>[] ctorTypes = constructor.getParameterTypes();

            // Check if passed parameters quantity satisfy to constructor's
            // parameters size
            if (parametersTypes.length != ctorTypes.length) {
                PAGEFACTORY_LOGGER.debug("Ctors quantity doesn't satisfy to requirements. " + "Expected: {}. Actual: {}", parametersTypes.length,
                        ctorTypes.length);
                continue;
            }
            if (parametersTypes.length == 0) {
                requiredCtor = constructor;
                break;
            }
            int foundParams = 0;

            // comparison logic for passed parameters type and ctor' parameters
            // type
            for (Class<?> ctorType : ctorTypes) {
                for (Class<?> paramType : parametersTypes) {
                    if (paramType.isInstance(ctorType) || ctorType.isAssignableFrom(paramType) || comparePrimitives(ctorType, paramType)) {
                        foundParams++;
                        break;
                    }
                }
            }

            if (foundParams == ctorTypes.length) {
                requiredCtor = constructor;
            }

        }

        if (null == requiredCtor) {
            throw new RequiredCtorNotFoundException();
        }

        return (Constructor<? extends T>) requiredCtor;
    }

    /**
     * Method to compare primitives with corresponding wrappers
     *
     * @param obj1 Object obj1
     * @param obj2 Object obj2
     * @return boolean result
     */
    default boolean comparePrimitives(Object obj1, Object obj2) {
        String s = obj1.toString();
        if (INT_STR.equals(s) || INTEGER_STR.equals(s)) {
            return INTEGER_STR.equalsIgnoreCase(obj2.toString()) || obj2.toString().equalsIgnoreCase(INT_STR);
        } else if (LONG_OBJ_STR.equals(s) || LONG_STR.equals(s)) {
            return LONG_OBJ_STR.equalsIgnoreCase(obj2.toString()) || obj2.toString().equalsIgnoreCase(LONG_STR);
        } else if (DOUBLE_OBJ_STR.equals(s) || DOUBLE_STR.equals(s)) {
            return DOUBLE_OBJ_STR.equalsIgnoreCase(obj2.toString()) || obj2.toString().equalsIgnoreCase(DOUBLE_STR);
        }
        return false;
    }
}
