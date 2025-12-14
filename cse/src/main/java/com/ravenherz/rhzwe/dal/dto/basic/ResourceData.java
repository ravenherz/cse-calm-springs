package com.ravenherz.rhzwe.dal.dto.basic;

import com.ravenherz.rhzwe.dal.dto.AccountEntity;
import com.ravenherz.rhzwe.dal.dto.basic.enums.ResourceType;
import com.ravenherz.rhzwe.util.StringUtils;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Indexed;

import java.util.Base64;

@Entity
public final class ResourceData {

    private ResourceType type;
    private long sizeInBytes;
    @Indexed
    private String pathPublic;
    @Indexed
    private String pathProtected;
    private String contentRaw;
    private String contentPreview; // todo make preview logic for image/audio

    public ResourceData() {
    }

    public ResourceData(byte[] content, String uniqueName, String fileName,
            AccountEntity accessor) {
        if (accessor == null) {
            throw new Error("Broken logic. It couldn't happen");
        }
        this.sizeInBytes = content.length;
        this.contentRaw = Base64.getEncoder().encodeToString(content);
        this.type = ResourceType.getByFileName(fileName);
        this.pathPublic = String
                .format("/%s/res/%s/%s", accessor.getAccountData().getLogin(), type.getPath(),
                        uniqueName);
        this.pathProtected = StringUtils.generateRandomPath(uniqueName);
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public void setSizeInBytes(long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public String getPathPublic() {
        return pathPublic;
    }

    public void setPathPublic(String pathPublic) {
        this.pathPublic = pathPublic;
    }

    public String getPathProtected() {
        return pathProtected;
    }

    public void setPathProtected(String pathProtected) {
        this.pathProtected = pathProtected;
    }

    public String getContentRaw() {
        return contentRaw;
    }

    public void setContentRaw(String contentRaw) {
        this.contentRaw = contentRaw;
    }

    public String getContentPreview() {
        return contentPreview;
    }

    public void setContentPreview(String contentPreview) {
        this.contentPreview = contentPreview;
    }
}
