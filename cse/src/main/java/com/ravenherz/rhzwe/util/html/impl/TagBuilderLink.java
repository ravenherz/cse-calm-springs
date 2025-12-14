package com.ravenherz.rhzwe.util.html.impl;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.util.html.AccessToSettingsTag;
import com.ravenherz.rhzwe.util.html.CustomHtmlTag;
import com.ravenherz.rhzwe.util.html.HTMLElement;
import com.ravenherz.rhzwe.util.html.HTMLElement.HTMLElementBuilder;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import static com.ravenherz.rhzwe.constants.Strings.ATTR_NAME_NAME;
import static com.ravenherz.rhzwe.constants.Strings.STR_DELIM_WS;

@Component
@DependsOn("settings")
public final class TagBuilderLink
        extends AccessToSettingsTag
        implements CustomHtmlTag {

    @Override
    public HTMLElement getHtmlElement(String value) {

        String keyTitle = Strings.KEY_TAG_BUILDER_LINK_TITLE;
        String keyRefer = Strings.KEY_TAG_BUILDER_LINK_REFER;

        String title = settings.getValue(context, keyTitle);
        String refer = settings.getValue(context, keyRefer);
        if (refer != null && !refer.contains(Strings.STR_PROTOCOL_HTTPS)) {
            refer = Strings.STR_PROTOCOL_HTTPS + refer;
        }

        return HTMLElementBuilder
                .get(
                        Strings.ELEM_NAME_B,
                Strings.STR_BUILT_BY + STR_DELIM_WS +
                        HTMLElementBuilder
                                .get(Strings.ELEM_NAME_A, title)
                                .addAttribute(Strings.ATTR_NAME_ID, keyTitle)
                                .addAttribute(Strings.ATTR_NAME_HREF, refer)
                                .addAttribute(Strings.ATTR_NAME_TARGET,
                                        Strings.ATTR_VALUE_BLANK)
                                .build()
                                .getCode()
                )
                .surroundByContainer(keyTitle, title)
                .addAttribute(ATTR_NAME_NAME, Strings.ATTR_VALUE_HIGHLIGHTABLE)
                .build();
    }
}
