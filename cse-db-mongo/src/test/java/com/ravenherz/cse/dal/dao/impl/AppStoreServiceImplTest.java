package com.ravenherz.cse.dal.dao.impl;

import com.mongodb.client.MongoCollection;
import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.dao.AppService;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.basic.AppData;
import com.ravenherz.cse.store.AppStoreAccess;
import com.ravenherz.cse.store.AppStoreDocument;
import com.ravenherz.cse.store.AppStoreException;
import com.ravenherz.cse.store.AppStoreTableSpec;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppStoreServiceImplTest {

    private AppService apps;
    private MongoTemplate mongo;
    private AppStoreServiceImpl store;

    @BeforeEach
    void stubs() {
        DataProvider data = mock(DataProvider.class);
        apps = mock(AppService.class);
        mongo = mock(MongoTemplate.class);
        when(data.getMongoTemplate()).thenReturn(mongo);
        store = new AppStoreServiceImpl(data, apps);
        AppEntity app = enabledApp("fretlab");
        when(apps.getBySlug("fretlab")).thenReturn(app);
        when(mongo.getCollectionNames()).thenReturn(Set.of());
        when(mongo.getCollection(any())).thenReturn(mock(MongoCollection.class));
    }

    @Test
    void unknownAndReservedAppsAre404() {
        AppStoreException missing = assertThrows(AppStoreException.class, () -> store.requireApp("nope"));
        assertEquals(404, missing.getStatus());
        AppStoreException cms = assertThrows(AppStoreException.class, () -> store.requireApp("cse"));
        assertEquals(404, cms.getStatus());
        AppStoreException admin = assertThrows(AppStoreException.class, () -> store.requireApp("admin"));
        assertEquals(404, admin.getStatus());
    }

    @Test
    void disabledStoreCannotWrite() {
        AppEntity app = enabledApp("fretlab");
        app.getAppData().setStoreEnabled(false);
        when(apps.getBySlug("fretlab")).thenReturn(app);
        AppStoreException error = assertThrows(AppStoreException.class,
                () -> store.insert("fretlab", "progress", "owner", Map.of("tuning", "E")));
        assertEquals(403, error.getStatus());
    }

    @Test
    void insertUsesNamespacedCollectionAndSessionOwner() {
        store.insert("fretlab", "progress", "68b000000000000000000001",
                Map.of("tuning", "EADGBE", "ownerId", "evil"));
        org.mockito.ArgumentCaptor<Document> captor = org.mockito.ArgumentCaptor.forClass(Document.class);
        verify(mongo).insert(captor.capture(), eq("fretlab-progress"));
        Document saved = captor.getValue();
        assertEquals("68b000000000000000000001", saved.get("ownerId"));
        Document data = (Document) saved.get("data");
        assertEquals("EADGBE", data.get("tuning"));
        assertTrue(!data.containsKey("ownerId"));
    }

    @Test
    void hyphenatedSlugStaysOnItsOwnPrefix() {
        AppEntity snake = enabledApp("hello-snake");
        snake.getAppData().setStoreTables(List.of(new AppStoreTableSpec("scores", AppStoreAccess.OWNER, null)));
        when(apps.getBySlug("hello-snake")).thenReturn(snake);
        store.insert("hello-snake", "scores", "owner", Map.of("score", 9));
        verify(mongo).insert(any(Document.class), eq("hello-snake-scores"));
    }

    @Test
    void closedAllowlistRejectsUnknownTable() {
        AppStoreException error = assertThrows(AppStoreException.class,
                () -> store.requireTable("fretlab", "secrets"));
        assertEquals(404, error.getStatus());
    }

    @Test
    void quotaFailsAtTenThousandDocuments() {
        when(mongo.getCollectionNames()).thenReturn(Set.of("fretlab-progress"));
        when(mongo.count(any(Query.class), eq("fretlab-progress"))).thenReturn(10_000L);
        AppStoreException error = assertThrows(AppStoreException.class,
                () -> store.insert("fretlab", "progress", "owner", Map.of("n", 1)));
        assertEquals(507, error.getStatus());
    }

    @Test
    void toBsonAndFromBsonRoundTripEnvelope() {
        AppStoreDocument document = new AppStoreDocument();
        document.setId("68b0000000000000000000aa");
        document.setOwnerId("68b000000000000000000001");
        document.setCreatedAt("2026-09-08T12:00:00Z");
        document.setUpdatedAt("2026-09-08T12:00:00Z");
        document.setData(Map.of("tuning", "EADGBE"));
        Document bson = AppStoreServiceImpl.toBson(document.toMap());
        AppStoreDocument back = AppStoreServiceImpl.fromBson(bson);
        assertEquals(document.getId(), back.getId());
        assertEquals(document.getOwnerId(), back.getOwnerId());
        assertEquals("EADGBE", back.getData().get("tuning"));
    }

    private static AppEntity enabledApp(String slug) {
        AppData data = new AppData();
        data.setSlug(slug);
        data.setStoreEnabled(true);
        data.setStoreOpen(false);
        data.setStoreTables(List.of(new AppStoreTableSpec("progress", AppStoreAccess.OWNER, null)));
        AppEntity app = new AppEntity();
        app.setAppData(data);
        return app;
    }
}
