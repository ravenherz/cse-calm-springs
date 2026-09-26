package com.ravenherz.cse.core.route;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservedPathsTest {

    @Test
    void prefixAndChildrenAreReserved() {
        assertTrue(ReservedPaths.isReserved("/editor"));
        assertTrue(ReservedPaths.isReserved("/editor/roles"));
        assertTrue(ReservedPaths.isReserved("/robots.txt"));
    }

    @Test
    void similarPublicPathsStayOpen() {
        assertFalse(ReservedPaths.isReserved("/editorial"));
        assertFalse(ReservedPaths.isReserved("/old-post"));
        assertFalse(ReservedPaths.isReserved("/"));
        assertFalse(ReservedPaths.isReserved(null));
    }
}
