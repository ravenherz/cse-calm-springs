package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.dao.AppService;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.basic.AppData;
import com.ravenherz.cse.dal.role.CapabilityIds;
import com.ravenherz.cse.util.frontend.ShippedPackCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

@Component
public class CapabilityCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(CapabilityCatalog.class);

    private static final List<CapabilityRecord> ENGINE = List.of(
            CapabilityRecord.engine(CapabilityIds.ACCOUNT_AUTH, "account", "Sign in", true,
                    "Post credentials to sign in."),
            CapabilityRecord.engine(CapabilityIds.ACCOUNT_REGISTER, "account", "Register", true,
                    "Create a new account."),
            CapabilityRecord.engine(CapabilityIds.ACCOUNT_ACTIVATE, "account", "Activate account", true,
                    "Activate an account from the email link."),
            CapabilityRecord.engine(CapabilityIds.ACCOUNT_LOGOUT, "account", "Sign out", false,
                    "End the current session."),
            CapabilityRecord.engine(CapabilityIds.ACCOUNT_ME, "account", "Edit own account", false,
                    "View and update the signed-in profile."),
            CapabilityRecord.engine(CapabilityIds.SITE_READ, "site", "Public site", true,
                    "Load the public home page and site JSON."),
            CapabilityRecord.engine(CapabilityIds.CONTENT_PROTECTED, "site", "Protected media", true,
                    "Fetch files from protected media URLs."),
            CapabilityRecord.engine(CapabilityIds.INSTALL, "install", "First-run install", true,
                    "Use the first-boot installer endpoints."),
            CapabilityRecord.engine(CapabilityIds.CONTENT_READ, "content", "Default content read", true,
                    "Read pages, files, and folders when Access inherits and no parent rule applies."),
            CapabilityRecord.engine(CapabilityIds.CONTENT_EDIT, "content", "Default content edit", false,
                    "Edit pages, files, and folders when Access inherits and no parent rule applies."),
            CapabilityRecord.engine(CapabilityIds.CONTENT_DELETE, "content", "Default content delete", false,
                    "Delete pages, files, and folders when Access inherits and no parent rule applies."),
            CapabilityRecord.engine(CapabilityIds.EDITOR_ACCESS, "editor", "Open Catalog", false,
                    "Open Catalog. Anyone without this is signed out of the editor."),
            CapabilityRecord.engine(CapabilityIds.EDITOR_PAGES, "editor", "Pages", false,
                    "Create and edit pages and albums."),
            CapabilityRecord.engine(CapabilityIds.EDITOR_CATEGORIES, "editor", "Categories", false,
                    "Create and edit categories."),
            CapabilityRecord.engine(CapabilityIds.EDITOR_FILES, "editor", "Files and folders", false,
                    "Manage files and folders in Catalog."),
            CapabilityRecord.engine(CapabilityIds.EDITOR_PLAYLISTS, "editor", "Playlists", false,
                    "Create and edit playlists."),
            CapabilityRecord.engine(CapabilityIds.EDITOR_APPS, "editor", "Install apps", false,
                    "Install and manage apps."),
            CapabilityRecord.engine(CapabilityIds.EDITOR_THEMES, "editor", "Install themes", false,
                    "Install and manage themes."),
            CapabilityRecord.engine(CapabilityIds.EDITOR_ACCOUNTS, "editor", "Accounts", false,
                    "Open the Accounts tab."),
            CapabilityRecord.engine(CapabilityIds.EDITOR_ROLES, "editor", "Roles tab", false,
                    "Open the Roles tab."),
            CapabilityRecord.engine(CapabilityIds.EDITOR_SETTINGS, "editor", "Settings", false,
                    "Open Settings."),
            CapabilityRecord.engine(CapabilityIds.EDITOR_SITE_DATA, "editor", "Site data", false,
                    "Import and export site data."),
            CapabilityRecord.engine(CapabilityIds.EDITOR_LOGS, "editor", "Logs", false,
                    "Open Logs."));

    private final AppService appService;
    private volatile List<CapabilityRecord> all = List.of();

    public CapabilityCatalog(AppService appService) {
        this.appService = appService;
        refresh();
    }

    public List<CapabilityRecord> engine() {
        return ENGINE;
    }

    public List<CapabilityRecord> all() {
        return all;
    }

    public CapabilityRecord find(String id) {
        if (id == null) {
            return null;
        }
        for (CapabilityRecord record : all) {
            if (id.equals(record.id())) {
                return record;
            }
        }
        return null;
    }

    public synchronized void refresh() {
        Map<String, CapabilityRecord> byId = new LinkedHashMap<>();
        for (CapabilityRecord record : ENGINE) {
            byId.put(record.id(), record);
        }
        TreeSet<String> slugs = new TreeSet<>();
        slugs.add("login");
        slugs.add("setup");
        slugs.add("admin");
        try {
            for (ShippedPackCatalog.Pack pack : ShippedPackCatalog.apps()) {
                if (pack != null && pack.stem() != null && !pack.stem().isBlank()) {
                    slugs.add(pack.stem().toLowerCase(Locale.ROOT));
                }
            }
        } catch (Exception ex) {
            LOGGER.debug("Shipped apps for catalog: {}", ex.getMessage());
        }
        try {
            if (appService != null) {
                for (var entity : appService.getAll()) {
                    if (entity instanceof AppEntity app && app.getAppData() != null) {
                        AppData data = app.getAppData();
                        if (data.getSlug() != null && !data.getSlug().isBlank()) {
                            slugs.add(data.getSlug().trim().toLowerCase(Locale.ROOT));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.debug("Installed apps for catalog: {}", ex.getMessage());
        }
        for (String slug : slugs) {
            CapabilityRecord record = CapabilityRecord.app(slug);
            byId.put(record.id(), record);
        }
        List<CapabilityRecord> records = new ArrayList<>(byId.values());
        records.sort(Comparator.comparing(CapabilityRecord::group).thenComparing(CapabilityRecord::id));
        this.all = List.copyOf(records);
    }
}
