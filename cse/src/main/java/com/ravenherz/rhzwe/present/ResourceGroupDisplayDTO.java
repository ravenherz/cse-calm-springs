package com.ravenherz.rhzwe.present;

import com.ravenherz.rhzwe.dal.dto.ResourceEntity;
import java.util.List;

public class ResourceGroupDisplayDTO {
    private String id;
    private String humanReadableId;
    private List<ResourceEntity> resources;
    private long totalSize;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHumanReadableId() { return humanReadableId; }
    public void setHumanReadableId(String humanReadableId) { this.humanReadableId = humanReadableId; }

    public List<ResourceEntity> getResources() { return resources; }
    public void setResources(List<ResourceEntity> resources) { this.resources = resources; }

    public long getTotalSize() { return totalSize; }
    public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
}