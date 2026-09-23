package com.ravenherz.cse.dal.dto.basic;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dto.basic.enums.EventType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public final class Event implements Serializable {

    private EventType eventType;
    private LocalDateTime localDateTime;
    private EntityId owner;

    public Event() {
    }

    public Event(EventType eventType) {
    }

    public Event(EventType eventType, LocalDateTime localDateTime, EntityId ownerId) {
        this.eventType = eventType;
        this.localDateTime = localDateTime;
        this.owner = ownerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Event event = (Event) o;
        return Objects.equals(localDateTime, event.localDateTime) &&
                Objects.equals(owner, event.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localDateTime, owner);
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public EntityId getOwnerId() {
        return owner;
    }

    public void setOwnerId(EntityId ownerId) {
        this.owner = ownerId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
}
