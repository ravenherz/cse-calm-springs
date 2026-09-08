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
public final class TagWelcomeMessage
        extends AccessToSettingsTag
        implements ControllerAccessibleTag {

    @Override
    public HTMLElement getHtmlElement(String value) {

        String key = SettingKeys.KEY_TAG_WELCOME_MESSAGE;
        value = defaultValueIfNull(value, key);

        return HTMLElementBuilder
                .get(HtmlNames.ELEM_NAME_PLAIN, value)
                .addAttribute(HtmlNames.ATTR_NAME_ID, key)
                .surroundByContainer(key, value, HtmlNames.ELEM_NAME_H6)
                .addAttribute(HtmlNames.ATTR_NAME_NAME, HtmlNames.ATTR_VALUE_HIGHLIGHTABLE)
                .addAttribute(HtmlNames.ATTR_NAME_STYLE, HtmlNames.ATTR_VALUE_DISPLAY_BLOCK)
                .build();
    }
}
