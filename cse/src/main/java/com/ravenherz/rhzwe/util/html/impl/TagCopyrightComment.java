package com.ravenherz.rhzwe.util.html.impl;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.util.html.AccessToSettingsTag;
import com.ravenherz.rhzwe.util.html.ControllerAccessibleTag;
import com.ravenherz.rhzwe.util.html.HTMLElement;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("settings")
public final class TagCopyrightComment
        extends AccessToSettingsTag
        implements ControllerAccessibleTag {

    @Override
    public HTMLElement getHtmlElement(String value) {
        String key = Strings.KEY_TAG_COPYRIGHT_COMMENT;
        value = defaultValueIfNull(value, key);

        HTMLElement labelElement = new HTMLElement(Strings.ELEM_NAME_LABEL, value);
        labelElement.addAttribute(Strings.ATTR_NAME_ID, key);
        HTMLElement container = labelElement.surroundByContainer(key, value);
        container.addAttribute(Strings.ATTR_NAME_NAME, Strings.ATTR_VALUE_HIGHLIGHTABLE);
        return container;
    }
}
