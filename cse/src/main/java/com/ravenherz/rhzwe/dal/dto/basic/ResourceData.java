package com.ravenherz.rhzwe.dal.dto.basic;

import com.ravenherz.rhzwe.dal.dto.AccountEntity;
import com.ravenherz.rhzwe.dal.dto.basic.enums.ResourceType;
import com.ravenherz.rhzwe.util.StringUtils;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Indexed;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
public final class ResourceData {

    private static final int CHUNK_SIZE = 7000000;

    private ResourceType type;
    private long sizeInBytes;
    @Indexed
    private String pathPublic;
    @Indexed
    private String pathProtected;
    private String contentRaw;
    private String contentPreview;
    private boolean largeFile;
    private List<ObjectId> dataChunkIds;
    private Map<String, String> metadata;

    public ResourceData() {
    }

    public ResourceData(byte[] content, String uniqueName, String fileName,
            AccountEntity accessor) {
        if (accessor == null) {
            throw new Error("Broken logic. It couldn't happen");
        }
        this.sizeInBytes = content.length;
        String base64Content = Base64.getEncoder().encodeToString(content);
        if (base64Content.length() > CHUNK_SIZE) {
            this.largeFile = true;
            this.dataChunkIds = new ArrayList<>();
            this.contentRaw = null;
        } else {
            this.largeFile = false;
            this.contentRaw = base64Content;
        }
        this.type = ResourceType.getByFileName(fileName);
        this.pathPublic = String
                .format("/%s/res/%s/%s", accessor.getAccountData().getLogin(), type.getPath(),
                        uniqueName);
        this.pathProtected = StringUtils.generateRandomPath(uniqueName);
    }

    public boolean isLargeFile() {
        return largeFile;
    }

    public void setLargeFile(boolean largeFile) {
        this.largeFile = largeFile;
    }

    public List<ObjectId> getDataChunkIds() {
        return dataChunkIds;
    }

    public void setDataChunkIds(List<ObjectId> dataChunkIds) {
        this.dataChunkIds = dataChunkIds;
    }

    public void addDataChunkId(ObjectId chunkId) {
        if (this.dataChunkIds == null) {
            this.dataChunkIds = new ArrayList<>();
        }
        this.dataChunkIds.add(chunkId);
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
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
