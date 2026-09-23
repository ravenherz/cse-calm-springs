package com.ravenherz.cse.dal.dto.basic;

import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityDataTest {

    @Test
    void thymeleafGettersWrapRules() {
        SecurityData data = new SecurityData();
        assertTrue(data.getRead().isInherit());
        assertTrue(data.getEdit().isInherit());
        assertTrue(data.getDelete().isInherit());
        AccessRule edit = AccessRule.ofRoles(List.of("member"));
        data.getAccessSettings().put(AccessType.ACCESS_EDIT, edit);
        assertEquals(edit, data.getEdit());
        assertEquals(data.rule(AccessType.ACCESS_READ), data.getRead());
        assertEquals(data.rule(AccessType.ACCESS_DELETE), data.getDelete());
    }
}
