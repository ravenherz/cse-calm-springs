package com.ravenherz.cse.install;

import com.ravenherz.cse.util.staticapps.StaticAppDeployer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SetupAppBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetupAppBootstrap.class);

    private final SiteReady siteReady;
    private final StaticAppDeployer deployer;

    public SetupAppBootstrap(SiteReady siteReady, StaticAppDeployer deployer) {
        this.siteReady = siteReady;
        this.deployer = deployer;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (siteReady.evaluateAtBoot()) {
            try {
                deployer.undeployEngineApp(StaticAppDeployer.INSTALLER_SLUG);
            } catch (IOException ex) {
                LOGGER.warn("Could not remove leftover installer: {}", ex.getMessage());
            }
            return;
        }
        explodeInstaller();
    }

    public boolean explodeInstaller() {
        try {
            byte[] zip = ClasspathFiles.readBytes(StaticAppDeployer.INSTALLER_CLASSPATH);
            if (zip == null || zip.length == 0) {
                LOGGER.error("Missing classpath {}", StaticAppDeployer.INSTALLER_CLASSPATH);
                return false;
            }
            deployer.deployEngineApp(StaticAppDeployer.INSTALLER_SLUG, zip);
            siteReady.holdInstallerOpen();
            return true;
        } catch (IOException ex) {
            LOGGER.error("Could not explode the setup app", ex);
            return false;
        }
    }
}
