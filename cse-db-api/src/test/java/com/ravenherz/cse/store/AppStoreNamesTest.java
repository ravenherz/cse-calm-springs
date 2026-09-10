package com.ravenherz.cse.store;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppStoreNamesTest {

    @Test
    void collectionNameUsesSlugPrefixAndTable() {
        assertEquals("fretlab-progress", AppStoreNames.collectionName("fretlab", "progress"));
        assertEquals("hello-snake-scores", AppStoreNames.collectionName("hello-snake", "scores"));
        assertEquals("fretlab-progress", AppStoreNames.collectionName("FretLab", "Progress"));
    }

    @Test
    void slugIsNeverTakenFromAFilename() {
        assertThrows(AppStoreException.class,
                () -> AppStoreNames.collectionName("scores.cseapp", "scores"));
        assertFalse(AppStoreNames.isSlug("scores.cseapp"));
    }

    @Test
    void cmsCollectionsAreNotAppTables() {
        assertNull(AppStoreNames.slugOf("cse-accounts"));
        assertNull(AppStoreNames.tableOf("cse-accounts"));
        assertFalse(AppStoreNames.isAppCollection("cse-accounts"));
        assertNull(AppStoreNames.slugOf("cse-datachunks"));
        assertFalse(AppStoreNames.belongsTo("cse-accounts", "cse"));
        assertThrows(AppStoreException.class, () -> AppStoreNames.collectionName("cse", "accounts"));
        assertThrows(AppStoreException.class, () -> AppStoreNames.collectionName("admin", "progress"));
    }

    @Test
    void tableInjectionCannotReachCms() {
        assertThrows(AppStoreException.class, () -> AppStoreNames.requireTable("cse-accounts"));
        assertThrows(AppStoreException.class, () -> AppStoreNames.requireTable("../accounts"));
        assertThrows(AppStoreException.class, () -> AppStoreNames.requireTable(""));
        assertThrows(AppStoreException.class, () -> AppStoreNames.requireTable("_schema"));
        assertFalse(AppStoreNames.isTable("cse-accounts"));
        assertFalse(AppStoreNames.isTable("progress-v2"));
        assertTrue(AppStoreNames.isTable("progress"));
        assertTrue(AppStoreNames.isReservedTable("_schema"));
    }

    @Test
    void lastHyphenIsTheTableBoundary() {
        assertEquals("hello-snake", AppStoreNames.slugOf("hello-snake-scores"));
        assertEquals("scores", AppStoreNames.tableOf("hello-snake-scores"));
        assertTrue(AppStoreNames.belongsTo("hello-snake-scores", "hello-snake"));
        assertFalse(AppStoreNames.belongsTo("hello-snake-scores", "hello"));
        assertFalse(AppStoreNames.belongsTo("fretlab-progress", "hello-snake"));
    }

    @Test
    void slugPatternMatchesInstallRules() {
        assertTrue(Pattern.compile("^[a-z0-9][a-z0-9-]{0,62}$").matcher("fretlab").matches());
        assertTrue(AppStoreNames.isSlug("a"));
        assertFalse(AppStoreNames.isSlug("-nope"));
        assertTrue(AppStoreNames.isStoreIneligible("app-data"));
        assertTrue(AppStoreNames.isStoreIneligible("setup"));
    }

    @Test
    void tableSpecMergeKeepsOperatorRows() {
        AppStoreTableSpec owner = new AppStoreTableSpec("progress", AppStoreAccess.OWNER, null);
        AppStoreTableSpec scores = new AppStoreTableSpec("scores", AppStoreAccess.PUBLIC_READ, Map.of());
        List<AppStoreTableSpec> merged = AppStoreTableSpec.merge(
                List.of(owner), List.of(owner, scores));
        assertEquals(2, merged.size());
        assertEquals("progress", merged.get(0).getName());
        assertEquals("scores", merged.get(1).getName());
        assertEquals("owner", merged.get(0).toMap().get("access"));
    }

    @Test
    void tableSpecFromMapSkipsInvalidNames() {
        Map<String, Object> bad = new LinkedHashMap<>();
        bad.put("name", "cse-accounts");
        assertNull(AppStoreTableSpec.fromMap(bad));
        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("name", "progress");
        ok.put("access", "public-read");
        AppStoreTableSpec spec = AppStoreTableSpec.fromMap(ok);
        assertEquals(AppStoreAccess.PUBLIC_READ, spec.getAccess());
        assertEquals(List.of(), AppStoreTableSpec.listFrom(null));
        assertEquals(1, AppStoreTableSpec.listFrom(List.of(ok)).size());
    }
}
