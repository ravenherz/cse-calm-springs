package com.ravenherz.cse.dal.dao.impl;

import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.dao.BasicService;
import com.ravenherz.cse.dal.dao.UrlTemplateService;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.UrlTemplateEntity;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository(value = "urlTemplateService")
public class UrlTemplateServiceImpl extends BasicService implements UrlTemplateService {

    public UrlTemplateServiceImpl(DataProvider dataProvider) {
        super(dataProvider);
    }

    @Override
    public List<BasicEntity> getAll() {
        return new ArrayList<>(mongo().findAll(UrlTemplateEntity.class));
    }

    @Override
    public UrlTemplateEntity getByUrlTemplateId(String urlTemplateId) {
        if (urlTemplateId == null || urlTemplateId.isBlank()) {
            return null;
        }
        return mongo().findOne(Query.query(Criteria.where("urlTemplateId").is(urlTemplateId)),
                UrlTemplateEntity.class);
    }

    @Override
    public void delete(UrlTemplateEntity template) {
        if (template != null) {
            mongo().remove(template);
        }
    }
}
