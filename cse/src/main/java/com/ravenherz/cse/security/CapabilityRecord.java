package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.role.CapabilityIds;

public record CapabilityRecord(String id, String group, String label, boolean guestSafe, String hint) {

    public CapabilityRecord {
        hint = hint == null ? "" : hint;
    }

    public static CapabilityRecord engine(String id, String group, String label, boolean guestSafe) {
        return engine(id, group, label, guestSafe, "");
    }

    public static CapabilityRecord engine(String id, String group, String label, boolean guestSafe, String hint) {
        return new CapabilityRecord(id, group, label, guestSafe, hint);
    }

    public static CapabilityRecord app(String slug) {
        boolean guestSafe = "login".equals(slug) || "setup".equals(slug);
        String label = switch (slug) {
            case "login" -> "Login app";
            case "setup" -> "Setup app";
            case "admin" -> "Admin app";
            default -> "App: " + slug;
        };
        String hint = switch (slug) {
            case "login" -> "Open the login app on the public site.";
            case "setup" -> "Open the first-run setup app.";
            case "admin" -> "Open the admin app. Catalog access also grants this.";
            default -> "Open the " + slug + " app on the public site.";
        };
        return new CapabilityRecord(CapabilityIds.app(slug), "apps", label, guestSafe, hint);
    }
}
