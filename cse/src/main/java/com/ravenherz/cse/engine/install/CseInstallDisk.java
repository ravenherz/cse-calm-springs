package com.ravenherz.cse.engine.install;

import com.ravenherz.cse.install.InstallDisk;

import com.ravenherz.cse.util.io.CseDisk;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class CseInstallDisk implements InstallDisk {

    @Override
    public boolean siteReadyMarkerPresent() {
        return CseDisk.siteReadyMarkerPresent();
    }

    @Override
    public boolean writeSiteReadyMarker() {
        return CseDisk.writeSiteReadyMarker();
    }

    @Override
    public boolean deleteSiteReadyMarker() {
        return CseDisk.deleteSiteReadyMarker();
    }

    @Override
    public File secretFile(String filename) {
        return CseDisk.secretFile(filename);
    }
}
