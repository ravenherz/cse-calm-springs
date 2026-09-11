package com.ravenherz.cse.dal;

import com.ravenherz.cse.dal.dto.basic.AccessRule;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.dal.role.RoleSeeds;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MongoTimeConversionsTest {

    @Test
    void dateIsUtcLocalDateTime() {
        LocalDateTime value = LocalDateTime.of(2026, Month.AUGUST, 22, 1, 14, 0);
        Date date = Date.from(value.toInstant(ZoneOffset.UTC));
        assertEquals(value, MongoTimeConversions.fromDate(date));
    }

    @Test
    void morphiaStyleDateAndTimeDocument() {
        Document time = new Document("hour", 1).append("minute", 14).append("second", 0).append("nano", 0);
        Document date = new Document("year", 2026).append("month", "AUGUST").append("dayOfMonth", 22);
        Document document = new Document("date", date).append("time", time);
        assertEquals(LocalDateTime.of(2026, 8, 22, 1, 14, 0), MongoTimeConversions.fromDocument(document));
    }

    @Test
    void stringAccessSettingsBecomeLegacyRules() {
        AccessRule guest = new MongoTimeConversions.StringToAccessRuleConverter().convert("GUEST");
        assertFalse(guest.isInherit());
        assertTrue(guest.getRoleIds().contains(RoleSeeds.GUEST));
        AccessRule owner = new MongoTimeConversions.StringToAccessRuleConverter().convert("OWNER");
        assertTrue(owner.getRoleIds().isEmpty());
        AccessRule unknown = new MongoTimeConversions.StringToAccessRuleConverter().convert("NOPE");
        assertTrue(unknown.isInherit());
    }

    @Test
    void documentAccessSettingsRoundTrip() {
        AccessRule source = AccessRule.fromLegacy(SecurityLevel.GUIDE);
        Document document = new MongoTimeConversions.AccessRuleToDocumentConverter().convert(source);
        AccessRule read = new MongoTimeConversions.DocumentToAccessRuleConverter().convert(document);
        assertFalse(read.isInherit());
        assertEquals(source.getRoleIds(), read.getRoleIds());
    }
}
