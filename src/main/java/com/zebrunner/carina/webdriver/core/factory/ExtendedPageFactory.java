package com.zebrunner.carina.webdriver.core.factory;

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

import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.gui.AbstractPage;
import com.zebrunner.carina.webdriver.gui.AbstractUIObject;
import com.zebrunner.carina.webdriver.locator.Context;
import com.zebrunner.carina.webdriver.locator.ExtendedElementLocator;

// todo investigate how it will work after refactor
public class ExtendedPageFactory extends PageFactory {
    public static void initElementsContext(Object page) {
        Class<?> proxyIn = page.getClass();
        while (proxyIn != AbstractUIObject.class && proxyIn != AbstractPage.class) {
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
                setContextForAbstractUIObject((AbstractUIObject<?>) getParamByField(field, page), contextElement);
            }

            if (List.class.isAssignableFrom(field.getType())) {
                setContextForWebElementsList(field, contextElement, page);
            }
        }
    }

    private static WebElement getElement(Field field, Object page) {
        Context context = field.getAnnotation(Context.class);
        Class<?> pageClass = field.getDeclaringClass();
        Field contextField = null;
        while (contextField == null && !pageClass.isAssignableFrom(AbstractUIObject.class)) {
            try {
                contextField = pageClass.getDeclaredField(context.dependsOn());
            } catch (NoSuchFieldException e) {
                pageClass = pageClass.getSuperclass();
            }
        }

        if (contextField == null){
            throw new RuntimeException("Cannot find context element: " + context.dependsOn());
        }

        try {
            contextField.setAccessible(true);
            AbstractUIObject<?> element = null;
            if (contextField.getType().isAssignableFrom(AbstractUIObject.class)) {
                element = ((AbstractUIObject<?>) contextField.get(page));
            } else if (contextField.getType().isAssignableFrom(List.class)){
                throw new IllegalArgumentException("List couldn't be passed as context element");
            }

            if (element != null && element.getElement() != null) {
                return element.getElement();
            } else {
                throw new RuntimeException("Context WebElement is null!");
            }
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new RuntimeException("Cannot get context field from " + pageClass.getName(), e);
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

    private static void setContextForAbstractUIObject(AbstractUIObject<?> param, SearchContext contextElement) {
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
        try {
            FieldUtils.getDeclaredField(AbstractUIObject.class, "searchContext", true)
                    .set(param, contextElement);
        } catch (Exception e) {
            throw new RuntimeException("Cannot find searchContext field", e);
        }
        Class<?> clazz = param.getClass().getSuperclass();
        if (clazz != AbstractUIObject.class) {
            setContext(clazz, clazz.getDeclaredFields());
        }

    }

    private static void setContextForWebElementsList(Field field, SearchContext contextElement, Object page) {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) {
            return;
        }

        genericType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
        if (ExtendedWebElement.class.isAssignableFrom((Class<?>) genericType)) {

            Object elementsList = getParamByField(field, page);
            if (elementsList instanceof Proxy){

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

        if (AbstractUIObject.class.isAssignableFrom((Class<?>) genericType)) {
            @SuppressWarnings({"unchecked"})
            List<AbstractUIObject> elementsList = (List<AbstractUIObject>) getParamByField(field, page);
            for (AbstractUIObject element : elementsList) {
                setContextForAbstractUIObject(element, contextElement);
            }
        }
    }
}
