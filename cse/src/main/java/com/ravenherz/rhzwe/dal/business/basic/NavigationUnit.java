package com.ravenherz.rhzwe.dal.business.basic;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

public class NavigationUnit {

    public enum NavigationUnitType {
        CATEGORY(0),
        PAGE(1),
        ALBUM(2);

        private final int value;

        NavigationUnitType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private final NavigationUnitType type;
    private final String name;
    private final String title;
    private final String description;
    private final List<NavigationUnit> children;
    private final LocalDateTime dateTimeCreated;

    public NavigationUnit(NavigationUnitType type, String name, String title, String description,
            List<NavigationUnit> children, LocalDateTime dateTimeCreated) {
        this.type = type;
        this.name = name;
        this.title = title;
        this.description = description;
        this.children = children == null ? new LinkedList<>() : children;
        this.dateTimeCreated = dateTimeCreated;

    }

    public void addChild(NavigationUnit child) {
        if (type.equals(NavigationUnitType.CATEGORY)) {
            children.add(child);
        }
    }

    public void addChildren(List<NavigationUnit> children) {
        children.forEach(this::addChild);
    }

    public NavigationUnitType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<NavigationUnit> getChildren() {
        return children;
    }

    public LocalDateTime getDateTimeCreated() {
        return dateTimeCreated;
    }
}
