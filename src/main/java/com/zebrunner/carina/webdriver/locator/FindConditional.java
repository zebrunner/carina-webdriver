package com.zebrunner.carina.webdriver.locator;

import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface FindConditional {

    Class<? extends FindCondition<?>> byCondition();

    class Builder {

        private Builder() {
            //hide
        }

        public static Optional<By> buildIt(Field field, LocatorCreatorContext locatorCreatorContext) {
            return getFirstConditionalFindBy(field, locatorCreatorContext)
                    .map(fb -> new FindBy.FindByBuilder().buildIt(fb, field));
        }

        private static Optional<FindBy> getFirstConditionalFindBy(Field field, LocatorCreatorContext locatorCreatorContext) {
            FindBy findBy = null;
            for (Annotation annotation : field.getDeclaredAnnotations()) {
                FindConditional findConditional = null;
                for (Annotation ann : annotation.annotationType().getDeclaredAnnotations()) {
                    if (ann.annotationType().isAssignableFrom(FindConditional.class)) {
                        findConditional = (FindConditional) ann;
                        break;
                    }
                }

                if (findConditional != null) {
                    Class<? extends FindCondition<?>> conditionClass = findConditional.byCondition();
                    findBy = instantiateCondition(conditionClass, locatorCreatorContext)
                            .filter(condition -> {
                                condition.assertValidAnnotations(annotation);
                                return condition.isConditionApply(annotation);
                            })
                            .map(condition -> condition.getFindBy(annotation))
                            .orElse(null);
                }

                if (findBy != null) {
                    break;
                }
            }
            return Optional.ofNullable(findBy);
        }

        @SuppressWarnings("unchecked")
        private static Optional<FindCondition<Annotation>> instantiateCondition(Class<? extends FindCondition<?>> conditionClass, LocatorCreatorContext locatorCreatorContext) {
            FindCondition<Annotation> condition = null;
            try {
                Optional<Constructor<?>> parameterizedConstructor = Arrays.stream(conditionClass.getDeclaredConstructors())
                        .filter(constructor -> Arrays.equals(constructor.getParameterTypes(), new Class[]{LocatorCreatorContext.class}))
                        .findFirst();
                if (parameterizedConstructor.isPresent()) {
                    condition = (FindCondition<Annotation>) parameterizedConstructor.get().newInstance(locatorCreatorContext);
                } else {
                    condition = (FindCondition<Annotation>) conditionClass.getDeclaredConstructor().newInstance();
                }
            } catch (ReflectiveOperationException e) {
                // Fall through.
            }
            return Optional.ofNullable(condition);
        }
    }

    class Helper {

        private Helper() {
            //hide
        }

        public static Optional<Annotation> getAnnotatedElement(AnnotatedElement annotatedElement) {
            return Arrays.stream(annotatedElement.getDeclaredAnnotations())
                    .filter(annotation -> Arrays.stream(annotation.annotationType().getDeclaredAnnotations()).anyMatch(ann -> ann.annotationType().isAssignableFrom(FindConditional.class)))
                    .findFirst();
        }
    }
}
