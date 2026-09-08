package com.ravenherz.cse.util.html.impl;

import com.ravenherz.cse.constants.HtmlNames;
import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.constants.Strings;
import com.ravenherz.cse.util.html.AccessToSettingsTag;
import com.ravenherz.cse.util.html.ControllerAccessibleTag;
import com.ravenherz.cse.util.html.HTMLElement;
import com.ravenherz.cse.util.html.HTMLElement.HTMLElementBuilder;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("settings")
public final class TagCompanyEmailAddress
        extends AccessToSettingsTag
        implements ControllerAccessibleTag {

    @Override
    public HTMLElement getHtmlElement(String value) {

        String key = SettingKeys.KEY_TAG_COMPANY_EMAIL;
        value = defaultValueIfNull(value, key);

        if (value != null && !value.contains(Strings.STR_PROTOCOL_MAILTO)) {
            value = Strings.STR_PROTOCOL_MAILTO + value;
        }

        return HTMLElementBuilder
                .get(HTMLElementBuilder
                                .get(HtmlNames.ELEM_NAME_B, Strings.STR_EMAIL)
                                .build().getCode()
                        + Strings.STR_DELIM_WS
                        + HTMLElementBuilder
                                .get(HtmlNames.ELEM_NAME_A, Strings.STR_CLICK)
                                .addAttribute(HtmlNames.ATTR_NAME_ID, key)
                                .addAttribute(HtmlNames.ATTR_NAME_HREF, value)
                                .build()
                                .getCode(),
                        key,
                        value)
                .addAttribute(HtmlNames.ATTR_NAME_NAME, HtmlNames.ATTR_VALUE_HIGHLIGHTABLE)
                .build();
    }
}
