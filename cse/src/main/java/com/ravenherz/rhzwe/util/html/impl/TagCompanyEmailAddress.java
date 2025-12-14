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
public final class TagCompanyEmailAddress
        extends AccessToSettingsTag
        implements ControllerAccessibleTag {

    @Override
    public HTMLElement getHtmlElement(String value) {

        String key = Strings.KEY_TAG_COMPANY_EMAIL;
        value = defaultValueIfNull(value, key);

        if (value != null && !value.contains(Strings.STR_PROTOCOL_MAILTO)) {
            value = Strings.STR_PROTOCOL_MAILTO + value;
        }

        return HTMLElementBuilder
                .get(HTMLElementBuilder
                                .get(Strings.ELEM_NAME_B, Strings.STR_EMAIL)
                                .build().getCode()
                        + Strings.STR_DELIM_WS
                        + HTMLElementBuilder
                                .get(Strings.ELEM_NAME_A, Strings.STR_CLICK)
                                .addAttribute(Strings.ATTR_NAME_ID, key)
                                .addAttribute(Strings.ATTR_NAME_HREF, value)
                                .build()
                                .getCode(),
                        key,
                        value)
                .addAttribute(Strings.ATTR_NAME_NAME, Strings.ATTR_VALUE_HIGHLIGHTABLE)
                .build();
    }
}
