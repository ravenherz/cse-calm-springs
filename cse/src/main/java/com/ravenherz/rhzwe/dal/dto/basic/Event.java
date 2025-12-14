package com.ravenherz.rhzwe.dal.dto.basic;

import com.ravenherz.rhzwe.dal.dto.AccountEntity;
import com.ravenherz.rhzwe.dal.dto.basic.enums.EventType;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Reference;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public final class Event implements Serializable {

    private EventType eventType;
    private LocalDateTime localDateTime;
    @Reference
    private AccountEntity owner;

    public Event() {

    }

    public Event(EventType eventType, LocalDateTime localDateTime, AccountEntity owner) {
        this.eventType = eventType;
        this.localDateTime = localDateTime;
        this.owner = owner;
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

    public AccountEntity getOwner() {
        return owner;
    }

    public void setOwner(AccountEntity owner) {
        this.owner = owner;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
}
