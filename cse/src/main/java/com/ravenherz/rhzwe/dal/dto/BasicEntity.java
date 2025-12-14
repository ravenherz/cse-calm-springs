package com.ravenherz.rhzwe.dal.dto;

import com.mongodb.lang.Nullable;
import com.ravenherz.rhzwe.dal.EntityUtils;
import com.ravenherz.rhzwe.dal.dto.basic.HistoryData;
import com.ravenherz.rhzwe.dal.dto.basic.SecurityData;
import com.ravenherz.rhzwe.dal.dto.basic.enums.AccessType;
import com.ravenherz.rhzwe.dal.dto.basic.enums.SecurityLevel;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public abstract class BasicEntity implements Serializable, EntityUtils {

    private SecurityData securityData;

    private HistoryData historyData;

    private String entityVersion;

    @Id
    ObjectId id;

    public BasicEntity() {
    }

    public BasicEntity(String entityVersion) {
        this(entityVersion, null, null);
    }

    public BasicEntity(String entityVersion, @Nullable Map<AccessType, SecurityLevel> accessSettings,
            @Nullable AccountEntity creator) {
        this.securityData = accessSettings == null ? getDefaultSecurityData()
                : new SecurityData(accessSettings);
        this.historyData = creator == null ? new HistoryData() : new HistoryData(creator);
        this.entityVersion = entityVersion;
    }

    private HistoryData getDefaultCommonData() {
        HistoryData historyData = new HistoryData();
        return new HistoryData();
    }

    private SecurityData getDefaultSecurityData() {
        return new SecurityData();
    }

    public SecurityData getSecurityData() {
        return securityData;
    }

    public void setSecurityData(SecurityData securityData) {
        this.securityData = securityData;
    }

    public HistoryData getHistoryData() {
        return historyData;
    }

    public void setHistoryData(HistoryData historyData) {
        this.historyData = historyData;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BasicEntity that = (BasicEntity) o;
        return Objects.equals(securityData, that.securityData) &&
                Objects.equals(historyData, that.historyData) &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {

        return Objects.hash(securityData, historyData, id);
    }

    public String getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(String entityVersion) {
        this.entityVersion = entityVersion;
    }
}
