package com.ravenherz.cse.core;

/**
 * Whether first-run setup has finished. {@code SiteReady} in the WAR is the implementation.
 */
public interface SiteConfigured {

    boolean isConfigured();
}
