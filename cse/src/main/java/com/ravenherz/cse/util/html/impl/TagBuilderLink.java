package com.ravenherz.cse.util.html.impl;

import com.ravenherz.cse.constants.HtmlNames;
import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.constants.Strings;
import com.ravenherz.cse.util.html.AccessToSettingsTag;
import com.ravenherz.cse.util.html.CustomHtmlTag;
import com.ravenherz.cse.util.html.HTMLElement;
import com.ravenherz.cse.util.html.HTMLElement.HTMLElementBuilder;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("settings")
public final class TagBuilderLink
        extends AccessToSettingsTag
        implements CustomHtmlTag {

    @Override
    public HTMLElement getHtmlElement(String value) {

        String keyTitle = SettingKeys.KEY_TAG_BUILDER_LINK_TITLE;
        String keyRefer = SettingKeys.KEY_TAG_BUILDER_LINK_REFER;

        String title = settings.getValue(context, keyTitle);
        String refer = settings.getValue(context, keyRefer);
        if (refer != null && !refer.contains(Strings.STR_PROTOCOL_HTTPS)) {
            refer = Strings.STR_PROTOCOL_HTTPS + refer;
        }

        return HTMLElementBuilder
                .get(
                        HtmlNames.ELEM_NAME_B,
                Strings.STR_BUILT_BY + Strings.STR_DELIM_WS +
                        HTMLElementBuilder
                                .get(HtmlNames.ELEM_NAME_A, title)
                                .addAttribute(HtmlNames.ATTR_NAME_ID, keyTitle)
                                .addAttribute(HtmlNames.ATTR_NAME_HREF, refer)
                                .addAttribute(HtmlNames.ATTR_NAME_TARGET,
                                        HtmlNames.ATTR_VALUE_BLANK)
                                .build()
                                .getCode()
                )
                .surroundByContainer(keyTitle, title)
                .addAttribute(HtmlNames.ATTR_NAME_NAME, HtmlNames.ATTR_VALUE_HIGHLIGHTABLE)
                .build();
    }
}
