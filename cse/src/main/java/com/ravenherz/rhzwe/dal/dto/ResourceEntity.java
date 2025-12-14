package com.ravenherz.rhzwe.dal.dto;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.dal.dto.basic.ResourceData;
import dev.morphia.annotations.Entity;

import java.util.Base64;
import java.util.Objects;

@Entity(Strings.DATABASE_RESOURCES)
public final class ResourceEntity extends BasicEntity {

    ResourceData resourceData;

    public ResourceEntity () {

    }

    public ResourceEntity (ResourceData resourceData,
            AccountEntity creator) {
        super("0.1.0", null, creator);
        this.resourceData = resourceData;
    }

    public ResourceData getResourceData() {
        return resourceData;
    }

    public void setResourceData(ResourceData resourceData) {
        this.resourceData = resourceData;
    }

    public byte[] getRawBytes() {
        return Base64.getDecoder().decode(resourceData.getContentRaw());
    }

    public byte[] getPreviewBytes() {
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
        return Objects.equals(getResourceData(), that.getResourceData());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getResourceData());
    }

    @Override
    public String toString() {
        return "ResourceEntity{" +
                "resourceData=" + resourceData +
                ", id=" + id +
                '}';
    }
}
