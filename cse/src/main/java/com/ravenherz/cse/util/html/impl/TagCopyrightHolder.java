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

import java.util.Calendar;

@Component
@DependsOn("settings")
public final class TagCopyrightHolder
        extends AccessToSettingsTag
        implements ControllerAccessibleTag {

    @Override
    public HTMLElement getHtmlElement(String value) {
        String key = SettingKeys.KEY_TAG_COPYRIGHT_HOLDER;
        value = defaultValueIfNull(value, key);

        return HTMLElementBuilder
                .get(HtmlNames.ELEM_NAME_B,
                        HTMLElementBuilder
                                .get(HtmlNames.ELEM_NAME_B, value)
                                .addAttribute(HtmlNames.ATTR_NAME_ID, key)
                                .build().getCode() +
                                getCopyright())
                .surroundByContainer(key, value)
                .addAttribute(HtmlNames.ATTR_NAME_NAME, HtmlNames.ATTR_VALUE_HIGHLIGHTABLE).build();
    }

    private String getCopyright() {
        String copyright = Strings.STR_DELIM_CPRIGHTWWS;
        String key = SettingKeys.KEY_TAG_COPYRIGHT_SINCE;
        String value = settings.getValue(context, key);
        String yearnow = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        if (value == null) {
            settings.putValue(context, key, yearnow);
            copyright = copyright + yearnow;
        } else {
            if (value.equals(yearnow)) {
                copyright = copyright + value;
            } else {
                copyright = copyright + value + Strings.STR_DELIM_HYPHEN + yearnow;
            }
        }
        return copyright;
    }
}
