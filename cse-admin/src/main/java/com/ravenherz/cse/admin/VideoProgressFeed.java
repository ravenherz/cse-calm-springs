package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * Live video progress for the editor. The WAR reads the queue and the store.
 */
public interface VideoProgressFeed {

    Map<String, Object> progress(String ids, HttpServletRequest request);
}
