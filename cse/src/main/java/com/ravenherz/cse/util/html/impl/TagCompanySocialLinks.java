package com.ravenherz.cse.util.html.impl;

import com.ravenherz.cse.constants.HtmlNames;
import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.constants.Strings;
import com.ravenherz.cse.dal.dao.UrlTemplateService;
import com.ravenherz.cse.util.CompanySocialMarkup;
import com.ravenherz.cse.util.UrlEmbedProcessor;
import com.ravenherz.cse.util.html.AccessToSettingsTag;
import com.ravenherz.cse.util.html.ControllerAccessibleTag;
import com.ravenherz.cse.util.html.HTMLElement;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("settings")
public final class TagCompanySocialLinks
        extends AccessToSettingsTag
        implements ControllerAccessibleTag {

    private final UrlEmbedProcessor urlEmbedProcessor;
    private final ObjectProvider<UrlTemplateService> urlTemplateService;

    public TagCompanySocialLinks(UrlEmbedProcessor urlEmbedProcessor,
            ObjectProvider<UrlTemplateService> urlTemplateService) {
        this.urlEmbedProcessor = urlEmbedProcessor;
        this.urlTemplateService = urlTemplateService;
    }

    public HTMLElement getContainer(String rawSource) {
        HTMLElement container = HTMLElement.getContainer(
                getContents(rawSource),
                SettingKeys.KEY_TAG_COMPANY_SOCIAL,
                Strings.STR_DUMMY);
        container.addAttribute(HtmlNames.ATTR_NAME_NAME, HtmlNames.ATTR_VALUE_HIGHLIGHTABLE);
        container.addAttribute(HtmlNames.ATTR_NAME_CLASS,
                HtmlNames.CSS_CLASS_ALL_WIDE_DIV + Strings.STR_DELIM_WS + SettingKeys.KEY_TAG_COMPANY_SOCIAL);
        return container;
    }

    public String getContents(String rawSource) {
        String markup = CompanySocialMarkup.toCseUrls(rawSource, this::knownTemplate);
        if (markup.isEmpty() || urlEmbedProcessor == null) {
            return markup;
        }
        return urlEmbedProcessor.expandHtml(markup);
    }

    @Override
    public HTMLElement getHtmlElement(String value) {
        return getContainer(defaultValueIfNull(value, SettingKeys.KEY_TAG_COMPANY_SOCIAL));
    }

    private boolean knownTemplate(String templateId) {
        try {
            UrlTemplateService service = urlTemplateService == null
                    ? null : urlTemplateService.getIfAvailable();
            if (service == null) {
                return true;
            }
            return service.getByUrlTemplateId(templateId) != null;
        } catch (RuntimeException ex) {
            return true;
        }
    }
}
