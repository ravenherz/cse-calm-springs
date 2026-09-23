package com.ravenherz.cse.dal.role;

import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RoleSeeds {

    public static final String GUEST = "guest";
    public static final String OWNER = "owner";
    public static final String INACTIVE = "inactive";
    public static final String MEMBER = "member";
    public static final String PRIVILEGED = "privileged";
    public static final String OPERATOR = "operator";
    public static final String MODERATOR = "moderator";
    public static final String ADMIN = "admin";

    public static final String SYSTEM_GUEST = "guest";
    public static final String SYSTEM_OWNER = "owner";

    public static final Set<String> SYSTEM_SLUGS = Set.of(GUEST, OWNER);
    public static final Set<String> RETIRED_SLUGS = Set.of("guide");

    private static final List<SeedRole> ROLES = List.of(
            new SeedRole(GUEST, "Guest", SYSTEM_GUEST, false, 0),
            new SeedRole(INACTIVE, "Inactive", null, false, 10),
            new SeedRole(MEMBER, "Member", null, true, 20),
            new SeedRole(PRIVILEGED, "Privileged", null, true, 30),
            new SeedRole(OPERATOR, "Operator", null, true, 50),
            new SeedRole(MODERATOR, "Moderator", null, true, 60),
            new SeedRole(ADMIN, "Admin", null, true, 70),
            new SeedRole(OWNER, "Owner", SYSTEM_OWNER, true, 1000));

    private static final Map<SecurityLevel, String> SLUG_BY_LEVEL = Map.ofEntries(
            Map.entry(SecurityLevel.GUEST, GUEST),
            Map.entry(SecurityLevel.INACTIVE_USER, INACTIVE),
            Map.entry(SecurityLevel.ACTIVE_USER, MEMBER),
            Map.entry(SecurityLevel.PRIVILEGIED_USER, PRIVILEGED),
            Map.entry(SecurityLevel.OPERATOR, OPERATOR),
            Map.entry(SecurityLevel.MODERATOR, MODERATOR),
            Map.entry(SecurityLevel.ADMIN, ADMIN),
            Map.entry(SecurityLevel.OWNER, OWNER));

    private static final Map<String, SecurityLevel> LEVEL_BY_SLUG = inverse(SLUG_BY_LEVEL);

    private static final List<String> LADDER = List.of(
            GUEST, INACTIVE, MEMBER, PRIVILEGED, OPERATOR, MODERATOR, ADMIN, OWNER);

    private RoleSeeds() {
    }

    public static List<SeedRole> roles() {
        return ROLES;
    }

    public static boolean isRetiredSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return false;
        }
        return RETIRED_SLUGS.contains(slug.trim().toLowerCase(Locale.ROOT));
    }

    public static String slugFor(SecurityLevel level) {
        if (level == null) {
            return null;
        }
        return SLUG_BY_LEVEL.get(level);
    }

    public static SecurityLevel levelForSlug(String slug) {
        if (slug == null) {
            return null;
        }
        return LEVEL_BY_SLUG.get(slug);
    }

    public static SecurityLevel dummyLevel(String slug, boolean editorAccess) {
        SecurityLevel mapped = levelForSlug(slug);
        if (mapped != null) {
            return mapped;
        }
        return editorAccess ? SecurityLevel.ADMIN : SecurityLevel.ACTIVE_USER;
    }

    public static boolean loginableFor(SecurityLevel level) {
        String slug = slugFor(level);
        if (slug == null) {
            return false;
        }
        for (SeedRole role : ROLES) {
            if (role.slug().equals(slug)) {
                return role.loginable();
            }
        }
        return false;
    }

    public static List<String> slugsAtOrAbove(SecurityLevel threshold) {
        if (threshold == null) {
            return List.of();
        }
        String start = slugFor(threshold);
        int index = LADDER.indexOf(start);
        if (index < 0) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (int i = index; i < LADDER.size(); i++) {
            String slug = LADDER.get(i);
            if (!OWNER.equals(slug)) {
                out.add(slug);
            }
        }
        return out;
    }

    public static List<String> contentDefaultSlugs(String capabilityId) {
        if (CapabilityIds.CONTENT_READ.equals(capabilityId)) {
            return List.of(GUEST, MEMBER, PRIVILEGED, OPERATOR, MODERATOR, ADMIN);
        }
        if (CapabilityIds.CONTENT_EDIT.equals(capabilityId)) {
            return List.of(ADMIN);
        }
        if (CapabilityIds.CONTENT_DELETE.equals(capabilityId)) {
            return List.of(MODERATOR, ADMIN);
        }
        return List.of();
    }

    public static Map<String, List<String>> seedGrantsBySlug() {
        Map<String, List<String>> bySlug = new LinkedHashMap<>();
        bySlug.put(GUEST, guestGrants());
        bySlug.put(MEMBER, loginablePublicGrants());
        bySlug.put(PRIVILEGED, loginablePublicGrants());
        bySlug.put(OPERATOR, loginablePublicGrants());
        bySlug.put(MODERATOR, withDelete(loginablePublicGrants()));
        bySlug.put(ADMIN, adminGrants());
        return bySlug;
    }

    private static List<String> guestGrants() {
        return List.of(
                CapabilityIds.ACCOUNT_AUTH,
                CapabilityIds.ACCOUNT_REGISTER,
                CapabilityIds.ACCOUNT_ACTIVATE,
                CapabilityIds.SITE_READ,
                CapabilityIds.CONTENT_PROTECTED,
                CapabilityIds.INSTALL,
                CapabilityIds.CONTENT_READ,
                CapabilityIds.app("login"),
                CapabilityIds.app("setup"));
    }

    private static List<String> loginablePublicGrants() {
        List<String> ids = new ArrayList<>();
        ids.add(CapabilityIds.ACCOUNT_AUTH);
        ids.add(CapabilityIds.ACCOUNT_REGISTER);
        ids.add(CapabilityIds.ACCOUNT_ACTIVATE);
        ids.add(CapabilityIds.ACCOUNT_LOGOUT);
        ids.add(CapabilityIds.ACCOUNT_ME);
        ids.add(CapabilityIds.SITE_READ);
        ids.add(CapabilityIds.CONTENT_PROTECTED);
        ids.add(CapabilityIds.CONTENT_READ);
        ids.add(CapabilityIds.app("login"));
        ids.add(CapabilityIds.app("setup"));
        return ids;
    }

    private static List<String> withDelete(List<String> base) {
        List<String> ids = new ArrayList<>(base);
        ids.add(CapabilityIds.CONTENT_DELETE);
        return ids;
    }

    private static List<String> adminGrants() {
        List<String> ids = withDelete(loginablePublicGrants());
        ids.add(CapabilityIds.CONTENT_EDIT);
        ids.addAll(CapabilityIds.EDITOR_ALL);
        ids.add(CapabilityIds.app("admin"));
        return ids;
    }

    private static Map<String, SecurityLevel> inverse(Map<SecurityLevel, String> source) {
        Map<String, SecurityLevel> bySlug = new LinkedHashMap<>();
        for (Map.Entry<SecurityLevel, String> entry : source.entrySet()) {
            bySlug.put(entry.getValue(), entry.getKey());
        }
        return bySlug;
    }

    public record SeedRole(String slug, String name, String system, boolean loginable, int sortOrder) {
        public boolean systemRole() {
            return system != null;
        }
    }
}
