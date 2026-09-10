package com.ravenherz.cse.controller;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicApiDocsTest {

    @Test
    void catalogListsPublicJsonAndInstallPaths() {
        List<String> paths = PublicApiDocs.paths();
        assertTrue(paths.contains("/account/auth"));
        assertTrue(paths.contains("/account/logout"));
        assertTrue(paths.contains("/account/register"));
        assertTrue(paths.contains("/account/activate"));
        assertTrue(paths.contains("/rest/forms/render"));
        assertTrue(paths.contains("/rest/markdown/render"));
        assertTrue(paths.contains("/rest/error"));
        assertTrue(paths.contains("/rest/site"));
        assertTrue(paths.contains("/"));
        assertTrue(paths.contains("/content-protected/**"));
        assertTrue(paths.contains("/static-pages/{slug}/**"));
        assertTrue(paths.contains("/install/status"));
        assertTrue(paths.contains("/install/mongo"));
        assertTrue(paths.contains("/install/owner"));
        assertTrue(paths.contains("/install/finish"));
        assertTrue(paths.contains("/app-data/{slug}/{table}"));
        assertTrue(paths.contains("/app-data/{slug}/_schema"));
        assertFalse(paths.stream().anyMatch(path -> path.startsWith("/editor")));
        assertEquals(5, PublicApiDocs.sections().size());
    }

    @Test
    void mutatingHelpersRequireCsrf() {
        boolean authCsrf = PublicApiDocs.sections().stream()
                .flatMap(section -> section.endpoints().stream())
                .filter(endpoint -> "/account/auth".equals(endpoint.path()))
                .findFirst()
                .orElseThrow()
                .csrf();
        assertTrue(authCsrf);
        boolean statusCsrf = PublicApiDocs.sections().stream()
                .flatMap(section -> section.endpoints().stream())
                .filter(endpoint -> "/install/status".equals(endpoint.path()))
                .findFirst()
                .orElseThrow()
                .csrf();
        assertFalse(statusCsrf);
        boolean siteCsrf = PublicApiDocs.sections().stream()
                .flatMap(section -> section.endpoints().stream())
                .filter(endpoint -> "/rest/site".equals(endpoint.path()))
                .findFirst()
                .orElseThrow()
                .csrf();
        assertFalse(siteCsrf);
    }
}
