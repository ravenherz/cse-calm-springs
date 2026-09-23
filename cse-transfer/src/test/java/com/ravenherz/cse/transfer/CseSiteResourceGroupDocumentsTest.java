package com.ravenherz.cse.transfer;

import com.ravenherz.cse.dal.EntityId;

import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.basic.ResourceGroupData;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CseSiteResourceGroupDocumentsTest {

    @Test
    void parentGroupIdRoundTrips() {
        ResourceGroupEntity parent = new ResourceGroupEntity(new ResourceGroupData("Travel"), null);
        parent.setId(EntityId.of("68b0000000000000000000aa"));
        ResourceGroupEntity child = new ResourceGroupEntity(new ResourceGroupData("2024"), null);
        child.setId(EntityId.of("68b0000000000000000000bb"));
        child.setRefParentGroup(parent);
        child.attachRefParentGroup(null);

        Map<String, Object> doc = CseSiteDocuments.resourceGroup(child);
        assertEquals("68b0000000000000000000bb", doc.get("id"));
        assertEquals("68b0000000000000000000aa", doc.get("refParentGroupId"));

        ResourceGroupEntity read = CseSiteReaders.resourceGroup(doc);
        assertEquals(parent.getId().toHexString(), read.refParentGroupObjectId().toHexString());
        assertNull(read.getRefParentGroup());
    }
}
