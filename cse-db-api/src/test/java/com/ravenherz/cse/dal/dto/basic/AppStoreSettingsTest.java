package com.ravenherz.cse.dal.dto.basic;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppStoreSettingsTest {

    @Test
    void legacyFlagsBecomeSettingsWithDefaultQuotas() {
        AppStoreSettings settings = AppStoreSettings.fromLegacy(true, true);
        assertTrue(settings.isEnabled());
        assertTrue(settings.isSchemaOpen());
        assertEquals(AppStoreSettings.DEFAULT_MAX_DATA_BYTES, settings.resolvedMaxDataBytes());
        assertEquals(AppStoreSettings.DEFAULT_MAX_DOCS, settings.resolvedMaxDocs());
        assertEquals(AppStoreSettings.DEFAULT_MAX_BYTES, settings.resolvedMaxBytes());
    }

    @Test
    void outOfRangeQuotasFallBackToDefaults() {
        AppStoreSettings settings = new AppStoreSettings();
        settings.setMaxDataBytes(1);
        settings.setMaxDocs(0);
        settings.setMaxBytes(10);
        settings.normalize();
        assertEquals(AppStoreSettings.DEFAULT_MAX_DATA_BYTES, settings.getMaxDataBytes());
        assertEquals(AppStoreSettings.DEFAULT_MAX_DOCS, settings.getMaxDocs());
        assertEquals(AppStoreSettings.DEFAULT_MAX_BYTES, settings.getMaxBytes());
    }

    @Test
    void mapRoundTripKeepsGrantAndQuotas() {
        AppStoreSettings settings = AppStoreSettings.fromLegacy(true, false);
        settings.setMaxDataBytes(128 * 1024);
        settings.setMaxDocs(50);
        settings.setMaxBytes(8L * 1024 * 1024);
        AppStoreSettings back = AppStoreSettings.fromMap(settings.toMap());
        assertTrue(back.isEnabled());
        assertFalse(back.isSchemaOpen());
        assertEquals(128 * 1024, back.resolvedMaxDataBytes());
        assertEquals(50, back.resolvedMaxDocs());
        assertEquals(8L * 1024 * 1024, back.resolvedMaxBytes());
        assertEquals(128, back.maxDataKb());
        assertEquals(8, back.maxBytesMb());
    }

    @Test
    void appDataKeepsGrantAcrossSettingsApply() {
        AppData data = new AppData();
        data.setStoreEnabled(true);
        data.setStoreOpen(true);
        AppStoreSettings next = AppStoreSettings.copyOf(data.storeSettings());
        next.setMaxDataBytes(64 * 1024);
        data.applyStoreSettings(next);
        assertTrue(data.isStoreEnabled());
        assertTrue(data.isStoreOpen());
        assertEquals(64 * 1024, data.storeSettings().resolvedMaxDataBytes());
        assertTrue(data.storeSettings().toMap().containsKey("enabled"));
    }
}
