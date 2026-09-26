package com.ravenherz.cse.redirect;

/**
 * The WAR reloads its in-memory map after a row is saved.
 */
public interface RedirectReload {

    void reloadRedirects();
}
