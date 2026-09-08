package com.ravenherz.cse.dal.impl;

import com.mongodb.ConnectionString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataProviderImplUriTest {

    @Test
    void doesNotDoublePortWhenAddressAlreadyHasOne() {
        String uri = DataProviderImpl.mongoConnectionUri(
                "mongodb", "u", "p", "localhost:27017", "27017", "rhz-we-site");
        assertFalse(uri.contains("27017:27017"));
        assertTrue(uri.contains("@localhost:27017/"));
        new ConnectionString(uri);
    }

    @Test
    void encodesAtSignInPassword() {
        String uri = DataProviderImpl.mongoConnectionUri(
                "mongodb", "u", "p@ss", "localhost", "27017", "db");
        assertTrue(uri.contains("p%40ss"));
        new ConnectionString(uri);
    }

    @Test
    void usesPastedMongoUriAsIs() {
        String pasted = "mongodb://u:p@localhost:27017/db";
        String uri = DataProviderImpl.mongoConnectionUri(
                "mongodb", "ignored", "ignored", pasted, "27017", "db");
        assertEquals(pasted, uri);
        new ConnectionString(uri);
    }

    @Test
    void emptyAddressIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> DataProviderImpl.mongoConnectionUri(
                "mongodb", "u", "p", "", "27017", "db"));
    }
}
