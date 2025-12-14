package com.ravenherz.rhzwe.dal.dto.basic;

import dev.morphia.annotations.Entity;

@Entity
public abstract class ItemData {

    protected String title;
    protected String subHeader;

    public ItemData() {};

    public ItemData(String title, String subHeader) {
        this.title = title;
        this.subHeader = subHeader;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubHeader() {
        return subHeader;
    }

    public void setSubHeader(String subHeader) {
        this.subHeader = subHeader;
    }
}
