package com.zebrunner.carina.webdriver.core.factory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.webdriver.AbstractContext;
import com.zebrunner.carina.webdriver.gui.AbstractUIObject;
import com.zebrunner.carina.webdriver.locator.Context;
import com.zebrunner.carina.webdriver.locator.ExtendedElementLocator;

public class ExtendedPageFactory extends PageFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void initElementsContext(Object page) {
        Class<?> proxyIn = page.getClass();

        while (proxyIn != AbstractContext.class) {
            if (proxyIn == Object.class) {
                LOGGER.warn("Context is not inherited from either AbstractUIObject or AbstractPage! Investigate class: {}", proxyIn);
            }
            Field[] contextFields = Arrays.stream(proxyIn.getDeclaredFields())
                    .filter(field -> !Objects.isNull(field.getDeclaredAnnotation(Context.class)))
                    .toArray(Field[]::new);

            setContext(page, contextFields);
            proxyIn = proxyIn.getSuperclass();
        }
    }

    private static void setContext(Object page, Field[] fields) {
        for (Field field : fields) {
            WebElement contextElement = getElement(field, page);

            if (AbstractUIObject.class.isAssignableFrom(field.getType())) {
                setContextForElement((AbstractUIObject<?>) getParamByField(field, page), contextElement);
            }

            if (List.class.isAssignableFrom(field.getType())) {
                setContextForList(field, contextElement, page);
            }
        }
    }

    private static WebElement getElement(Field field, Object page) {
        Context context = field.getAnnotation(Context.class);
        Class<?> contextElClass = field.getDeclaringClass();
        Field contextField = null;
        while (contextField == null && !contextElClass.isAssignableFrom(AbstractContext.class)) {
            try {
                contextField = contextElClass.getDeclaredField(context.dependsOn());
            } catch (NoSuchFieldException e) {
                contextElClass = contextElClass.getSuperclass();
            }
        }

        if (contextField == null) {
            throw new IllegalArgumentException("Cannot find context element: " + context.dependsOn());
        }

        try {
            contextField.setAccessible(true);
            AbstractUIObject<?> element = null;
            if (AbstractUIObject.class.isAssignableFrom(contextField.getType())) {
                element = ((AbstractUIObject<?>) contextField.get(page));
            } else if (List.class.isAssignableFrom(contextField.getType())) {
                throw new IllegalArgumentException("List couldn't be passed as context element");
            }

            if (element != null && element.getElement() != null) {
                return element.getElement();
            } else {
                throw new RuntimeException("Context WebElement for " + context.dependsOn() + " is null!");
            }
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new RuntimeException("Cannot get context field from " + contextElClass.getName(), e);
        }
    }

    private static Object getParamByField(Field field, Object page) {
        Object param;
        try {
            field.setAccessible(true);
            param = field.get(page);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error on finding param: " + field.getName(), e);
        }
        return param;
    }

    private static void setContextForElement(AbstractUIObject<?> param, SearchContext contextElement) {
        if (param.getElement() instanceof Proxy) {
            InvocationHandler innerProxy = Proxy.getInvocationHandler(param.getElement());
            ExtendedElementLocator locator;
            try {
                locator = (ExtendedElementLocator) (FieldUtils.getDeclaredField(innerProxy.getClass(), "locator", true))
                        .get(innerProxy);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error finding locator for created ExtendedWebElement.", e);
            }

            locator.setSearchContext(contextElement);
        }
        param.setSearchContext(contextElement);
    }

    private static void setContextForList(Field field, SearchContext contextElement, Object page) {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) {
            return;
        }

        genericType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
        if (AbstractUIObject.class.isAssignableFrom((Class<?>) genericType)) {
            Object elementsList = getParamByField(field, page);
            if (elementsList instanceof Proxy) {
                InvocationHandler innerProxy = Proxy.getInvocationHandler(elementsList);
                ExtendedElementLocator locator;
                try {
                    locator = (ExtendedElementLocator) (FieldUtils.getDeclaredField(innerProxy.getClass(), "locator", true))
                            .get(innerProxy);
                } catch (Exception e) {
                    throw new RuntimeException("Error finding locator for created List<ExtendedWebElement>.", e);
                }
                locator.setSearchContext(contextElement);
            }
        }
    }
}
