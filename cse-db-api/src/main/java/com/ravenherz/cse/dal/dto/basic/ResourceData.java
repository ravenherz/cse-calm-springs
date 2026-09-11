package com.ravenherz.cse.dal.dto.basic;

import com.ravenherz.cse.dal.DalStrings;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import com.ravenherz.cse.dal.dto.AccountEntity;
import org.springframework.data.mongodb.core.index.Indexed;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ResourceData {

    private static final int CHUNK_SIZE = 7000000;

    private ResourceType type;
    private long sizeInBytes;
    @Indexed
    private String pathPublic;
    @Indexed
    private String pathProtected;
    private String imageDescription;
    private String contentRaw;
    private String contentPreview;
    private boolean largeFile;
    private List<ObjectId> dataChunkIds;
    private Map<String, String> metadata;
    private byte[] waveform;

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
        this.pathProtected = DalStrings.generateRandomPath(uniqueName);
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
        if (metadata == null) {
            this.metadata = null;
        } else {
            this.metadata = new LinkedHashMap<>(metadata);
        }
    }

    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new LinkedHashMap<>();
        }
        this.metadata.put(key, value);
    }

    public byte[] getWaveform() {
        return waveform;
    }

    public void setWaveform(byte[] waveform) {
        this.waveform = waveform;
    }

    public String getWaveformBase64() {
        if (waveform == null || waveform.length == 0) {
            return null;
        }
        return Base64.getEncoder().encodeToString(waveform);
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

    public String getSizeLabel() {
        return DalStrings.formatByteSize(sizeInBytes);
    }

    public String getFileName() {
        if (pathPublic == null || pathPublic.isEmpty()) {
            return "";
        }
        int slash = pathPublic.lastIndexOf('/');
        return slash >= 0 ? pathPublic.substring(slash + 1) : pathPublic;
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

    public String getImageDescription() {
        return imageDescription;
    }

    public void setImageDescription(String imageDescription) {
        if (imageDescription == null || imageDescription.isBlank()) {
            this.imageDescription = null;
        } else {
            this.imageDescription = imageDescription.trim();
        }
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
