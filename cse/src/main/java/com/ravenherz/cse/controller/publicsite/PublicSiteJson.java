package com.ravenherz.cse.controller.publicsite;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.events.PageEvent;
import com.ravenherz.cse.dal.dto.events.PageEventComment;
import com.ravenherz.cse.install.SiteReady;
import com.ravenherz.cse.content.AlbumImageDTO;
import com.ravenherz.cse.present.CategorySectionDTO;
import com.ravenherz.cse.engine.util.Settings;
import com.ravenherz.cse.util.helpers.HttpErrorHelper;
import com.ravenherz.cse.util.helpers.HttpErrorHelper.HttpErrorDescription;
import com.ravenherz.cse.util.themes.ThemeCatalog;
import com.ravenherz.cse.util.themes.ThemeSelection;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON shape for {@code GET /rest/site}. Does not dump PageEvent/PageData graphs
 * (those pull Mongo entities through getters).
 */
public final class PublicSiteJson {

    private PublicSiteJson() {
    }

    public static Map<String, Object> from(Model model, HttpServletRequest request, Settings settings,
            SiteReady siteReady, ThemeCatalog themeCatalog, AccountEntity accessor, String errorParam,
            HttpErrorHelper errors, int loadError) {
        Map<String, Object> body = new LinkedHashMap<>();
        boolean configured = siteReady != null && siteReady.isConfigured();
        String context = request.getContextPath();
        body.put("configured", configured);
        body.put("contextPath", context == null || context.isBlank() ? "/" : context);
        ThemeSelection theme = themeCatalog == null ? ThemeSelection.fallback() : themeCatalog.resolve(accessor);
        body.put("stylesTheme", theme.getCssId());
        body.put("stylesSchema", theme.getSchemaId());
        body.put("stylesShell", theme.getShellId());
        String siteName = settings == null ? "" : nullToEmpty(settings.getValue(
                SettingKeys.CONTEXT_DATASOURCE_PERSONAL, SettingKeys.KEY_TAG_COMPANY_TITLE));
        body.put("siteName", siteName);
        body.put("authenticated", accessor != null);
        body.put("username", accessor == null || accessor.getAccountData() == null
                ? null : accessor.getAccountData().getLogin());
        body.put("copy", copy(settings));
        body.put("foot", foot(model));

        int errorCode = loadError;
        if (errorCode == 0 && errorParam != null && !errorParam.isBlank()) {
            try {
                errorCode = Integer.parseInt(errorParam.trim());
            } catch (NumberFormatException ex) {
                errorCode = 500;
            }
        }

        String view = viewName(model, errorCode);
        if (!configured && errorCode == 0) {
            view = "setup";
        }
        body.put("view", view);
        body.put("htmlTitle", firstNonBlank(stringAttr(model, "htmlTitle"), siteName));
        body.put("selectedTag", stringAttr(model, "selectedTag"));
        body.put("nav", nav(model));
        body.put("sections", sections(model));
        body.put("introPage", item((PageEvent) model.getAttribute("introPage")));
        body.put("page", currentPage(model));
        if (errorCode != 0 && errors != null) {
            HttpErrorDescription desc = errors.getHttpErrorDescByCode(errorCode);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", desc.getCode());
            error.put("name", desc.getName());
            error.put("description", desc.getDescription());
            body.put("error", error);
        } else {
            body.put("error", null);
        }
        return body;
    }

