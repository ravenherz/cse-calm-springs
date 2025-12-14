package com.ravenherz.rhzwe.util.html.impl;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.util.helpers.SocialNetworkHelper;
import com.ravenherz.rhzwe.util.html.AccessToSocialHelperTag;
import com.ravenherz.rhzwe.util.html.ControllerAccessibleTag;
import com.ravenherz.rhzwe.util.html.HTMLElement;
import com.ravenherz.rhzwe.util.html.HTMLElement.HTMLElementBuilder;
import org.springframework.stereotype.Component;

import java.util.LinkedList;

@Component
public final class TagCompanySocialLinks
        extends AccessToSocialHelperTag
        implements ControllerAccessibleTag {

    private String key = Strings.KEY_TAG_COMPANY_SOCIAL;

    public HTMLElement getContainer(String rawSource) {
        HTMLElement container = HTMLElement.getContainer(
                getContents(rawSource),
                key,
                Strings.STR_DUMMY);
        container.addAttribute(Strings.ATTR_NAME_NAME, Strings.ATTR_VALUE_HIGHLIGHTABLE);
        return container;
    }

    public String getContents(String rawSource) {
        LinkedList<String> elements = new LinkedList<>();
        for (SocialNetworkHelper.SocialNetworkData data : socialNetworkHelper
                .makeSocialNetworkDataFromString(rawSource)) {
            elements.add(getLinkCode(data));
        }
        return String.join(Strings.STR_EMPTY, elements);
    }

    private String getLinkCode(SocialNetworkHelper.SocialNetworkData data) {
        HTMLElement imgElement = HTMLElementBuilder
                .get(Strings.ELEM_NAME_IMG, null)
                .addAttribute(Strings.ATTR_NAME_SRC, data.getLogo())
                .addAttribute(Strings.ATTR_NAME_ALT, data.getName())
                .addAttribute(Strings.ATTR_NAME_TITLE, data.getTooltip())
                .addAttribute(Strings.ATTR_NAME_WIDTH, Strings.ATTR_VALUE_SOCIAL_LINK_SIZE)
                .addAttribute(Strings.ATTR_NAME_HEIGHT, Strings.ATTR_VALUE_SOCIAL_LINK_SIZE)
                .addAttribute(Strings.ATTR_NAME_BORDER, "0")
                .surroundByContainer(
                        String.format("%s-%s-%s", key, data.getName(),
                                String.valueOf(data.hashCode()).replace("-", "a")),
                        data.getName(),
                        Strings.ELEM_NAME_A)
                .addAttribute(Strings.ATTR_NAME_HREF, data.getUrl())
                .addAttribute(Strings.ATTR_NAME_CLASS, key)
                .addAttribute(Strings.ATTR_NAME_TARGET, Strings.ATTR_VALUE_BLANK)
                .build();
        return imgElement.getCode();
    }

    @Override
    public HTMLElement getHtmlElement(String value) {
        return getContainer(defaultValueIfNull(value, key));
    }
}
