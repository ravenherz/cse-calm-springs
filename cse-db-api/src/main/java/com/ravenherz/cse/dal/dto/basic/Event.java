package com.ravenherz.cse.dal.dto.basic;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.enums.EventType;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public final class Event implements Serializable {

    private EventType eventType;
    private LocalDateTime localDateTime;
    @Field("owner")
    private ObjectId ownerId;
    @Transient
    private AccountEntity owner;

    public Event() {
    }

    public Event(EventType eventType) {
    }

    public Event(EventType eventType, LocalDateTime localDateTime, AccountEntity owner) {
        this.eventType = eventType;
        this.localDateTime = localDateTime;
        setOwner(owner);
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
                Objects.equals(ownerId, event.ownerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localDateTime, ownerId);
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public ObjectId getOwnerId() {
        return ownerId != null ? ownerId : (owner == null ? null : owner.getId());
    }

    public AccountEntity getOwner() {
        return owner;
    }

    public void setOwner(AccountEntity owner) {
        this.owner = owner;
        this.ownerId = owner == null ? null : owner.getId();
    }

    public void attachOwner(AccountEntity owner) {
        this.owner = owner;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
}
