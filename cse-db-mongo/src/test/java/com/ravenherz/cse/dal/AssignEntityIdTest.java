package com.ravenherz.cse.dal;

import com.ravenherz.cse.dal.dto.DataChunkEntity;
import com.ravenherz.cse.dal.dto.SettingContextEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AssignEntityIdTest {

    @Test
    void assignsIdWhenMissing() {
        DataChunkEntity chunk = new DataChunkEntity("bytes");
        assertNull(chunk.getId());

        new AssignEntityId().onBeforeConvert(chunk, "datachunks");

        assertNotNull(chunk.getId());
        assertEquals(24, chunk.getId().hex().length());
    }

    @Test
    void keepsExistingId() {
        DataChunkEntity chunk = new DataChunkEntity("bytes");
        EntityId id = EntityId.generate();
        chunk.setId(id);

        new AssignEntityId().onBeforeConvert(chunk, "datachunks");

        assertSame(id, chunk.getId());
    }

    @Test
    void leavesNonBasicEntitiesAlone() {
        SettingContextEntity settings = new SettingContextEntity();
        assertNull(settings.getId());

        new AssignEntityId().onBeforeConvert(settings, "settings");

        assertNull(settings.getId());
    }
}
