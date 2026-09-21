package com.ravenherz.cse.security;

/**
 * Whether first-run setup has finished. {@code SiteReady} in the WAR is the implementation.
 */
public interface InstanceConfigured {

    boolean isConfigured();
}
