package com.ravenherz.cse.util.html.impl;

import com.ravenherz.cse.constants.HtmlNames;
import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.constants.Strings;
import com.ravenherz.cse.util.helpers.SocialNetworkHelper;
import com.ravenherz.cse.util.html.AccessToSocialHelperTag;
import com.ravenherz.cse.util.html.ControllerAccessibleTag;
import com.ravenherz.cse.util.html.HTMLElement;
import com.ravenherz.cse.util.html.HTMLElement.HTMLElementBuilder;
import org.springframework.stereotype.Component;

import java.util.LinkedList;

@Component
public final class TagCompanySocialLinks
        extends AccessToSocialHelperTag
        implements ControllerAccessibleTag {

    private String key = SettingKeys.KEY_TAG_COMPANY_SOCIAL;

    public HTMLElement getContainer(String rawSource) {
        HTMLElement container = HTMLElement.getContainer(
                getContents(rawSource),
                key,
                Strings.STR_DUMMY);
        container.addAttribute(HtmlNames.ATTR_NAME_NAME, HtmlNames.ATTR_VALUE_HIGHLIGHTABLE);
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
                .get(HtmlNames.ELEM_NAME_IMG, null)
                .addAttribute(HtmlNames.ATTR_NAME_SRC, data.getLogo())
                .addAttribute(HtmlNames.ATTR_NAME_ALT, data.getName())
                .addAttribute(HtmlNames.ATTR_NAME_TITLE, data.getTooltip())
                .addAttribute(HtmlNames.ATTR_NAME_WIDTH, HtmlNames.ATTR_VALUE_SOCIAL_LINK_SIZE)
                .addAttribute(HtmlNames.ATTR_NAME_HEIGHT, HtmlNames.ATTR_VALUE_SOCIAL_LINK_SIZE)
                .addAttribute(HtmlNames.ATTR_NAME_BORDER, "0")
                .surroundByContainer(
                        String.format("%s-%s-%s", key, data.getName(),
                                String.valueOf(data.hashCode()).replace("-", "a")),
                        data.getName(),
                        HtmlNames.ELEM_NAME_A)
                .addAttribute(HtmlNames.ATTR_NAME_HREF, data.getUrl())
                .addAttribute(HtmlNames.ATTR_NAME_CLASS, key)
                .addAttribute(HtmlNames.ATTR_NAME_TARGET, HtmlNames.ATTR_VALUE_BLANK)
                .build();
        return imgElement.getCode();
    }

    @Override
    public HTMLElement getHtmlElement(String value) {
        return getContainer(defaultValueIfNull(value, key));
    }
}
