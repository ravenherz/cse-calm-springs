package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.dto.AccountEntity;

public final class AccessRuntime {

    private static volatile CapabilityService capabilities;

    private AccessRuntime() {
    }

    public static void bind(CapabilityService service) {
        capabilities = service;
    }

    public static CapabilityService capabilities() {
        return capabilities;
    }

    public static boolean editorAccess(AccountEntity account) {
        CapabilityService service = capabilities;
        if (service != null) {
            return service.canOpenEditor(account);
        }
        return CseAuthorities.legacyAdmin(account);
    }
}
