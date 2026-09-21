package com.ravenherz.cse.engine.apps;

import com.ravenherz.cse.util.staticapps.AppRoots;

import com.ravenherz.cse.util.io.CseDisk;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Points app deploy at this instance's exploded app directory.
 */
@Component("appRootsBinder")
@DependsOn("cseDiskBinder")
public class AppRootsBinder {

    public AppRootsBinder() {
        AppRoots.bind(new AppRoots.Paths() {
            @Override
            public Path appsDir() throws IOException {
                return CseDisk.staticPagesDir().getAbsoluteFile().toPath().normalize();
            }
        });
    }
}
