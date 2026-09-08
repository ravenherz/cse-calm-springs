package com.ravenherz.cse.controller.publicsite;

import com.ravenherz.cse.util.html.CustomHtmlTag;
import org.springframework.ui.Model;

import java.util.List;

/**
 * Puts rendered {@code Tag*} HTML on the public model (footer contact copy).
 */
public final class PublicSiteTags {

    private PublicSiteTags() {
    }

    public static void addTo(Model model, List<? extends CustomHtmlTag> tags) {
        if (model == null || tags == null) {
            return;
        }
        for (CustomHtmlTag tag : tags) {
            if (tag == null || tag.getHtmlElement(null) == null) {
                continue;
            }
            model.addAttribute(tag.getClass().getSimpleName(), tag.getHtmlElement(null).getCode());
        }
    }
}
