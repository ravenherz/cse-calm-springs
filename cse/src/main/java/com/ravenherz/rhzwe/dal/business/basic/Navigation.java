package com.ravenherz.rhzwe.dal.business.basic;

import java.util.ArrayList;
import java.util.LinkedList;

public class Navigation {

    private final LinkedList<NavigationUnit> tree;

    public Navigation() {
        tree = new LinkedList<>();
    }

    public void addItem(NavigationUnit newItem) {
        tree.add(newItem);
    }

    public ArrayList<NavigationUnit> getTree() {
        return new ArrayList<>(tree);
    }
}
