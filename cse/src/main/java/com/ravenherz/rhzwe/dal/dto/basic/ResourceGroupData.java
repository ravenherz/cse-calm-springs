package com.ravenherz.rhzwe.dal.dto.basic;

import dev.morphia.annotations.Entity;

@Entity
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