package com.ravenherz.rhzwe.dal.dto;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.dal.dto.basic.ResourceGroupData;
import dev.morphia.annotations.Entity;

@Entity(Strings.DATABASE_RESOURCE_GROUPS)
public final class ResourceGroupEntity extends BasicEntity {

    ResourceGroupData resourceGroupData;

    public ResourceGroupEntity() {
    }

    public ResourceGroupEntity(ResourceGroupData resourceGroupData, AccountEntity creator) {
        super("0.1.0", null, creator);
        this.resourceGroupData = resourceGroupData;
    }

    public ResourceGroupData getResourceGroupData() {
        return resourceGroupData;
    }

    public void setResourceGroupData(ResourceGroupData resourceGroupData) {
        this.resourceGroupData = resourceGroupData;
    }
}