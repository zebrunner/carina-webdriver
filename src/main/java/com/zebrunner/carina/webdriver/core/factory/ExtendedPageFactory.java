package com.zebrunner.carina.webdriver.core.factory;

import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.gui.AbstractUIObject;
import com.zebrunner.carina.webdriver.locator.Context;
import com.zebrunner.carina.webdriver.locator.ExtendedElementLocator;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Objects;

public class ExtendedPageFactory extends PageFactory {
    public static void initElementsContext(Object page) {
        Class<?> proxyIn = page.getClass();
        while (proxyIn != AbstractUIObject.class) {
            setContext(page, proxyIn);
            proxyIn = proxyIn.getSuperclass();
        }
    }

    private static void setContext(Object page, Class<?> proxyIn) {
        Field[] fields = Arrays.stream(proxyIn.getDeclaredFields())
                .filter(field -> !Objects.isNull(field.getDeclaredAnnotation(Context.class)))
                .toArray(Field[]::new);

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                ExtendedWebElement param = (ExtendedWebElement) field.get(page);
                WebElement contextElement = getElement(field,page);
                if (param.getElement() instanceof Proxy){
                    WebElement paramWeb = param.getElement();
                    InvocationHandler innerProxy = Proxy.getInvocationHandler(paramWeb);
                    ExtendedElementLocator locator = (ExtendedElementLocator) (FieldUtils.getDeclaredField(innerProxy.getClass(), "locator", true))
                            .get(innerProxy);
                    locator.setSearchContext(contextElement);
                }
                param.setSearchContext(contextElement);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Can't set context for null element " + field.getName(), e);
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

        try {
            contextField.setAccessible(true);
            return ((ExtendedWebElement) contextField.get(page)).getElement();
        } catch (NullPointerException e) {
            throw new RuntimeException("Cannot find context element: " + context.dependsOn(), e);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new RuntimeException("Cannot get context field from " + pageClass.getName(), e);
        }
    }
}