    static Map<String, Object> item(PageEvent event) {
        if (event == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", event.getId());
        map.put("uri", event.getUniqueUriName());
        map.put("title", event.getTitle());
        map.put("header", event.getHeader());
        map.put("subHeader", event.getSubHeader());
        map.put("html", event.getDescription());
        map.put("category", event.getCategoryItemName());
        map.put("image", event.getImageLinkFull());
        map.put("href", event.getPageLink());
        map.put("album", event.isAlbum());
        map.put("noTopDisplayImage", event.isNoTopDisplayImage());
        map.put("exportPdf", event.isExportPdf());
        map.put("tags", event.getTags() == null ? List.of() : event.getTags());
        map.put("created", event.getCreated());
        map.put("comments", comments(event.getPageComments()));
        map.put("albumImages", albumImages(event.getAlbumImages()));
        return map;
    }

    private static String viewName(Model model, int errorCode) {
        if (errorCode != 0) {
            return "error";
        }
        if (booleanAttr(model, "readingAlbum")) {
            return "album";
        }
        if (booleanAttr(model, "readingPage")) {
            return "page";
        }
        if (booleanAttr(model, "tagView")) {
            return "tag";
        }
        if (booleanAttr(model, "categoryView")) {
            return "category";
        }
        if (booleanAttr(model, "portfolioHome")) {
            return "home";
        }
        return "home";
    }

    private static Map<String, String> foot(Model model) {
        Map<String, String> foot = new LinkedHashMap<>();
        foot.put("org", join(model, "TagCompanyName", "TagCompanyPhysicalAddress",
                "TagCompanyPhone", "TagCompanyEmailAddress", "TagCompanySocialLinks"));
        foot.put("copy", join(model, "TagCopyrightHolder", "TagCopyrightComment"));
        foot.put("studio", join(model, "TagBuilderLink", "TagSoftVersion"));
        return foot;
    }

    private static String join(Model model, String... names) {
        StringBuilder out = new StringBuilder();
        if (model == null || names == null) {
            return "";
        }
        for (String name : names) {
            Object value = model.getAttribute(name);
            if (value != null) {
                out.append(value);
            }
        }
        return out.toString();
    }

    private static Map<String, String> copy(Settings settings) {
        Map<String, String> copy = new LinkedHashMap<>();
        if (settings == null) {
            return copy;
        }
        for (String key : SettingKeys.INSTALL_PERSONAL_KEYS) {
            copy.put(key, nullToEmpty(settings.getValue(SettingKeys.CONTEXT_DATASOURCE_PERSONAL, key)));
        }
        copy.put(SettingKeys.KEY_TAG_SOFT_VERSION_PRODUCT, nullToEmpty(
                settings.getValue(SettingKeys.CONTEXT_DATASOURCE_BUILD_INFO,
                        SettingKeys.KEY_TAG_SOFT_VERSION_PRODUCT)));
        copy.put(SettingKeys.KEY_TAG_SOFT_VERSION_VERSION, nullToEmpty(
                settings.getValue(SettingKeys.CONTEXT_DATASOURCE_BUILD_INFO,
                        SettingKeys.KEY_TAG_SOFT_VERSION_VERSION)));
        copy.put(SettingKeys.KEY_TAG_SOFT_VERSION_BRANCH, nullToEmpty(
                settings.getValue(SettingKeys.CONTEXT_DATASOURCE_BUILD_INFO,
                        SettingKeys.KEY_TAG_SOFT_VERSION_BRANCH)));
        return copy;
    }

    private static List<Map<String, Object>> nav(Model model) {
        Object raw = model.getAttribute("navCategories");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof CategorySectionDTO section) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("slug", section.getSlug());
                map.put("title", section.getTitle());
                map.put("itemName", section.getItemName());
                out.add(map);
            }
        }
        return out;
    }

    private static List<Map<String, Object>> sections(Model model) {
        Object raw = model.getAttribute("sections");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof CategorySectionDTO section) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("slug", section.getSlug());
                map.put("itemName", section.getItemName());
                map.put("title", section.getTitle());
                map.put("description", section.getDescription());
                map.put("kind", section.getKind());
                map.put("featured", section.isFeatured());
                map.put("hasMore", section.isHasMore());
                map.put("totalCount", section.getTotalCount());
                List<Map<String, Object>> pages = new ArrayList<>();
                if (section.getPages() != null) {
                    for (PageEvent page : section.getPages()) {
                        pages.add(item(page));
                    }
                }
                map.put("pages", pages);
                out.add(map);
            }
        }
        return out;
    }

    private static Map<String, Object> currentPage(Model model) {
        if (!booleanAttr(model, "readingPage") && !booleanAttr(model, "readingAlbum")) {
            return null;
        }
        Object raw = model.getAttribute("sections");
        if (raw instanceof List<?> list && !list.isEmpty()
                && list.get(0) instanceof CategorySectionDTO section
                && section.getPages() != null && !section.getPages().isEmpty()) {
            return item(section.getPages().get(0));
        }
        return null;
    }

    private static List<Map<String, String>> comments(List<PageEventComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return List.of();
        }
        List<Map<String, String>> out = new ArrayList<>();
        for (PageEventComment comment : comments) {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("author", comment.getAuthor());
            map.put("date", comment.getDate());
            map.put("time", comment.getTime());
            map.put("message", comment.getMessage());
            out.add(map);
        }
        return out;
    }

    private static List<Map<String, String>> albumImages(List<AlbumImageDTO> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        List<Map<String, String>> out = new ArrayList<>();
        for (AlbumImageDTO image : images) {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("src", image.getSrc());
            map.put("full", image.getFull());
            map.put("alt", image.getAlt());
            map.put("description", image.getDescription());
            out.add(map);
        }
        return out;
    }

    private static boolean booleanAttr(Model model, String name) {
        Object value = model.getAttribute(name);
        return Boolean.TRUE.equals(value);
    }

    private static String stringAttr(Model model, String name) {
        Object value = model.getAttribute(name);
        return value == null ? null : value.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
