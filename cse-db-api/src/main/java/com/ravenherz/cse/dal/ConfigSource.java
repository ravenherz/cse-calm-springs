package com.ravenherz.cse.dal;

/**
 * Read-only settings overlay used by persistence adapters.
 * Implemented by {@code com.ravenherz.cse.util.Settings} in the WAR.
 */
public interface ConfigSource {

    String getValue(String context, String key);
}
