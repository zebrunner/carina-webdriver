package com.zebrunner.carina.webdriver.locator.converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FormatLocatorConverter implements LocatorConverter {
    private final List<Object> arguments;

    public FormatLocatorConverter(Object... objects) {
        arguments = Arrays.stream(objects)
                .collect(Collectors.toList());
    }

    //if we use @FindAny({ @FindBy(id = "%s"), @FindBy(id = "%s") }) annotation,
    //we want element.format("id1","id2") to create @FindAny({ @FindBy(id = "id1"), @FindBy(id = "id2") })
    //with simply String.format(by, arguments.toString()) we will get @FindAny({ @FindBy(id = "id1"), @FindBy(id = "id1") })
    @Override
    public String convert(String by) {
        String converted = by;
        int index = 0;
        while (index < arguments.size()){
            String tmp =  String.format(converted, arguments.get(index));
            if (tmp.equals(converted)){
                break;
            } else {
                converted = tmp;
                arguments.remove(index);
                --index;
            }
            ++index;
        }

        return converted;
    }
}
