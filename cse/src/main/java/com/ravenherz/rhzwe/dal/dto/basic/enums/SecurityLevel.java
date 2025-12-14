package com.ravenherz.rhzwe.dal.dto.basic.enums;

public enum SecurityLevel {
    GUEST(0),
    INACTIVE_USER(1),
    ACTIVE_USER(2),
    PRIVILEGIED_USER(3),
    GUIDE(4),
    OPERATOR(300),
    MODERATOR(400),
    ADMIN(500),
    OWNER(666);

    private final int intLevel;

    SecurityLevel(final int newValue) {
        intLevel = newValue;
    }

    public int getIntLevel() {
        return intLevel;
    }
}
