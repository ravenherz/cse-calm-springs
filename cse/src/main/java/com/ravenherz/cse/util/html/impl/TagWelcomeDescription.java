package com.ravenherz.cse.util.html.impl;

import com.ravenherz.cse.constants.HtmlNames;
import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.util.html.AccessToSettingsTag;
import com.ravenherz.cse.util.html.ControllerAccessibleTag;
import com.ravenherz.cse.util.html.HTMLElement;
import com.ravenherz.cse.util.html.HTMLElement.HTMLElementBuilder;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("settings")
public final class TagWelcomeDescription
        extends AccessToSettingsTag
        implements ControllerAccessibleTag {

    @Override
    public HTMLElement getHtmlElement(String value) {

        String key = SettingKeys.KEY_TAG_WELCOME_DESCRIPTION;
        value = defaultValueIfNull(value, key);

        return HTMLElementBuilder
                .get(HtmlNames.ELEM_NAME_H6, value)
                .addAttribute(HtmlNames.ATTR_NAME_ID, key)
                .surroundByContainer(key, value)
                .addAttribute(HtmlNames.ATTR_NAME_NAME, HtmlNames.ATTR_VALUE_HIGHLIGHTABLE)
                .build();
    }
}
