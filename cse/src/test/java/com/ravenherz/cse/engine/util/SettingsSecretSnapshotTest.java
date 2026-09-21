package com.ravenherz.cse.engine.util;

import com.ravenherz.cse.constants.SettingKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsSecretSnapshotTest {

    private Settings settings;

    @BeforeEach
    void storage() {
        settings = new Settings();
        ReflectionTestUtils.setField(settings, "storage", new HashMap<String, Map<String, String>>());
    }

    @Test
    void snapshotOmitsSecretContexts() {
        settings.putValue(SettingKeys.CONTEXT_DATASOURCE_PERSONAL, SettingKeys.KEY_TAG_COMPANY_TITLE, "Site");
        settings.putValue(SettingKeys.CONTEXT_DATASOURCE_DBMS_ACCESS, SettingKeys.KEY_DBMS_ACCESS_PSWD, "secret");
        Map<String, Map<String, String>> snapshot = settings.getStorageSnapshot();
        assertTrue(snapshot.containsKey(SettingKeys.CONTEXT_DATASOURCE_PERSONAL));
        assertFalse(snapshot.containsKey(SettingKeys.CONTEXT_DATASOURCE_DBMS_ACCESS));
        assertFalse(snapshot.containsKey(SettingKeys.CONTEXT_DATASOURCE_DBMS_INSTANCE));
    }

    @Test
    void persistedOverlayIsEmptyWhenMongoIsUnbound() {
        assertTrue(settings.listPersistedContexts().isEmpty());
    }
}
