package com.ravenherz.cse.dal.dao;

import com.ravenherz.cse.dal.dto.UrlTemplateEntity;

import java.util.List;

public interface UrlTemplateService extends Service {

    UrlTemplateEntity getByUrlTemplateId(String urlTemplateId);

    void delete(UrlTemplateEntity template);

    default List<UrlTemplateEntity> getAllUrlTemplates() {
        return getAll().stream()
                .filter(UrlTemplateEntity.class::isInstance)
                .map(UrlTemplateEntity.class::cast)
                .collect(java.util.stream.Collectors.toList());
    }
}
