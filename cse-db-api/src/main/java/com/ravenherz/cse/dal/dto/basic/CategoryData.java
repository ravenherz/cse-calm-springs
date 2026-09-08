package com.ravenherz.cse.dal.dto.basic;

import org.springframework.data.mongodb.core.index.Indexed;

import java.io.Serializable;
import java.util.Objects;

public final class CategoryData implements Serializable {

    @Indexed
    private String itemName;
    @Indexed
    private String navigationTitle;
    @Indexed
    private String navigationDescription;
    @org.springframework.data.mongodb.core.mapping.Field("isVisible")
    private boolean isVisible;
    @org.springframework.data.mongodb.core.mapping.Field("isActive")
    private boolean isActive;
    private int displayCount;
    private int displayPriority;

    public CategoryData() {

    }

    public CategoryData(String itemName, String navigationTitle, String navigationDescription,
            boolean isVisible, boolean isActive) {
        this.itemName = itemName;
        this.navigationTitle = navigationTitle;
        this.navigationDescription = navigationDescription;
        this.isVisible = isVisible;
        this.isActive = isActive;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getNavigationTitle() {
        return navigationTitle;
    }

    public void setNavigationTitle(String navigationTitle) {
        this.navigationTitle = navigationTitle;
    }

    public String getNavigationDescription() {
        return navigationDescription;
    }

    public void setNavigationDescription(String navigationDescription) {
        this.navigationDescription = navigationDescription;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getDisplayCount() {
        return displayCount;
    }

    public void setDisplayCount(int displayCount) {
        this.displayCount = displayCount;
    }

    public int getDisplayPriority() {
        return displayPriority;
    }

    public void setDisplayPriority(int displayPriority) {
        this.displayPriority = displayPriority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CategoryData that = (CategoryData) o;
        return isVisible == that.isVisible &&
                isActive == that.isActive &&
                displayCount == that.displayCount &&
                displayPriority == that.displayPriority &&
                Objects.equals(itemName, that.itemName) &&
                Objects.equals(navigationTitle, that.navigationTitle) &&
                Objects.equals(navigationDescription, that.navigationDescription);
    }

    @Override
    public int hashCode() {

        return Objects.hash(itemName, navigationTitle, navigationDescription, isVisible, isActive,
                displayCount, displayPriority);
    }

    @Override
    public String toString() {
        return "CategoryData{" +
                "itemName='" + itemName + '\'' +
                ", navigationTitle='" + navigationTitle + '\'' +
                ", navigationDescription='" + navigationDescription + '\'' +
                ", isVisible=" + isVisible +
                ", isLoginable=" + isActive +
                ", displayCount=" + displayCount +
                ", displayPriority=" + displayPriority +
                '}';
    }
}
