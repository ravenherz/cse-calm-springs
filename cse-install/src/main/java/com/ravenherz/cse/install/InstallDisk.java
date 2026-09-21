package com.ravenherz.cse.install;

import java.io.File;

/**
 * Site-ready marker and DBMS secret files. The WAR binds this to {@code CseDisk}.
 */
public interface InstallDisk {

    boolean siteReadyMarkerPresent();

    boolean writeSiteReadyMarker();

    boolean deleteSiteReadyMarker();

    File secretFile(String filename);
}
