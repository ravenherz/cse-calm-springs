package com.ravenherz.cse.dal;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EntityVersionsTest {

    @AfterEach
    void unbind() {
        EntityVersions.bind(null);
    }

    @Test
    void unboundFallsBack() {
        assertEquals(EntityVersions.FALLBACK, EntityVersions.current());
    }

    @Test
    void createdEntitiesUseSettingsVersion() {
        EntityVersions.bind((context, key) -> {
            if (SettingKeys.CONTEXT_DATASOURCE_BUILD_INFO.equals(context)
                    && SettingKeys.KEY_TAG_SOFT_VERSION_VERSION.equals(key)) {
                return " 0.7.0.3437 ";
            }
            return null;
        });
        assertEquals("0.7.0.3437", EntityVersions.current());
        assertEquals("0.7.0.3437", new ItemEntity("hello", new PageData(), null).getEntityVersion());
        assertEquals("0.7.0.3437", new AccountEntity(new AccountData("ada", "hash", "a@example.com",
                SecurityLevel.OWNER)).getEntityVersion());
        assertEquals("0.7.0.3437", new RoleEntity().getEntityVersion());
    }

    @Test
    void blankSettingsFallBack() {
        EntityVersions.bind((context, key) -> "  ");
        assertEquals(EntityVersions.FALLBACK, EntityVersions.current());
    }

    @Test
    void mappingConstructorLeavesVersionUnset() {
        assertNull(new ItemEntity().getEntityVersion());
    }
}
