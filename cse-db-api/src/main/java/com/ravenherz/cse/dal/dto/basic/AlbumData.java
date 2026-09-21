package com.ravenherz.cse.dal.dto.basic;

import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.List;

public class AlbumData extends ItemData implements Serializable {

    private String header;
    private String description;
    private List<String> tags;
    @Field("refResourceGroup")
    private ObjectId refResourceGroupId;
    @Transient
    private ResourceGroupEntity refResourceGroup;

    public AlbumData() {
    }

    public AlbumData(String header, String subHeader, String description) {
        super(subHeader);
        this.header = header;
        this.description = description;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public ObjectId getRefResourceGroupId() {
        return refResourceGroupId != null ? refResourceGroupId
                : (refResourceGroup == null ? null : refResourceGroup.getId());
    }

    public ResourceGroupEntity getRefResourceGroup() {
        return refResourceGroup;
    }

    public void setRefResourceGroup(ResourceGroupEntity refResourceGroup) {
        this.refResourceGroup = refResourceGroup;
        this.refResourceGroupId = refResourceGroup == null ? null : refResourceGroup.getId();
    }

    public void attachRefResourceGroup(ResourceGroupEntity refResourceGroup) {
        this.refResourceGroup = refResourceGroup;
    }
}
