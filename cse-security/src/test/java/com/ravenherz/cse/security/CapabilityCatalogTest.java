package com.ravenherz.cse.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapabilityCatalogTest {

    @Test
    void engineRowsCarryHints() {
        CapabilityCatalog catalog = new CapabilityCatalog(null, null);
        for (CapabilityRecord record : catalog.engine()) {
            assertFalse(record.hint().isBlank(), record.id());
        }
        CapabilityRecord login = CapabilityRecord.app("login");
        assertTrue(login.hint().contains("login"));
        CapabilityRecord custom = CapabilityRecord.app("gallery");
        assertTrue(custom.hint().contains("gallery"));
    }
}
