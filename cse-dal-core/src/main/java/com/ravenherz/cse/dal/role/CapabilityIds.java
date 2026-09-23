package com.ravenherz.cse.dal.role;

import java.util.List;

public final class CapabilityIds {

    public static final String ACCOUNT_AUTH = "account.auth";
    public static final String ACCOUNT_REGISTER = "account.register";
    public static final String ACCOUNT_ACTIVATE = "account.activate";
    public static final String ACCOUNT_LOGOUT = "account.logout";
    public static final String ACCOUNT_ME = "account.me";

    public static final String SITE_READ = "site.read";
    public static final String CONTENT_PROTECTED = "content.protected";

    public static final String EDITOR_ACCESS = "editor.access";
    public static final String EDITOR_PAGES = "editor.pages";
    public static final String EDITOR_CATEGORIES = "editor.categories";
    public static final String EDITOR_FILES = "editor.files";
    public static final String EDITOR_PLAYLISTS = "editor.playlists";
    public static final String EDITOR_URL_TEMPLATES = "editor.url-templates";
    public static final String EDITOR_APPS = "editor.apps";
    public static final String EDITOR_THEMES = "editor.themes";
    public static final String EDITOR_ACCOUNTS = "editor.accounts";
    public static final String EDITOR_ROLES = "editor.roles";
    public static final String EDITOR_SETTINGS = "editor.settings";
    public static final String EDITOR_SITE_DATA = "editor.site-data";
    public static final String EDITOR_LOGS = "editor.logs";
    public static final String EDITOR_INSTANCE = "editor.instance";

    public static final String INSTALL = "install";

    public static final String CONTENT_READ = "content.read";
    public static final String CONTENT_EDIT = "content.edit";
    public static final String CONTENT_DELETE = "content.delete";

    public static final String APP_PREFIX = "app:";

    public static final List<String> EDITOR_ALL = List.of(
            EDITOR_ACCESS,
            EDITOR_PAGES,
            EDITOR_CATEGORIES,
            EDITOR_FILES,
            EDITOR_PLAYLISTS,
            EDITOR_URL_TEMPLATES,
            EDITOR_APPS,
            EDITOR_THEMES,
            EDITOR_ACCOUNTS,
            EDITOR_ROLES,
            EDITOR_SETTINGS,
            EDITOR_SITE_DATA,
            EDITOR_LOGS,
            EDITOR_INSTANCE);

    public static final List<String> ENGINE_ALL = List.of(
            ACCOUNT_AUTH,
            ACCOUNT_REGISTER,
            ACCOUNT_ACTIVATE,
            ACCOUNT_LOGOUT,
            ACCOUNT_ME,
            SITE_READ,
            CONTENT_PROTECTED,
            INSTALL,
            CONTENT_READ,
            CONTENT_EDIT,
            CONTENT_DELETE,
            EDITOR_ACCESS,
            EDITOR_PAGES,
            EDITOR_CATEGORIES,
            EDITOR_FILES,
            EDITOR_PLAYLISTS,
            EDITOR_URL_TEMPLATES,
            EDITOR_APPS,
            EDITOR_THEMES,
            EDITOR_ACCOUNTS,
            EDITOR_ROLES,
            EDITOR_SETTINGS,
            EDITOR_SITE_DATA,
            EDITOR_LOGS,
            EDITOR_INSTANCE);

    private CapabilityIds() {
    }

    public static String app(String slug) {
        return APP_PREFIX + slug;
    }

    public static boolean isApp(String capabilityId) {
        return capabilityId != null && capabilityId.startsWith(APP_PREFIX);
    }

    public static String appSlug(String capabilityId) {
        if (!isApp(capabilityId)) {
            return null;
        }
        return capabilityId.substring(APP_PREFIX.length());
    }
}
