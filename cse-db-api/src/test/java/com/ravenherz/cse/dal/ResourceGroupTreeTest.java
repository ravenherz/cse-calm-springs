package com.ravenherz.cse.dal;

import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.basic.ResourceGroupData;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceGroupTreeTest {

    @Test
    void missingParentIsRootDepthOne() {
        ResourceGroupEntity travel = group("Travel");
        assertEquals(1, ResourceGroupTree.depthOf(List.of(travel), travel.getId()));
        assertNull(ResourceGroupTree.refuseCreate(List.of(travel), "Studio", null));
    }

    @Test
    void siblingUniqueAllowsSameNameUnderDifferentParents() {
        ResourceGroupEntity travel = group("Travel");
        ResourceGroupEntity studio = group("Studio");
        ResourceGroupEntity y2024 = child("2024", travel);
        assertTrue(ResourceGroupTree.nameTakenAmongSiblings(
                List.of(travel, studio, y2024), "2024", travel.getId(), null));
        assertFalse(ResourceGroupTree.nameTakenAmongSiblings(
                List.of(travel, studio, y2024), "2024", studio.getId(), null));
        assertEquals("group_exists", ResourceGroupTree.refuseCreate(
                List.of(travel, studio, y2024), "2024", travel.getId()));
        assertNull(ResourceGroupTree.refuseCreate(
                List.of(travel, studio, y2024), "2024", studio.getId()));
    }

    @Test
    void cycleWhenParentIsSelfOrDescendant() {
        ResourceGroupEntity travel = group("Travel");
        ResourceGroupEntity y2024 = child("2024", travel);
        ResourceGroupEntity iceland = child("Iceland", y2024);
        List<ResourceGroupEntity> all = List.of(travel, y2024, iceland);
        assertTrue(ResourceGroupTree.wouldCycle(all, travel.getId(), travel.getId()));
        assertTrue(ResourceGroupTree.wouldCycle(all, travel.getId(), iceland.getId()));
        assertEquals("cycle", ResourceGroupTree.refuseMove(all, travel, iceland.getId()));
        assertNull(ResourceGroupTree.refuseMove(all, iceland, travel.getId()));
    }

    @Test
    void allowsNestingBeyondThreeLevels() {
        ResourceGroupEntity travel = group("Travel");
        ResourceGroupEntity y2024 = child("2024", travel);
        ResourceGroupEntity iceland = child("Iceland", y2024);
        List<ResourceGroupEntity> all = List.of(travel, y2024, iceland);
        assertEquals(3, ResourceGroupTree.depthOf(all, iceland.getId()));
        assertNull(ResourceGroupTree.refuseCreate(all, "Reykjavik", iceland.getId()));
        ResourceGroupEntity extra = group("Extra");
        ResourceGroupEntity leaf = child("Leaf", extra);
        assertNull(ResourceGroupTree.refuseMove(
                List.of(travel, y2024, iceland, extra, leaf), extra, iceland.getId()));
    }

    @Test
    void defaultStaysARootAndCannotBeDeleted() {
        ResourceGroupEntity defaults = group("Default");
        ResourceGroupEntity travel = group("Travel");
        List<ResourceGroupEntity> all = List.of(defaults, travel);
        assertEquals("default_immovable", ResourceGroupTree.refuseMove(all, defaults, travel.getId()));
        assertEquals("default_immovable", ResourceGroupTree.refuseCreate(all, "Unsorted", travel.getId()));
        assertEquals("default_immovable", ResourceGroupTree.refuseCreate(all, "Default", travel.getId()));
        assertEquals("group_exists", ResourceGroupTree.refuseCreate(all, "Unsorted", null));
        assertEquals("default_immovable", ResourceGroupTree.refuseDelete(all, defaults));
        assertEquals("default_no_children", ResourceGroupTree.refuseCreate(all, "Shots", defaults.getId()));
        assertEquals("default_no_children", ResourceGroupTree.refuseMove(all, travel, defaults.getId()));
    }

    @Test
    void renameKeepsSiblingUniquenessAndLeavesDefaultAlone() {
        ResourceGroupEntity defaults = group("Default");
        ResourceGroupEntity travel = group("Travel");
        ResourceGroupEntity y2024 = child("2024", travel);
        ResourceGroupEntity studio = group("Studio");
        List<ResourceGroupEntity> all = List.of(defaults, travel, y2024, studio);
        assertNull(ResourceGroupTree.refuseRename(all, travel, "Travel"));
        assertNull(ResourceGroupTree.refuseRename(all, travel, "Archive"));
        assertEquals("group_exists", ResourceGroupTree.refuseRename(all, travel, "Studio"));
        assertEquals("default_rename", ResourceGroupTree.refuseRename(all, defaults, "Home"));
        assertEquals("default_immovable", ResourceGroupTree.refuseRename(all, y2024, "Unsorted"));
        assertEquals("default_immovable", ResourceGroupTree.refuseRename(all, y2024, "Default"));
        assertEquals("group_exists", ResourceGroupTree.refuseRename(all, studio, "Unsorted"));
        assertNull(ResourceGroupTree.refuseRename(all, y2024, "2025"));
    }

    @Test
    void deleteRefusesGroupsThatStillHaveChildren() {
        ResourceGroupEntity travel = group("Travel");
        ResourceGroupEntity y2024 = child("2024", travel);
        assertEquals("has_children",
                ResourceGroupTree.refuseDelete(List.of(travel, y2024), travel));
        assertNull(ResourceGroupTree.refuseDelete(List.of(travel, y2024), y2024));
    }

    @Test
    void errorMessageExplainsEachCode() {
        assertEquals("A group with that name already exists under the same parent.",
                ResourceGroupTree.errorMessage("group_exists"));
        assertEquals("The Unsorted group cannot be renamed.",
                ResourceGroupTree.errorMessage("default_rename"));
        assertEquals("The Unsorted group cannot contain folders.",
                ResourceGroupTree.errorMessage("default_no_children"));
        assertEquals("Move or delete child groups first.",
                ResourceGroupTree.errorMessage("has_children"));
        assertEquals("That name is not allowed.",
                ResourceGroupTree.errorMessage("invalid_name"));
        assertEquals("That name is already in use.",
                ResourceGroupTree.errorMessage("name_taken"));
        assertEquals("Could not update that item.",
                ResourceGroupTree.errorMessage("invalid_item"));
        assertNull(ResourceGroupTree.errorMessage("nope"));
    }

    private static ResourceGroupEntity group(String name) {
        ResourceGroupEntity entity = new ResourceGroupEntity(new ResourceGroupData(name), null);
        entity.setId(new ObjectId());
        return entity;
    }

    private static ResourceGroupEntity child(String name, ResourceGroupEntity parent) {
        ResourceGroupEntity entity = group(name);
        entity.setRefParentGroup(parent);
        return entity;
    }
}
