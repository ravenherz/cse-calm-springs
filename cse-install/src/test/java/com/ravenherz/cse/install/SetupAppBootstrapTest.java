package com.ravenherz.cse.install;

import com.ravenherz.cse.util.staticapps.StaticAppDeployer;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SetupAppBootstrapTest {

    @Test
    void configuredBootUndeploysInstaller() throws IOException {
        SiteReady siteReady = mock(SiteReady.class);
        StaticAppDeployer deployer = mock(StaticAppDeployer.class);
        when(siteReady.evaluateAtBoot()).thenReturn(true);

        new SetupAppBootstrap(siteReady, deployer).run(null);

        verify(deployer).undeployEngineApp(StaticAppDeployer.INSTALLER_SLUG);
    }

    @Test
    void unconfiguredBootDoesNotUndeployInstaller() throws IOException {
        SiteReady siteReady = mock(SiteReady.class);
        StaticAppDeployer deployer = mock(StaticAppDeployer.class);
        when(siteReady.evaluateAtBoot()).thenReturn(false);

        new SetupAppBootstrap(siteReady, deployer).run(null);

        verify(deployer, never()).undeployEngineApp(StaticAppDeployer.INSTALLER_SLUG);
    }
}
