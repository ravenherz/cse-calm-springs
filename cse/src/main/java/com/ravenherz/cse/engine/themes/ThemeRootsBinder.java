package com.ravenherz.cse.engine.themes;

import com.ravenherz.cse.util.themes.ThemeRoots;

import com.ravenherz.cse.util.io.CseDisk;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Points theme deploy and template lookup at this instance's disk.
 */
@Component("themeRootsBinder")
@DependsOn("cseDiskBinder")
public class ThemeRootsBinder {

    public ThemeRootsBinder() {
        ThemeRoots.bind(new ThemeRoots.Paths() {
            @Override
            public Path themesDir() throws IOException {
                return CseDisk.themesDir().getAbsoluteFile().toPath().normalize();
            }

            @Override
            public Path appsDir() throws IOException {
                return CseDisk.staticPagesDir().getAbsoluteFile().toPath().normalize();
            }
        });
    }
}
