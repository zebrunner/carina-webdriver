package com.zebrunner.carina.webdriver.locator;

import org.openqa.selenium.By;
import org.openqa.selenium.support.FindAll;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.pagefactory.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class ExtendedSeleniumAnnotations extends Annotations {

    private final LocatorCreatorContext locatorCreatorContext;

    /**
     * @param field expected to be an element in a Page Object
     */
    public ExtendedSeleniumAnnotations(Field field, LocatorCreatorContext locatorCreatorContext) {
        super(field);
        this.locatorCreatorContext = locatorCreatorContext;
    }

    @Override
    public By buildBy() {
        assertValidAnnotations();

        Field field = getField();
        return FindConditional.Builder
                .buildIt(field, locatorCreatorContext)
                .orElseGet(super::buildBy);
    }

    @Override
    protected void assertValidAnnotations() {
        FindBy findBy = getField().getAnnotation(FindBy.class);
        FindBys findBys = getField().getAnnotation(FindBys.class);
        FindAll findAll = getField().getAnnotation(FindAll.class);
        FindAny findAny = getField().getAnnotation(FindAny.class);

        checkAloneAnnotation(findBy, findBys, findAll, findAny);
    }

    private static void checkAloneAnnotation(Annotation... annotations) {
        for (int i = 0; i < annotations.length; i++) {
            Annotation a1 = annotations[i];

            for (int j = i + 1; j < annotations.length; j++) {
                Annotation a2 = annotations[j];
                checkDisallowedAnnotationPairs(a1, a2);
            }
        }
    }

    private static void checkDisallowedAnnotationPairs(Annotation a1, Annotation a2) {
        if (a1 != null && a2 != null) {
            throw new IllegalArgumentException(
                    "If you use a '@" + a1.annotationType().getSimpleName() + "' annotation, "
                            + "you must not also use a '@" + a2.annotationType().getSimpleName()
                            + "' annotation");
        }
    }
}
