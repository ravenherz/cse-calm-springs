package com.ravenherz.cse.core.admin;

import java.util.List;
import java.util.Map;

/**
 * Stored rows for one generic editor section. The feature module implements this.
 * {@code cse-admin} never sees the feature entity.
 */
public interface AdminSectionRecords {

    String sectionId();

    List<Map<String, String>> list();

    Map<String, String> find(String id);

    List<AdminFieldError> create(Map<String, String> fields);

    List<AdminFieldError> update(String id, Map<String, String> fields);

    void delete(String id);
}
