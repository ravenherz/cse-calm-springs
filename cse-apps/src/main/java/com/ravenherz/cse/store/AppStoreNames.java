package com.ravenherz.cse.store;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Collection names for packed-app JSON: {@code {slug}-{table}}.
 * The slug is the install prefix. The zip filename is never used here.
 */
public final class AppStoreNames {

    public static final Pattern SLUG = Pattern.compile("^[a-z0-9][a-z0-9-]{0,62}$");
    public static final Pattern TABLE = Pattern.compile("^[a-z][a-z0-9]{0,31}$");
    public static final String CMS_SLUG = "cse";
    public static final Set<String> STORE_INELIGIBLE = Set.of(
            CMS_SLUG, "admin", "setup", "app-data");

    private AppStoreNames() {
    }

    public static String collectionName(String slug, String table) {
        String normalizedSlug = requireSlug(slug);
        String normalizedTable = requireTable(table);
        return normalizedSlug + "-" + normalizedTable;
    }

    public static String requireSlug(String slug) {
        String normalized = normalize(slug);
        if (!isSlug(normalized) || isStoreIneligible(normalized)) {
            throw new AppStoreException(400, "Invalid app slug");
        }
        return normalized;
    }

    public static String requireTable(String table) {
        String normalized = normalize(table);
        if (!isTable(normalized)) {
            throw new AppStoreException(400, "Invalid table name");
        }
        return normalized;
    }

    public static boolean isSlug(String slug) {
        String normalized = normalize(slug);
        return !normalized.isEmpty() && SLUG.matcher(normalized).matches();
    }

    public static boolean isTable(String table) {
        String normalized = normalize(table);
        return !normalized.isEmpty() && TABLE.matcher(normalized).matches();
    }

    public static boolean isReservedTable(String table) {
        String normalized = normalize(table);
        return normalized.startsWith("_");
    }

    public static boolean isStoreIneligible(String slug) {
        return STORE_INELIGIBLE.contains(normalize(slug));
    }

    /**
     * True when {@code collection} can only be an app table (not a CMS {@code cse-*} name).
     * Split from the right: the last segment is the table (no hyphens).
     */
    public static boolean isAppCollection(String collection) {
        String slug = slugOf(collection);
        String table = tableOf(collection);
        return slug != null && table != null;
    }

    public static boolean belongsTo(String collection, String slug) {
        String normalizedSlug = normalize(slug);
        if (!isSlug(normalizedSlug) || isStoreIneligible(normalizedSlug)) {
            return false;
        }
        String table = tableOf(collection);
        if (table == null) {
            return false;
        }
        return collectionName(normalizedSlug, table).equals(normalize(collection));
    }

    public static String slugOf(String collection) {
        String normalized = normalize(collection);
        int dash = normalized.lastIndexOf('-');
        if (dash <= 0 || dash == normalized.length() - 1) {
            return null;
        }
        String slug = normalized.substring(0, dash);
        String table = normalized.substring(dash + 1);
        if (!isSlug(slug) || isStoreIneligible(slug) || !isTable(table)) {
            return null;
        }
        return slug;
    }

    public static String tableOf(String collection) {
        String normalized = normalize(collection);
        int dash = normalized.lastIndexOf('-');
        if (dash <= 0 || dash == normalized.length() - 1) {
            return null;
        }
        String table = normalized.substring(dash + 1);
        String slug = normalized.substring(0, dash);
        if (!isSlug(slug) || isStoreIneligible(slug) || !isTable(table)) {
            return null;
        }
        return table;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
