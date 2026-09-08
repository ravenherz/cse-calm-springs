package com.ravenherz.cse.dal;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
