package com.ravenherz.rhzwe.dal.business.basic;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Navigation {

    private final List<NavigationUnit> tree;

    public Navigation() {
        tree = new LinkedList<>();
    }

    public void addItem(NavigationUnit newItem) {
        tree.add(newItem);
    }

    public void addItems(List<NavigationUnit> items) {
        tree.addAll(items);
    }

    public ArrayList<NavigationUnit> getTree() {
        return new ArrayList<>(tree);
    }
}
