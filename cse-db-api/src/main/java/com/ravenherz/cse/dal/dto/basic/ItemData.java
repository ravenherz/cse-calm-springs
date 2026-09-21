package com.ravenherz.cse.dal.dto.basic;

public abstract class ItemData {

    protected String subHeader;

    public ItemData() {};

    public ItemData(String subHeader) {
        this.subHeader = subHeader;
    }

    public String getSubHeader() {
        return subHeader;
    }

    public void setSubHeader(String subHeader) {
        this.subHeader = subHeader;
    }
}
