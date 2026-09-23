package com.ravenherz.cse.dal.dto;

import com.ravenherz.cse.constants.MongoCollections;
import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.MediaIds;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Base64;
import java.util.Objects;

@Document(collection = MongoCollections.DATABASE_RESOURCES)
public final class ResourceEntity extends BasicEntity {

    ResourceData resourceData;
    ResourceData previewData;

    @Field("refResourceGroup")
    private ObjectId refResourceGroupId;
    @Transient
    private ResourceGroupEntity refResourceGroup;

    public ResourceEntity () {

    }

    public ResourceEntity(ResourceData resourceData, EntityId creatorId) {
        super(null, creatorId);
        this.resourceData = resourceData;
    }

    public ResourceData getResourceData() {
        return resourceData;
    }

    public void setResourceData(ResourceData resourceData) {
        this.resourceData = resourceData;
    }

    public ResourceData getPreviewData() {
        return previewData;
    }

    public void setPreviewData(ResourceData previewData) {
        this.previewData = previewData;
    }

    public String displayPublicPath() {
        if (previewData != null && previewData.getPathPublic() != null
                && !previewData.getPathPublic().isBlank()) {
            return previewData.getPathPublic();
        }
        return resourceData == null ? null : resourceData.getPathPublic();
    }

    public ResourceData dataForPublicPath(String publicPath) {
        if (publicPath == null) {
            return resourceData;
        }
        if (previewData != null && publicPath.equals(previewData.getPathPublic())) {
            return previewData;
        }
        return resourceData;
    }

    public ResourceGroupEntity getRefResourceGroup() {
        return refResourceGroup;
    }

    public void setRefResourceGroup(ResourceGroupEntity refResourceGroup) {
        this.refResourceGroup = refResourceGroup;
        this.refResourceGroupId = refResourceGroup == null ? null : MediaIds.objectId(refResourceGroup.getId());
    }

    public void attachRefResourceGroup(ResourceGroupEntity refResourceGroup) {
        this.refResourceGroup = refResourceGroup;
    }

    public ObjectId refResourceGroupObjectId() {
        return refResourceGroupId != null ? refResourceGroupId
                : (refResourceGroup == null ? null : MediaIds.objectId(refResourceGroup.getId()));
    }

    public String getRefResourceGroupId() {
        ObjectId id = refResourceGroupObjectId();
        return id != null ? id.toString() : null;
    }

    public byte[] getRawBytes() {
        return bytesOf(resourceData);
    }

    public byte[] getRawBytes(ResourceData data) {
        return bytesOf(data);
    }

    public static byte[] bytesOf(ResourceData data) {
        if (data == null) {
            return null;
        }
        if (data.isLargeFile() && data.getDataChunkIds() != null && !data.getDataChunkIds().isEmpty()) {
            return null;
        }
        String contentRaw = data.getContentRaw();
        if (contentRaw == null || contentRaw.isEmpty()) {
            return null;
        }
        return Base64.getDecoder().decode(contentRaw);
    }

    public byte[] getPreviewBytes() {
        if (previewData != null) {
            byte[] preview = bytesOf(previewData);
            if (preview != null) {
                return preview;
            }
        }
        if (resourceData == null || resourceData.getContentPreview() == null
                || resourceData.getContentPreview().isEmpty()) {
            return null;
        }
        return Base64.getDecoder().decode(resourceData.getContentPreview());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceEntity)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ResourceEntity that = (ResourceEntity) o;
        return Objects.equals(getResourceData(), that.getResourceData())
                && Objects.equals(getPreviewData(), that.getPreviewData());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getResourceData(), getPreviewData());
    }

    @Override
    public String toString() {
        return "ResourceEntity{" +
                "resourceData=" + resourceData +
                ", previewData=" + previewData +
                ", id=" + id +
                '}';
    }
}
