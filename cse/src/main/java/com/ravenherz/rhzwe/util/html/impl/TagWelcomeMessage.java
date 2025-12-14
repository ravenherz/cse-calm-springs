package com.ravenherz.rhzwe.util.html.impl;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.util.html.AccessToSettingsTag;
import com.ravenherz.rhzwe.util.html.ControllerAccessibleTag;
import com.ravenherz.rhzwe.util.html.HTMLElement;
import com.ravenherz.rhzwe.util.html.HTMLElement.HTMLElementBuilder;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("settings")
public final class TagWelcomeMessage
        extends AccessToSettingsTag
        implements ControllerAccessibleTag {

    @Override
    public HTMLElement getHtmlElement(String value) {

        String key = Strings.KEY_TAG_WELCOME_MESSAGE;
        value = defaultValueIfNull(value, key);

        return HTMLElementBuilder
                .get(Strings.ELEM_NAME_PLAIN, value)
                .addAttribute(Strings.ATTR_NAME_ID, key)
                .surroundByContainer(key, value, Strings.ELEM_NAME_H6)
                .addAttribute(Strings.ATTR_NAME_NAME, Strings.ATTR_VALUE_HIGHLIGHTABLE)
                .addAttribute(Strings.ATTR_NAME_STYLE, Strings.ATTR_VALUE_DISPLAY_BLOCK)
                .build();
    }
}
