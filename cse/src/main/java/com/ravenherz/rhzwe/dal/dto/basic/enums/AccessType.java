package com.ravenherz.rhzwe.dal.dto.basic.enums;

public enum AccessType {
    ACCESS_READ(0),
    ACCESS_EDIT(1),
    ACCESS_DELETE(2);

    private int value;

    AccessType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
