package com.ravenherz.cse.core.route;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathTargetTest {

    @Test
    void keepsStatusAndQueryFlag() {
        PathTarget target = new PathTarget("/?page=new-post", PathTarget.MOVED_PERMANENTLY, true);

        assertEquals("/?page=new-post", target.targetPath());
        assertEquals(301, target.status());
        assertTrue(target.preserveQuery());
    }

    @Test
    void rejectsOtherStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> new PathTarget("/next", 200, false));
    }

    @Test
    void rejectsPathWithoutSlash() {
        assertThrows(IllegalArgumentException.class,
                () -> new PathTarget("next", PathTarget.FOUND, false));
    }
}
