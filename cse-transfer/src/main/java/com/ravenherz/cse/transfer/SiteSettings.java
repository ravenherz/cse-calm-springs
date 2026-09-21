package com.ravenherz.cse.transfer;

/**
 * Settings the importer may rewrite. {@code Settings} in the WAR is the implementation.
 */
public interface SiteSettings {

    boolean isOverlayContext(String context);

    void reloadFromMongo();
}
