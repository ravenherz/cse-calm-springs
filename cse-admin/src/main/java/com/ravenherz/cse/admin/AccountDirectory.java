package com.ravenherz.cse.admin;

import java.util.Locale;

/**
 * Login search and avatar fallbacks for the Accounts directory.
 */
public final class AccountDirectory {

    private AccountDirectory() {
    }

    public static String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        return query.trim();
    }

    public static boolean loginMatches(String login, String query) {
        String needle = normalizeQuery(query);
        if (needle.isEmpty()) {
            return true;
        }
        if (login == null) {
            return false;
        }
        return login.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    public static String initial(String login) {
        if (login == null) {
            return "?";
        }
        String trimmed = login.trim();
        if (trimmed.isEmpty()) {
            return "?";
        }
        int cp = trimmed.codePointAt(0);
        return new String(Character.toChars(cp)).toUpperCase(Locale.ROOT);
    }

    public static String safeAvatar(String avatar) {
        if (avatar == null || avatar.isBlank()) {
            return null;
        }
        if (avatar.regionMatches(true, 0, "data:image/", 0, 11)) {
            return avatar;
        }
        return null;
    }
}
