package com.ravenherz.cse.dal.dto.basic;

public class ResourceGroupData {
    private String humanReadableId;

    public ResourceGroupData() {
    }

    public ResourceGroupData(String humanReadableId) {
        this.humanReadableId = humanReadableId;
    }

    public String getHumanReadableId() {
        return humanReadableId;
    }

    public void setHumanReadableId(String humanReadableId) {
        this.humanReadableId = humanReadableId;
    }
}
