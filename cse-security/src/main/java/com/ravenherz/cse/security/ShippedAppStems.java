package com.ravenherz.cse.security;

/**
 * Slugs of apps packed with the WAR. The catalog adds a capability for each stem.
 */
public interface ShippedAppStems {

    Iterable<String> stems();
}
