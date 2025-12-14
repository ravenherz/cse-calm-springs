package com.ravenherz.rhzwe.dal;

import com.ravenherz.rhzwe.dal.dto.AccountEntity;
import com.ravenherz.rhzwe.dal.dto.BasicEntity;
import com.ravenherz.rhzwe.dal.dto.basic.Event;
import com.ravenherz.rhzwe.dal.dto.basic.enums.AccessType;
import com.ravenherz.rhzwe.dal.dto.basic.enums.EventType;
import com.ravenherz.rhzwe.dal.dto.basic.enums.SecurityLevel;

import java.util.Arrays;

public interface EntityUtils {

    static boolean isAccessible (BasicEntity obtainable, AccessType accessType, AccountEntity accessor) {
        if (obtainable == null || accessType == null) {
            return false;
        }

        if (accessor != null && accessor.getId().equals(obtainable.getId())) {
            return true;
        }

        AccountEntity owner = getOwner(obtainable);

        if (owner == null) {
            return switch (accessType) {
                case ACCESS_READ -> true;
                case ACCESS_EDIT -> false;
                case ACCESS_DELETE -> false;
            };
        }

        SecurityLevel relativeSecurityLevel = accessor == null ? SecurityLevel.GUEST :
                owner.getId().equals(accessor.getId()) ? SecurityLevel.OWNER :
                (accessor.getAccountData().getLevel());
        return (obtainable.getSecurityData().getAccessSettings().get(accessType).getIntLevel()
                <= relativeSecurityLevel.getIntLevel());
    }

    static AccountEntity getOwner(BasicEntity entity) {
        return getLastEventByType(EventType.ENTITY_CREATED, entity).getOwner();
    }

    static Event getLastEventByType(EventType eventType, BasicEntity entity) {
        return Arrays.stream(entity.getHistoryData().getEvents()).filter(event ->
            event.getEventType().equals(eventType)).findFirst().orElse(null);
    }
}
