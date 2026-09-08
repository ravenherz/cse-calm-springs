package com.ravenherz.cse.dal.dto.basic;

import com.ravenherz.cse.dal.dto.basic.enums.EventType;
import com.ravenherz.cse.dal.dto.AccountEntity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;

public final class HistoryData implements Serializable {
    
    private Event[] events;

    public HistoryData() {
        events = new Event[0];
    }

    public HistoryData(AccountEntity account) {
        events = new Event[1];
        events[0] = new Event(EventType.ENTITY_CREATED, LocalDateTime.now(), account);
    }

    public Event[] getEvents() {
        return events;
    }

    public void setEvents(Event[] events) {
        this.events = events;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HistoryData that = (HistoryData) o;
        return Arrays.equals(events, that.events);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(events);
    }

    @Override
    public String toString() {
        return "HistoryData{" +
                "events=" + Arrays.toString(events) +
                '}';
    }
}
