package com.ravenherz.rhzwe.util.html.impl;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.util.html.AccessToSettingsTag;
import com.ravenherz.rhzwe.util.html.CustomHtmlTag;
import com.ravenherz.rhzwe.util.html.HTMLElement;
import com.ravenherz.rhzwe.util.html.HTMLElement.HTMLElementBuilder;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("settings")
public final class TagSoftVersion
        extends AccessToSettingsTag
        implements CustomHtmlTag {

    private String getProductInfo(String product, String branch, String version) {
        String productinfo = null;
        if (product != null && product.trim().length() > 0) {
            productinfo = Strings.STR_EMPTY + product;

            if (version != null && version.trim().length() > 0) {
                productinfo += "<br>" + version;
            }
            if (branch != null && branch.trim().length() > 0) {
                productinfo += Strings.STR_DELIM_DOT + branch;
            }
        }
        return productinfo;
    }

    private String getLinkCode() {
        return HTMLElementBuilder.get(Strings.ELEM_NAME_IMG, null)
                .addAttribute(Strings.ATTR_NAME_SRC,
                        "data:image/png;base64,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")
                .addAttribute(Strings.ATTR_NAME_ALT, "logo")
                .addAttribute(Strings.ATTR_NAME_WIDTH, Strings.ATTR_VALUE_SOCIAL_LINK_SIZE)
                .addAttribute(Strings.ATTR_NAME_HEIGHT, Strings.ATTR_VALUE_SOCIAL_LINK_SIZE)
                .addAttribute(Strings.ATTR_NAME_BORDER, "0")
                .surroundByContainer(Strings.STR_DUMMY, Strings.STR_DUMMY, Strings.ELEM_NAME_A)
                .addAttribute(Strings.ATTR_NAME_HREF, "https://ravenherz.com/rhz-we/?page=rhz-we")
                .addAttribute(Strings.ATTR_NAME_CLASS, Strings.KEY_TAG_COMPANY_SOCIAL)
                .addAttribute(Strings.ATTR_NAME_TARGET, Strings.ATTR_VALUE_BLANK)
                .build()
                .getCode();
    }

    @Override
    public HTMLElement getHtmlElement(String value) {
        String product = settings.getValue(Strings.CONTEXT_DATASOURCE_BUILD_INFO,
                Strings.KEY_TAG_SOFT_VERSION_PRODUCT);
        String branch = settings.getValue(Strings.CONTEXT_DATASOURCE_BUILD_INFO,
                Strings.KEY_TAG_SOFT_VERSION_BRANCH);
        String version = settings.getValue(Strings.CONTEXT_DATASOURCE_BUILD_INFO,
                Strings.KEY_TAG_SOFT_VERSION_VERSION);

        //String spring = Strings.STR_SPRING + Strings.STR_DELIM_WS + SpringVersion.getVersion();
        String spring = null;
        String productInfo = getProductInfo(product, branch, version);

        String outStr = Strings.STR_EMPTY;
        if (productInfo != null) {
            outStr += productInfo;
        }

        HTMLElement text = new HTMLElement(Strings.ELEM_NAME_DIV, outStr);
        text.addAttribute(Strings.ATTR_NAME_CLASS, "soft-version");
        HTMLElement imgDiv = new HTMLElement(Strings.ELEM_NAME_DIV, getLinkCode());
        imgDiv.addAttribute(Strings.ATTR_NAME_STYLE, "display:inline-block; padding-left:5px");
        HTMLElement div = new HTMLElement(Strings.ELEM_NAME_DIV, text.getCode() + imgDiv.getCode());
        HTMLElement container = div.surroundByContainer(Strings.STR_DUMMY, Strings.STR_DUMMY);
        container.addAttribute(Strings.ATTR_NAME_CLASS, Strings.CSS_CLASS_DIV_MARGIN_RIGHT_5PX);
        return container;
    }
}
