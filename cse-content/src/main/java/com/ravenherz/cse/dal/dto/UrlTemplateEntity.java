package com.ravenherz.cse.dal.dto;

import com.ravenherz.cse.constants.MongoCollections;
import com.ravenherz.cse.dal.dto.basic.UrlTemplateData;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Objects;

@Document(collection = MongoCollections.DATABASE_URL_TEMPLATES)
public final class UrlTemplateEntity extends BasicEntity implements Serializable {

    @Indexed(unique = true)
    private String urlTemplateId;
    private UrlTemplateData urlTemplateData;

    public UrlTemplateEntity() {
    }

    public UrlTemplateEntity(String urlTemplateId, UrlTemplateData urlTemplateData, AccountEntity creator) {
        super(null, creator == null ? null : creator.getId());
        this.urlTemplateId = urlTemplateId;
        this.urlTemplateData = urlTemplateData;
    }

    public String getUrlTemplateId() {
        return urlTemplateId;
    }

    public void setUrlTemplateId(String urlTemplateId) {
        this.urlTemplateId = urlTemplateId;
    }

    public UrlTemplateData getUrlTemplateData() {
        return urlTemplateData;
    }

    public void setUrlTemplateData(UrlTemplateData urlTemplateData) {
        this.urlTemplateData = urlTemplateData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        UrlTemplateEntity that = (UrlTemplateEntity) o;
        return Objects.equals(urlTemplateId, that.urlTemplateId)
                && Objects.equals(urlTemplateData, that.urlTemplateData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), urlTemplateId, urlTemplateData);
    }
}
