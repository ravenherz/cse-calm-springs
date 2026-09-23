package com.ravenherz.cse.dal.dto;

import com.ravenherz.cse.constants.MongoCollections;
import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.MediaIds;
import com.ravenherz.cse.dal.dto.basic.ResourceGroupData;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = MongoCollections.DATABASE_RESOURCE_GROUPS)
public final class ResourceGroupEntity extends BasicEntity {

    ResourceGroupData resourceGroupData;

    @Field("refParentGroup")
    private ObjectId refParentGroupId;
    @Transient
    private ResourceGroupEntity refParentGroup;

    public ResourceGroupEntity() {
    }

    public ResourceGroupEntity(ResourceGroupData resourceGroupData, EntityId creatorId) {
        super(null, creatorId);
        this.resourceGroupData = resourceGroupData;
    }

    public ResourceGroupData getResourceGroupData() {
        return resourceGroupData;
    }

    public void setResourceGroupData(ResourceGroupData resourceGroupData) {
        this.resourceGroupData = resourceGroupData;
    }

    public ResourceGroupEntity getRefParentGroup() {
        return refParentGroup;
    }

    public void setRefParentGroup(ResourceGroupEntity refParentGroup) {
        this.refParentGroup = refParentGroup;
        this.refParentGroupId = refParentGroup == null ? null : MediaIds.objectId(refParentGroup.getId());
    }

    public void attachRefParentGroup(ResourceGroupEntity refParentGroup) {
        this.refParentGroup = refParentGroup;
    }

    public ObjectId refParentGroupObjectId() {
        return refParentGroupId != null ? refParentGroupId
                : (refParentGroup == null ? null : MediaIds.objectId(refParentGroup.getId()));
    }

    public String getRefParentGroupId() {
        ObjectId id = refParentGroupObjectId();
        return id != null ? id.toString() : null;
    }
}