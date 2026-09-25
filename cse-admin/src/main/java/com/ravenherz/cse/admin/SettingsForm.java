package com.ravenherz.cse.admin;

import org.springframework.ui.Model;

import java.util.Map;

/**
 * Settings groups and the public theme pair. The WAR reads and writes them.
 */
public interface SettingsForm {

    void fill(Model model);

    void save(Map<String, String> params, Model model);
}
