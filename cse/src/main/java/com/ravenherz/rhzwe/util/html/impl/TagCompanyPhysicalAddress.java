package com.ravenherz.rhzwe.util.html.impl;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.util.html.AccessToSettingsTag;
import com.ravenherz.rhzwe.util.html.ControllerAccessibleTag;
import com.ravenherz.rhzwe.util.html.HTMLElement;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("settings")
public final class TagCompanyPhysicalAddress
        extends AccessToSettingsTag
        implements ControllerAccessibleTag {

    @Override
    public HTMLElement getHtmlElement(String value) {
        String key = Strings.KEY_TAG_COMPANY_ADDRESS;
        value = defaultValueIfNull(value, key);

        HTMLElement label = new HTMLElement(Strings.ELEM_NAME_LABEL, value);
        label.addAttribute(Strings.ATTR_NAME_ID, key);
        HTMLElement b = new HTMLElement(Strings.ELEM_NAME_B, Strings.STR_ADDRESS);
        HTMLElement container = HTMLElement
                .getContainer(b.getCode() + Strings.STR_DELIM_WS + label.getCode(), key, value);
        container.addAttribute(Strings.ATTR_NAME_NAME, Strings.ATTR_VALUE_HIGHLIGHTABLE);
        return container;
    }
}
