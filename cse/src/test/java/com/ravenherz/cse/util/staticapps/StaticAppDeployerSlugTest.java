package com.ravenherz.cse.util.staticapps;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StaticAppDeployerSlugTest {

    private final StaticAppDeployer deployer = new StaticAppDeployer();

    @Test
    void setupSlugCannotBeUploaded() {
        assertThrows(IllegalArgumentException.class, () -> deployer.validateSlug("setup"));
        assertThrows(IllegalArgumentException.class, () -> deployer.validateSlug("install"));
        assertThrows(IllegalArgumentException.class, () -> deployer.validateSlug("admin"));
        assertThrows(IllegalArgumentException.class, () -> deployer.validateSlug("login"));
        assertThrows(IllegalArgumentException.class, () -> deployer.validateSlug("apps"));
        assertDoesNotThrow(() -> deployer.validateSlug("hello"));
    }

    @Test
    void cmsPrefixCannotBeAnInstallSlug() {
        assertThrows(IllegalArgumentException.class, () -> deployer.validateSlug("cse"));
        assertThrows(IllegalArgumentException.class, () -> deployer.validateSlug("app-data"));
    }
}
