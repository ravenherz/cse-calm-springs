package com.ravenherz.cse.util.html.impl;

import com.ravenherz.cse.constants.HtmlNames;
import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.constants.Strings;
import com.ravenherz.cse.util.html.AccessToSettingsTag;
import com.ravenherz.cse.util.html.ControllerAccessibleTag;
import com.ravenherz.cse.util.html.HTMLElement;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("settings")
public final class TagCompanyPhysicalAddress
        extends AccessToSettingsTag
        implements ControllerAccessibleTag {

    @Override
    public HTMLElement getHtmlElement(String value) {
        String key = SettingKeys.KEY_TAG_COMPANY_ADDRESS;
        value = defaultValueIfNull(value, key);

        HTMLElement label = new HTMLElement(HtmlNames.ELEM_NAME_LABEL, value);
        label.addAttribute(HtmlNames.ATTR_NAME_ID, key);
        HTMLElement b = new HTMLElement(HtmlNames.ELEM_NAME_B, Strings.STR_ADDRESS);
        HTMLElement container = HTMLElement
                .getContainer(b.getCode() + Strings.STR_DELIM_WS + label.getCode(), key, value);
        container.addAttribute(HtmlNames.ATTR_NAME_NAME, HtmlNames.ATTR_VALUE_HIGHLIGHTABLE);
        return container;
    }
}
