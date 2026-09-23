package com.ravenherz.cse.present;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.StoredIds;

import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.basic.EntityAccessConstants;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.ResourceGroupData;
import com.ravenherz.cse.dal.dto.basic.ResourceSizeHint;
import com.ravenherz.cse.dal.dto.basic.SecurityData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceGroupTreeViewTest {

    @Test
    void nestsChildrenAndBuildsPathLabels() {
        ResourceGroupEntity defaults = group("Default");
        ResourceGroupEntity travel = group("Travel");
        ResourceGroupEntity y2024 = child("2024", travel);
        ResourceGroupEntity iceland = child("Iceland", y2024);
        ResourceEntity onIceland = resource(iceland, 100);
        ResourceEntity onTravel = resource(travel, 50);
        ResourceEntity loose = resource(null, 10);

        ResourceGroupTreeView.Assembled tree = ResourceGroupTreeView.assemble(
                List.of(travel, iceland, defaults, y2024),
                List.of(onIceland, onTravel, loose));

        assertEquals("Unsorted", tree.roots().get(0).getHumanReadableId());
        assertEquals("Travel", tree.roots().get(1).getHumanReadableId());
        ResourceGroupDisplayDTO travelDto = tree.roots().get(1);
        assertEquals("2024", travelDto.getChildren().get(0).getHumanReadableId());
        assertEquals("Travel / 2024 / Iceland", travelDto.getChildren().get(0).getChildren().get(0).getPathLabel());
        assertEquals(2, travelDto.getDescendantFileCount());
        assertEquals(150, travelDto.getDescendantTotalSize());
        assertEquals(1, travelDto.getResources().size());
        assertEquals(1, travelDto.getOwnFileCount());
        assertEquals(1, travelDto.getTreeFiles().size());
        ResourceGroupDisplayDTO defaultDto = tree.roots().get(0);
        assertEquals(1, defaultDto.getTreeFiles().size());
        assertEquals(1, defaultDto.getResources().size());
        assertEquals(1, defaultDto.getOwnFileCount());
        assertEquals(10, defaultDto.getTotalSize());
        assertEquals(List.of("Unsorted", "Travel", "2024", "Iceland"),
                tree.assignableGroups().stream().map(ResourceGroupDisplayDTO::getHumanReadableId).toList());
        assertTrue(tree.ungrouped().getResources().isEmpty());
        assertTrue(tree.ungrouped().getTreeFiles().isEmpty());
        assertEquals(0, tree.ungrouped().getOwnFileCount());
        assertFalse(defaultDto.canRename());
        assertFalse(defaultDto.canMove());
        assertFalse(defaultDto.canDelete());
        assertFalse(defaultDto.canCreateChild());
        assertTrue(defaultDto.canAcceptFiles());
        assertTrue(defaultDto.isLocked());
        assertTrue(travelDto.canRename());
        assertTrue(travelDto.canMove());
        assertTrue(travelDto.canDelete());
        assertFalse(travelDto.isLocked());
        assertFalse(travelDto.isGuestDenied());
        assertFalse(defaultDto.isGuestDenied());
        assertEquals("Iceland", ResourceGroupTreeView.find(tree, iceland.getId().toString()).getHumanReadableId());
        assertEquals("Unsorted", ResourceGroupTreeView.find(tree, "missing").getHumanReadableId());
        assertEquals("Unsorted", ResourceGroupTreeView.find(tree, "ungrouped").getHumanReadableId());
    }

    @Test
    void moveTargetsRefuseSelfAndDescendants() {
        ResourceGroupEntity travel = group("Travel");
        ResourceGroupEntity y2024 = child("2024", travel);
        ResourceGroupEntity iceland = child("Iceland", y2024);
        ResourceGroupEntity studio = group("Studio");
        ResourceGroupTreeView.Assembled tree = ResourceGroupTreeView.assemble(
                List.of(travel, y2024, iceland, studio), List.of());
        ResourceGroupDisplayDTO travelDto = byName(tree.roots(), "Travel");
        ResourceGroupDisplayDTO icelandDto = travelDto.getChildren().get(0).getChildren().get(0);
        ResourceGroupDisplayDTO studioDto = byName(tree.roots(), "Studio");
        assertFalse(travelDto.canReparentUnder(icelandDto));
        assertFalse(travelDto.canReparentUnder(travelDto));
        assertTrue(icelandDto.canReparentUnder(studioDto));
        assertTrue(studioDto.canReparentUnder(icelandDto));
    }

    @Test
    void assembleStatsUsesHintsWithoutFileEntities() {
        ResourceGroupEntity travel = group("Travel");
        ResourceGroupEntity y2024 = child("2024", travel);
        ResourceGroupTreeView.Assembled tree = ResourceGroupTreeView.assembleStats(
                List.of(travel, y2024),
                List.of(new ResourceSizeHint(StoredIds.objectId(travel.getId()), 50),
                        new ResourceSizeHint(StoredIds.objectId(y2024.getId()), 100),
                        new ResourceSizeHint(null, 10)));
        ResourceGroupDisplayDTO travelDto = byName(tree.roots(), "Travel");
        assertEquals(1, travelDto.getOwnFileCount());
        assertEquals(50, travelDto.getTotalSize());
        assertEquals(2, travelDto.getDescendantFileCount());
        assertEquals(150, travelDto.getDescendantTotalSize());
        assertTrue(travelDto.getResources().isEmpty());
        assertTrue(travelDto.getTreeFiles().isEmpty());
        assertEquals(1, tree.ungrouped().getOwnFileCount());
        assertEquals(10, tree.ungrouped().getTotalSize());
    }

    @Test
    void assembleStatsFoldsOrphansIntoDefault() {
        ResourceGroupEntity defaults = group("Default");
        ResourceGroupEntity travel = group("Travel");
        ResourceGroupTreeView.Assembled tree = ResourceGroupTreeView.assembleStats(
                List.of(defaults, travel),
                List.of(new ResourceSizeHint(StoredIds.objectId(travel.getId()), 50),
                        new ResourceSizeHint(null, 10)));
        ResourceGroupDisplayDTO defaultDto = byName(tree.roots(), "Unsorted");
        ResourceGroupDisplayDTO travelDto = byName(tree.roots(), "Travel");
        assertEquals(1, defaultDto.getOwnFileCount());
        assertEquals(10, defaultDto.getTotalSize());
        assertEquals(1, travelDto.getOwnFileCount());
        assertEquals(0, tree.ungrouped().getOwnFileCount());
        assertTrue(tree.ungrouped().getTreeFiles().isEmpty());
        assertEquals(defaults.getId().toString(), ResourceGroupTreeView.defaultGroupId(tree));
    }

    @Test
    void assembleStatsAttachesNamedTreeFiles() {
        ResourceGroupEntity travel = group("Travel");
        ObjectId fileId = new ObjectId();
        ResourceGroupTreeView.Assembled tree = ResourceGroupTreeView.assembleStats(
                List.of(travel),
                List.of(new ResourceSizeHint(fileId, StoredIds.objectId(travel.getId()), "/u/res/image/aurora.jpg", 50)));
        ResourceGroupDisplayDTO travelDto = byName(tree.roots(), "Travel");
        assertEquals(1, travelDto.getTreeFiles().size());
        assertEquals(fileId.toString(), travelDto.getTreeFiles().get(0).id());
        assertEquals("aurora.jpg", travelDto.getTreeFiles().get(0).name());
        assertTrue(travelDto.hasExpandableChildren());
        assertFalse(travelDto.getTreeFiles().get(0).preview());
        ResourceGroupTreeView.applyPreviews(tree, java.util.Set.of(fileId.toString()));
        assertTrue(travelDto.getTreeFiles().get(0).preview());
    }

    @Test
    void fileDeltaUpdatesOwnAndAncestorTotals() {
        ResourceGroupEntity travel = group("Travel");
        ResourceGroupEntity y2024 = child("2024", travel);
        ResourceGroupTreeView.Assembled tree = ResourceGroupTreeView.assembleStats(
                List.of(travel, y2024), List.of());
        ResourceGroupTreeView.applyFileDelta(tree, y2024.getId().toString(), 1, 40);
        ResourceGroupDisplayDTO travelDto = byName(tree.roots(), "Travel");
        ResourceGroupDisplayDTO y2024Dto = travelDto.getChildren().get(0);
        assertEquals(1, y2024Dto.getOwnFileCount());
        assertEquals(40, y2024Dto.getTotalSize());
        assertEquals(1, y2024Dto.getDescendantFileCount());
        assertEquals(0, travelDto.getOwnFileCount());
        assertEquals(1, travelDto.getDescendantFileCount());
        assertEquals(40, travelDto.getDescendantTotalSize());
        ResourceGroupTreeView.Assembled copy = ResourceGroupTreeView.copy(tree);
        ResourceGroupTreeView.applyFileDelta(copy, y2024.getId().toString(), -1, -40);
        assertEquals(0, copy.roots().get(0).getDescendantFileCount());
        assertEquals(1, tree.roots().get(0).getDescendantFileCount());
    }

    @Test
    void withResourcesDoesNotMutateSnapshotChildrenStats() {
        ResourceGroupEntity travel = group("Travel");
        ResourceGroupTreeView.Assembled tree = ResourceGroupTreeView.assembleStats(
                List.of(travel), List.of(new ResourceSizeHint(StoredIds.objectId(travel.getId()), 9)));
        ResourceGroupDisplayDTO selected = ResourceGroupTreeView.withResources(
                tree.roots().get(0), List.of(resource(travel, 9)));
        assertEquals(1, selected.getResources().size());
        assertTrue(tree.roots().get(0).getResources().isEmpty());
        assertEquals(1, tree.roots().get(0).getOwnFileCount());
    }

    @Test
    void guestDeniedFollowsReadAclNotSystemLock() {
        ResourceGroupEntity defaults = group("Default");
        ResourceGroupEntity travel = group("Travel");
        travel.setSecurityData(new SecurityData(EntityAccessConstants.forLevel(SecurityLevel.OPERATOR)));
        ResourceGroupTreeView.Assembled tree = ResourceGroupTreeView.assembleStats(
                List.of(defaults, travel), List.of());
        ResourceGroupDisplayDTO defaultDto = byName(tree.roots(), "Unsorted");
        ResourceGroupDisplayDTO travelDto = byName(tree.roots(), "Travel");
        assertTrue(defaultDto.isLocked());
        assertFalse(defaultDto.isGuestDenied());
        assertFalse(travelDto.isLocked());
        assertTrue(travelDto.isGuestDenied());
        assertTrue(travelDto.canEditAccess());
        assertFalse(defaultDto.canEditAccess());
    }

    private static ResourceGroupDisplayDTO byName(List<ResourceGroupDisplayDTO> groups, String name) {
        return groups.stream()
                .filter(group -> name.equals(group.getHumanReadableId()))
                .findFirst()
                .orElseThrow();
    }

    private static ResourceGroupEntity group(String name) {
        ResourceGroupEntity entity = new ResourceGroupEntity(new ResourceGroupData(name), null);
        entity.setId(EntityId.generate());
        return entity;
    }

    private static ResourceGroupEntity child(String name, ResourceGroupEntity parent) {
        ResourceGroupEntity entity = group(name);
        entity.setRefParentGroup(parent);
        return entity;
    }

    private static ResourceEntity resource(ResourceGroupEntity group, long bytes) {
        ResourceData data = new ResourceData();
        data.setSizeInBytes(bytes);
        data.setPathPublic("/u/res/image/file-" + bytes + ".jpg");
        ResourceEntity entity = new ResourceEntity(data, null);
        entity.setId(EntityId.generate());
        entity.setRefResourceGroup(group);
        return entity;
    }
}
