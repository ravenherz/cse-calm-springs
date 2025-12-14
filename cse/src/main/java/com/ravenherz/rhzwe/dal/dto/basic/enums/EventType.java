package com.ravenherz.rhzwe.dal.dto.basic.enums;

public enum EventType {
    ENTITY_CREATED(0),
    ENTITY_EDITED(1),
    ENTITY_DELETED(2),
    ENTITY_RESTORED(3);

    private int value;

    EventType(int securityInt) {
        value = securityInt;
    }

    public int getValue() {
        return value;
    }
}
