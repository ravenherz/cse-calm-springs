package com.ravenherz.cse.dal;

import com.ravenherz.cse.dal.dto.BasicEntity;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;

/**
 * Spring Data MongoDB only autogenerates {@code ObjectId}, {@code String}, and {@code BigInteger} ids.
 * {@link BasicEntity} uses {@link EntityId}, so a missing id has to be assigned before convert.
 */
public final class AssignEntityId implements BeforeConvertCallback<Object> {

    @Override
    public Object onBeforeConvert(Object entity, String collection) {
        if (entity instanceof BasicEntity basic && basic.getId() == null) {
            basic.setId(EntityId.generate());
        }
        return entity;
    }
}
