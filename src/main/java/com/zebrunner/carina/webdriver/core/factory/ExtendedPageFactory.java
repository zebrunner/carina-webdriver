package com.zebrunner.carina.webdriver.core.factory;

import com.zebrunner.carina.webdriver.locator.Context;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.FieldDecorator;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ExtendedPageFactory extends PageFactory {
    public static void initElements(FieldDecorator decorator, Object page) {
        //first init fields without @Context annotation
        proxyFields(decorator, page, true);
        //after that init fields with @Context annotation
        proxyFields(decorator, page, false);
    }

    private static void proxyFields(FieldDecorator decorator, Object page, boolean isContextDependingField) {
        Class<?> proxyIn = page.getClass();
        while (proxyIn != Object.class) {
            List<Field> fields = Arrays.stream(proxyIn.getDeclaredFields())
                    .filter(field ->
                            Objects.isNull(field.getDeclaredAnnotation(Context.class)) == isContextDependingField)
                    .collect(Collectors.toList());

            proxyCurrentClassFields(decorator, page, fields);
            proxyIn = proxyIn.getSuperclass();
        }
    }

    private static void proxyCurrentClassFields(FieldDecorator decorator, Object page, List<Field> fieldList) {
        for (Field field : fieldList) {
            Object value = decorator.decorate(page.getClass().getClassLoader(), field);
            if (value != null) {
                try {
                    field.setAccessible(true);
                    field.set(page, value);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
