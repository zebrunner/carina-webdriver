package com.zebrunner.carina.webdriver.core.factory;

import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.gui.AbstractUIObject;
import com.zebrunner.carina.webdriver.locator.Context;
import com.zebrunner.carina.webdriver.locator.ExtendedElementLocator;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.FieldDecorator;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ExtendedPageFactory extends PageFactory {

    public static void initElements(FieldDecorator decorator, Object page) {
        Class<?> proxyIn = page.getClass();
        while (proxyIn != AbstractUIObject.class) {
            try {
                Method proxyFields = PageFactory.class.getDeclaredMethod("proxyFields", FieldDecorator.class, Object.class, Class.class);
                proxyFields.setAccessible(true);
                proxyFields.invoke(null, decorator, page, proxyIn);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            proxyIn = proxyIn.getSuperclass();
        }
    }

    public static void initElementsContext(Object page) {
        Class<?> proxyIn = page.getClass();
        while (proxyIn != AbstractUIObject.class) {
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

            if (ExtendedWebElement.class.isAssignableFrom(field.getType())) {
                setContextForWebElement((ExtendedWebElement) getParamByField(field, page), contextElement);
            }

            if (AbstractUIObject.class.isAssignableFrom(field.getType())) {
                setContextForAbstractUIObject((AbstractUIObject) getParamByField(field, page), contextElement);
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

        if (contextField == null) {
            throw new RuntimeException("Cannot find context element: " + context.dependsOn());
        }

        try {
            contextField.setAccessible(true);
            ExtendedWebElement element = null;
            if (contextField.getType().isAssignableFrom(AbstractUIObject.class)) {
                element = ((AbstractUIObject) contextField.get(page)).getRootExtendedElement();
            } else if (contextField.getType().isAssignableFrom(ExtendedWebElement.class)) {
                element = ((ExtendedWebElement) contextField.get(page));
            } else if (contextField.getType().isAssignableFrom(List.class)) {
                throw new RuntimeException("List couldn't be passed as context element");
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

    private static void setContextForWebElement(ExtendedWebElement param, SearchContext contextElement) {
        if (param.getElement() instanceof Proxy) {
            WebElement paramWeb = param.getElement();

            InvocationHandler innerProxy = Proxy.getInvocationHandler(paramWeb);
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

    private static void setContextForAbstractUIObject(AbstractUIObject param, SearchContext contextElement) {
        setContextForWebElement(param.getRootExtendedElement(), contextElement);
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

        if (AbstractUIObject.class.isAssignableFrom((Class<?>) genericType)) {
            @SuppressWarnings({"unchecked"})
            List<AbstractUIObject> elementsList = (List<AbstractUIObject>) getParamByField(field, page);
            for (AbstractUIObject element : elementsList) {
                setContextForAbstractUIObject(element, contextElement);
            }
        }
    }
}
