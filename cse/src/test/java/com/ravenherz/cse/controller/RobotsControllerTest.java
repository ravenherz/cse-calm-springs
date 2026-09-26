package com.ravenherz.cse.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RobotsControllerTest {

    @Test
    void rootBlocksPrivateAreasAndSiblingContexts() {
        String body = RobotsController.body("");

        assertTrue(body.startsWith("User-agent: *\nAllow: /\n"));
        assertTrue(body.contains("Disallow: /editor\n"));
        assertTrue(body.contains("Disallow: /apps/login\n"));
        assertTrue(body.contains("Disallow: /apps/setup\n"));
        assertTrue(body.contains("Disallow: /apps/admin\n"));
        assertTrue(body.contains("Disallow: /content-private\n"));
        assertTrue(body.contains("Disallow: /*/editor\n"));
        assertTrue(body.contains("Disallow: /*/apps/admin\n"));
        assertFalse(body.contains("Disallow: /rhz-we"));
        assertFalse(body.contains("Disallow: /content-public\n"));
    }

    @Test
    void contextPathIsPrefixed() {
        String body = RobotsController.body("/other-context");

        assertTrue(body.contains("Allow: /other-context/\n"));
        assertTrue(body.contains("Disallow: /other-context/editor\n"));
        assertTrue(body.contains("Disallow: /other-context/install\n"));
        assertFalse(body.contains("Disallow: /*/editor"));
        assertFalse(body.contains("Disallow: /editor\n"));
    }
}
