package com.ravenherz.cse.engine.apps;

import com.ravenherz.cse.store.AppStoreAcl;

import com.ravenherz.cse.security.AccessRuntime;
import org.springframework.stereotype.Component;

/**
 * Store admin follows editor access. The check stays in {@code cse-security}; this library only receives the answer.
 */
@Component
public class AppStoreAdminBinder {

    public AppStoreAdminBinder() {
        AppStoreAcl.bindSiteAdmin(AccessRuntime::editorAccess);
    }
}
