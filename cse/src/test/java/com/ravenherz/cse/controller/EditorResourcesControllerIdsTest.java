package com.ravenherz.cse.controller;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EditorResourcesControllerIdsTest {

    @Test
    void uniqueIdsKeepsOrderAndDropsBlanks() {
        assertEquals(List.of("a", "b"),
                EditorResourcesController.uniqueIds(java.util.Arrays.asList(" a ", "", "a", "b", null)));
        assertEquals(List.of(), EditorResourcesController.uniqueIds(null));
        assertEquals(List.of(), EditorResourcesController.uniqueIds(List.of("  ")));
    }
}
