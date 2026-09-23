package com.ravenherz.cse.dal;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.basic.PageData;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class ReferenceHydrator {

    private static final ThreadLocal<Boolean> LOADING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final MongoTemplate mongo;

    public ReferenceHydrator(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public void hydrate(Object entity) {
        if (entity == null || LOADING.get()) {
            return;
        }
        LOADING.set(Boolean.TRUE);
        try {
            if (entity instanceof ItemEntity item) {
                item.attachRefCategory(load(item.getRefCategoryId(), CategoryEntity.class));
                hydratePage(item.getPageData());
            } else if (entity instanceof ResourceEntity resource) {
                resource.attachRefResourceGroup(load(resource.refResourceGroupObjectId(),
                        ResourceGroupEntity.class));
            } else if (entity instanceof ResourceGroupEntity group) {
                group.attachRefParentGroup(load(group.refParentGroupObjectId(),
                        ResourceGroupEntity.class));
            }
        } finally {
            LOADING.set(Boolean.FALSE);
        }
    }

    private void hydratePage(PageData page) {
        if (page == null || page.getComments() == null) {
            return;
        }
        for (PageData.Comment comment : page.getComments()) {
            if (comment != null) {
                comment.attachAuthor(load(comment.getAuthorId(), AccountEntity.class));
            }
        }
    }

    private <T> T load(ObjectId id, Class<T> type) {
        if (id == null) {
            return null;
        }
        return mongo.findById(id, type);
    }
}
