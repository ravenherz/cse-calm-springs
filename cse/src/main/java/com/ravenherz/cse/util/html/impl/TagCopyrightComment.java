package com.ravenherz.cse.util.html.impl;

import com.ravenherz.cse.constants.HtmlNames;
import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.util.html.AccessToSettingsTag;
import com.ravenherz.cse.util.html.ControllerAccessibleTag;
import com.ravenherz.cse.util.html.HTMLElement;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("settings")
public final class TagCopyrightComment
        extends AccessToSettingsTag
        implements ControllerAccessibleTag {

    @Override
    public HTMLElement getHtmlElement(String value) {
        String key = SettingKeys.KEY_TAG_COPYRIGHT_COMMENT;
        value = defaultValueIfNull(value, key);

        HTMLElement labelElement = new HTMLElement(HtmlNames.ELEM_NAME_LABEL, value);
        labelElement.addAttribute(HtmlNames.ATTR_NAME_ID, key);
        HTMLElement container = labelElement.surroundByContainer(key, value);
        container.addAttribute(HtmlNames.ATTR_NAME_NAME, HtmlNames.ATTR_VALUE_HIGHLIGHTABLE);
        return container;
    }
}
