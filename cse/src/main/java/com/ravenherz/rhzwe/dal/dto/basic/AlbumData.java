package com.ravenherz.rhzwe.dal.dto.basic;

import dev.morphia.annotations.Entity;
import org.bson.types.ObjectId;

import java.io.Serializable;
import java.util.List;

@Entity
public class AlbumData extends ItemData implements Serializable {

    private List<ObjectId> resourcesIds;

    public List<ObjectId> getResourcesIds() {
        return resourcesIds;
    }

    public void setResourcesIds(List<ObjectId> resourcesIds) {
        this.resourcesIds = resourcesIds;
    }
}
