package com.ravenherz.cse.dal.dto;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.EntityVersions;
import com.ravenherz.cse.dal.dto.basic.AccessRule;
import com.ravenherz.cse.dal.dto.basic.Event;
import com.ravenherz.cse.dal.dto.basic.HistoryData;
import com.ravenherz.cse.dal.dto.basic.SecurityData;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.dto.basic.enums.EventType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public abstract class BasicEntity implements Serializable {

    private SecurityData securityData;

    private HistoryData historyData;

    private String entityVersion;

    EntityId id;

    public BasicEntity() {
    }

    public BasicEntity(Map<AccessType, AccessRule> accessSettings, EntityId creatorId) {
        this.securityData = accessSettings == null ? getDefaultSecurityData()
                : new SecurityData(accessSettings);
        this.historyData = creatorId == null ? new HistoryData() : new HistoryData(creatorId);
        this.entityVersion = EntityVersions.current();
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

    public EntityId getId() {
        return id;
    }

    public void setId(EntityId id) {
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

    public LocalDateTime selectCreationLocalDateTime() {
        return Arrays.stream(this.getHistoryData().getEvents())
                .filter(event -> EventType.ENTITY_CREATED.equals(event.getEventType()))
                .map(Event::getLocalDateTime)
                .findFirst().orElse(LocalDateTime.now());
    }

    public void setCreationLocalDateTime(LocalDateTime dateTime) {
        if (dateTime == null || historyData == null || historyData.getEvents() == null) {
            return;
        }
        for (Event event : historyData.getEvents()) {
            if (EventType.ENTITY_CREATED.equals(event.getEventType())) {
                event.setLocalDateTime(dateTime);
                return;
            }
        }
    }
}
