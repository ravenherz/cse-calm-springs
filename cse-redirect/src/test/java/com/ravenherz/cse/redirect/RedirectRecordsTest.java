package com.ravenherz.cse.redirect;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedirectRecordsTest {

    @Test
    void saveStoresTheRowAndReloads() {
        MemoryRedirectStore store = new MemoryRedirectStore();
        AtomicInteger reloads = new AtomicInteger();
        RedirectRecords records = new RedirectRecords(store, List.of(reloads::incrementAndGet));

        List<com.ravenherz.cse.core.admin.AdminFieldError> errors = records.create(Map.of(
                "fromPath", "/rhz-we/*",
                "targetPath", "/*",
                "enabled", "true",
                "status", "301",
                "preserveQuery", "true"));

        assertTrue(errors.isEmpty());
        assertEquals(1, reloads.get());
        assertEquals("/rhz-we/*", records.list().get(0).get("fromPath"));
        assertEquals("/*", records.list().get(0).get("targetPath"));
    }
}
