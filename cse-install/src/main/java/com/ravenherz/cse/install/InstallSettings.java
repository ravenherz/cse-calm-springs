package com.ravenherz.cse.install;

/**
 * Settings written during first-run setup. {@code Settings} in the WAR is the implementation.
 */
public interface InstallSettings {

    String getValue(String context, String key);

    void putValue(String context, String key, String value);

    boolean persistSecretsToDisk();

    boolean persistContext(String context);
}
