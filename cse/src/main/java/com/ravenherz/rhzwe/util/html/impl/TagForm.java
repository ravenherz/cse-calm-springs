package com.ravenherz.rhzwe.util.html.impl;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.controller.objects.FormDescription.FormElementGroup.FormElementDescription.FormElementType;
import com.ravenherz.rhzwe.util.html.AccessToSettingsTag;
import com.ravenherz.rhzwe.util.html.HTMLElement;
import com.ravenherz.rhzwe.util.html.HTMLElement.HTMLElementBuilder;

import java.util.ArrayList;
import java.util.List;

import static com.ravenherz.rhzwe.constants.Strings.*;

public class TagForm
        extends AccessToSettingsTag {

    public HTMLElement renderTag(String formId) {
        List<String> contents = new ArrayList<>();
        settings.getFormDescriptions().forEach(form -> {
            if (form.getId().equals(formId)) {
                contents.add(HTMLElementBuilder.get(Strings.ELEM_NAME_INPUT, null)
                        .addAttribute(ATTR_NAME_ID, formId + "-rest")
                        .addAttribute(ATTR_NAME_STYLE, "display: none;")
                        .addAttribute(Strings.ATTR_NAME_VALUE, form.getButton().getRest())
                        .build()
                        .getCode());
                switch (form.getFormType()) {
                    case LIST:
                        form.getGroups().forEach(formElementGroup -> {
                            formElementGroup.getElements().forEach(element -> {
                                String required = element.isRequired() ? "*" : "";
                                HTMLElement input;
                                if (FormElementType.INPUT.equals(element.getElementType())) {
                                    input = HTMLElementBuilder
                                            .get(ELEM_NAME_INPUT, element.getDefaultValue())
                                            .addAttribute(ATTR_NAME_CLASS,
                                                    "transition " + formId+"-collectible" + (
                                                            element.getClasses() == null ? ""
                                                                    : String.join(" ",
                                                                            element.getClasses())))
                                            .addAttribute(ATTR_NAME_ID, element.getId())
                                            .addAttribute(ATTR_NAME_TYPE, element.getContentType())
                                            .build();
                                    if (element.getPlaceholder() != null) {
                                        input.addAttribute(Strings.ATTR_NAME_PLACEHOLDER,
                                                element.getPlaceholder());
                                    }

                                    if (element.isRequired()) {
                                        input.addAttribute(Strings.ATTR_NAME_REQUIRED,
                                                Strings.ATTR_NAME_REQUIRED);
                                    }
                                } else {
                                    input = new HTMLElement();
                                }

                                HTMLElement label = HTMLElementBuilder
                                        .get(ELEM_NAME_LABEL, element.getLabel() + required)
                                        .addAttribute(ATTR_NAME_FOR, element.getId()).build();
                                contents.add(label.getCode() + input.getCode());
                            });
                        });
                        break;
                    case TABLE:
                        break;
                    default:
                }
                HTMLElement button = HTMLElementBuilder
                        .get(Strings.ELEM_NAME_BUTTON, form.getButton().getText())
                        .addAttribute(ATTR_NAME_CLASS, formId + "-button transition")
                        .build();
                contents.add(button.getCode());
            }
        });
        return HTMLElementBuilder
                .get(Strings.ELEM_NAME_DIV, String.join("", contents))
                .addAttribute(ATTR_NAME_ID, formId)
                .build();
    }
}
