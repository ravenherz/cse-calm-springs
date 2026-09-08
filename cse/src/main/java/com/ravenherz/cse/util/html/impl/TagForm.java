package com.ravenherz.cse.util.html.impl;

import com.ravenherz.cse.constants.HtmlNames;
import com.ravenherz.cse.controller.objects.FormDescription;
import com.ravenherz.cse.util.html.AccessToSettingsTag;
import com.ravenherz.cse.util.html.HTMLElement;
import com.ravenherz.cse.util.html.HTMLElement.HTMLElementBuilder;

import java.util.ArrayList;
import java.util.List;

public class TagForm
        extends AccessToSettingsTag {

    public HTMLElement renderTag(String formId) {
        List<String> contents = new ArrayList<>();
        settings.getFormDescriptions().forEach(form -> {
            if (form.getId().equals(formId)) {
                contents.add(HTMLElementBuilder.get(HtmlNames.ELEM_NAME_INPUT, null)
                        .addAttribute(HtmlNames.ATTR_NAME_ID, formId + "-rest")
                        .addAttribute(HtmlNames.ATTR_NAME_STYLE, "display: none;")
                        .addAttribute(HtmlNames.ATTR_NAME_VALUE, form.getButton().getRest())
                        .build()
                        .getCode());
                switch (form.getFormType()) {
                    case LIST:
                        form.getGroups().forEach(formElementGroup -> {
                            formElementGroup.getElements().forEach(element -> {
                                String required = element.isRequired() ? "*" : "";
                                HTMLElement input;
                                if (FormDescription.FormElementGroup.FormElementDescription.FormElementType.INPUT.equals(element.getElementType())) {
                                    input = HTMLElementBuilder
                                            .get(HtmlNames.ELEM_NAME_INPUT, element.getDefaultValue())
                                            .addAttribute(HtmlNames.ATTR_NAME_CLASS,
                                                    "transition " + formId+"-collectible" + (
                                                            element.getClasses() == null ? ""
                                                                    : String.join(" ",
                                                                            element.getClasses())))
                                            .addAttribute(HtmlNames.ATTR_NAME_ID, element.getId())
                                            .addAttribute(HtmlNames.ATTR_NAME_TYPE, element.getContentType())
                                            .build();
                                    if (element.getPlaceholder() != null) {
                                        input.addAttribute(HtmlNames.ATTR_NAME_PLACEHOLDER,
                                                element.getPlaceholder());
                                    }

                                    if (element.isRequired()) {
                                        input.addAttribute(HtmlNames.ATTR_NAME_REQUIRED,
                                                HtmlNames.ATTR_NAME_REQUIRED);
                                    }
                                } else {
                                    input = new HTMLElement();
                                }

                                HTMLElement label = HTMLElementBuilder
                                        .get(HtmlNames.ELEM_NAME_LABEL, element.getLabel() + required)
                                        .addAttribute(HtmlNames.ATTR_NAME_FOR, element.getId()).build();
                                contents.add(label.getCode() + input.getCode());
                            });
                        });
                        break;
                    case TABLE:
                        break;
                    default:
                }
                HTMLElement button = HTMLElementBuilder
                        .get(HtmlNames.ELEM_NAME_BUTTON, form.getButton().getText())
                        .addAttribute(HtmlNames.ATTR_NAME_CLASS, formId + "-button transition")
                        .build();
                contents.add(button.getCode());
            }
        });
        return HTMLElementBuilder
                .get(HtmlNames.ELEM_NAME_DIV, String.join("", contents))
                .addAttribute(HtmlNames.ATTR_NAME_ID, formId)
                .build();
    }
}
