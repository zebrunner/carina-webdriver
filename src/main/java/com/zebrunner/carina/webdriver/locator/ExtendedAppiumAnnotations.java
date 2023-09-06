package com.zebrunner.carina.webdriver.locator;

import org.openqa.selenium.By;
import org.openqa.selenium.support.FindAll;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;

import io.appium.java_client.pagefactory.DefaultElementByBuilder;

public class ExtendedAppiumAnnotations extends DefaultElementByBuilder {

    private final LocatorCreatorContext locatorCreatorContext;

    public ExtendedAppiumAnnotations(LocatorCreatorContext locatorCreatorContext) {
        super(locatorCreatorContext.getPlatform(), locatorCreatorContext.getAutomation());
        this.locatorCreatorContext = locatorCreatorContext;
    }

    @Override
    public By buildBy() {
        assertValidAnnotations();

        AnnotatedElement annotatedElement = annotatedElementContainer.getAnnotated();
        return FindConditional.Builder
                .buildIt((Field) annotatedElement, locatorCreatorContext)
                .orElseGet(super::buildBy);
    }

    @Override
    protected By buildDefaultBy() {
        AnnotatedElement annotatedElement = annotatedElementContainer.getAnnotated();
        By defaultBy = null;
        FindBy findBy = annotatedElement.getAnnotation(FindBy.class);
        if (findBy != null) {
            defaultBy = new FindBy.FindByBuilder().buildIt(findBy, (Field) annotatedElement);
        }

        if (defaultBy == null) {
            FindBys findBys = annotatedElement.getAnnotation(FindBys.class);
            if (findBys != null) {
                defaultBy = new FindBys.FindByBuilder().buildIt(findBys, (Field) annotatedElement);
            }
        }

        if (defaultBy == null) {
            FindAll findAll = annotatedElement.getAnnotation(FindAll.class);
            if (findAll != null) {
                defaultBy = new FindAll.FindByBuilder().buildIt(findAll, (Field) annotatedElement);
            }
        }

        if (defaultBy == null) {
            FindAny findAny = annotatedElement.getAnnotation(FindAny.class);
            if (findAny != null) {
                defaultBy = new FindAny.FindAnyBuilder().buildIt(findAny, (Field) annotatedElement);
            }
        }

        return defaultBy;
    }

    @Override
    protected void assertValidAnnotations() {
        AnnotatedElement annotatedElement = annotatedElementContainer.getAnnotated();
        FindBy findBy = annotatedElement.getAnnotation(FindBy.class);
        FindBys findBys = annotatedElement.getAnnotation(FindBys.class);
        checkDisallowedAnnotationPairs(findBy, findBys);
        FindAll findAll = annotatedElement.getAnnotation(FindAll.class);
        checkDisallowedAnnotationPairs(findBy, findAll);
        checkDisallowedAnnotationPairs(findBys, findAll);
        FindAny findAny = annotatedElement.getAnnotation(FindAny.class);
        checkDisallowedAnnotationPairs(findBy, findAny);
        checkDisallowedAnnotationPairs(findBys, findAny);
        checkDisallowedAnnotationPairs(findAll, findAny);
    }

    private static void checkDisallowedAnnotationPairs(Annotation a1, Annotation a2) {
        if (a1 != null && a2 != null) {
            throw new IllegalArgumentException(
                    "If you use a '@" + a1.getClass().getSimpleName() + "' annotation, "
                            + "you must not also use a '@" + a2.getClass().getSimpleName()
                            + "' annotation");
        }
    }
}
