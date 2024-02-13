package com.zebrunner.carina.webdriver.core.factory;

import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.locator.Context;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.openqa.selenium.support.PageFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

public class ExtendedPageFactory extends PageFactory {
    public static void reinitElementsContext(Object page) {
        try {
            List<Field> fields = FieldUtils.getAllFieldsList(page.getClass())
                    .stream()
                    .filter(field -> field.getType().isAssignableFrom(ExtendedWebElement.class))
                    .collect(Collectors.toList());
            List<Field> annotatedByContextFields = fields.parallelStream()
                    .filter(field -> field.isAnnotationPresent(Context.class))
                    .collect(Collectors.toList());

            if (!annotatedByContextFields.isEmpty()) {
                for (Field field : annotatedByContextFields) {
                    String dependsOn = field.getAnnotation(Context.class).dependsOn();
                    Field context = fields.stream()
                            .filter(f -> StringUtils.equals(f.getName(), dependsOn))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Provided invalid 'Context' field name - there are no such field."));
                    ((ExtendedWebElement) FieldUtils.readField(field, page, true))
                            .setSearchContext((ExtendedWebElement) FieldUtils.readField(context, page, true));
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            ExceptionUtils.rethrow(e);
        }

    }
}
