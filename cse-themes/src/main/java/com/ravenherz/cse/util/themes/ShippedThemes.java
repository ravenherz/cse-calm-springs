package com.ravenherz.cse.util.themes;

import java.util.List;

/**
 * Theme packs shipped inside the WAR. The WAR reads them from the classpath.
 */
public interface ShippedThemes {

    List<Pack> packs();

    record Pack(String stem, byte[] bytes) {
        public Pack {
            bytes = bytes == null ? new byte[0] : bytes;
        }
    }
}
