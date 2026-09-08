package com.ravenherz.cse.dal.dao.impl;

import com.ravenherz.cse.constants.MongoCollections;
import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.MediaCache;
import com.ravenherz.cse.dal.dao.BasicService;
import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.DataChunkEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.ResourcePreviewSource;
import com.ravenherz.cse.dal.dto.basic.ResourceSizeHint;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository(value = "resourceService")
public class ResourceServiceImpl extends BasicService implements ResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceServiceImpl.class);

    private final MediaCache mediaCache;

    public ResourceServiceImpl(DataProvider dataProvider, MediaCache mediaCache) {
        super(dataProvider);
        this.mediaCache = mediaCache;
    }

    @Override
    public ResourceEntity getByPublicPath(String publicPath) {
        return mongo().findOne(Query.query(new Criteria().orOperator(
                Criteria.where("resourceData.pathPublic").is(publicPath),
                Criteria.where("previewData.pathPublic").is(publicPath))), ResourceEntity.class);
    }

    @Override
    public List<BasicEntity> getAll() {
        return new java.util.ArrayList<>(mongo().findAll(ResourceEntity.class));
    }

    @Override
    public ResourceEntity getByProtectedPath(String protectedPath) {
        return mongo().findOne(Query.query(new Criteria().orOperator(
                Criteria.where("resourceData.pathProtected").is(protectedPath),
                Criteria.where("previewData.pathProtected").is(protectedPath))), ResourceEntity.class);
    }

    @Override
    public void deleteByPublicPath(String publicPath) {
        ResourceEntity entity = getByPublicPath(publicPath);
        if (entity != null) {
            String filePath = mediaCache.cachedMedia(entity.getResourceData().getPathProtected()).getAbsolutePath();
            java.io.File cacheFile = new java.io.File(filePath);
            if (cacheFile.exists()) {
                if (cacheFile.delete()) LOGGER.info("Deleted cache file: " + filePath);
                else LOGGER.warn("Failed to delete cache file: " + filePath);
            } else LOGGER.warn("Cache file does not exist: " + filePath);
            if (entity.getPreviewData() != null && entity.getPreviewData().getPathProtected() != null) {
                String previewFilePath = mediaCache.cachedMedia(entity.getPreviewData().getPathProtected())
                        .getAbsolutePath();
                java.io.File previewCacheFile = new java.io.File(previewFilePath);
                if (previewCacheFile.exists() && previewCacheFile.delete()) {
                    LOGGER.info("Deleted preview cache file: " + previewFilePath);
                }
            }
            deleteChunks(entity.getResourceData());
            deleteChunks(entity.getPreviewData());

            mongo().remove(entity);
        }
    }

    @Override
    public List<DataChunkEntity> getDataChunks(List<ObjectId> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<DataChunkEntity> chunks = new ArrayList<>();
        for (ObjectId id : chunkIds) {
            DataChunkEntity chunk = mongo().findById(id, DataChunkEntity.class);
            if (chunk != null) {
                chunks.add(chunk);
            }
        }
        return chunks;
    }

    @Override
    public byte[] getRawBytesFromChunks(List<ObjectId> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (ObjectId id : chunkIds) {
            DataChunkEntity chunk = mongo().findById(id, DataChunkEntity.class);
            if (chunk != null && chunk.getData() != null) {
                sb.append(chunk.getData());
            }
        }
        if (sb.length() == 0) {
            return null;
        }
        return Base64.getDecoder().decode(sb.toString());
    }

    @Override
    public void saveDataChunk(DataChunkEntity chunk) {
        mongo().save(chunk);
    }

    @Override
    public List<ResourceEntity> getImagesByGroup(ResourceGroupEntity group) {
        if (group == null || group.getId() == null) {
            return new ArrayList<>();
        }
        return mongo().find(Query.query(Criteria.where("refResourceGroup").is(group.getId())),
                ResourceEntity.class)
                .stream()
                .filter(resource -> resource.getResourceData() != null
                        && resource.getResourceData().getType() == ResourceType.IMAGE)
                .sorted((a, b) -> {
                    String left = a.getResourceData().getFileName();
                    String right = b.getResourceData().getFileName();
                    return String.CASE_INSENSITIVE_ORDER.compare(
                            left == null ? "" : left, right == null ? "" : right);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ResourceSizeHint> listSizeHints() {
        Query query = new Query();
        query.fields().include("refResourceGroup")
                .include("resourceData.sizeInBytes")
                .include("resourceData.pathPublic");
        List<ResourceSizeHint> hints = new ArrayList<>();
        for (Document doc : mongo().find(query, Document.class, MongoCollections.DATABASE_RESOURCES)) {
            if (doc == null) {
                continue;
            }
            hints.add(new ResourceSizeHint(
                    objectId(doc.get("_id")),
                    objectId(doc.get("refResourceGroup")),
                    pathPublic(doc),
                    sizeInBytes(doc)));
        }
        return hints;
    }

    @Override
    public List<ResourcePreviewSource> listPreviewSources() {
        Map<ObjectId, byte[]> byId = new LinkedHashMap<>();
        Query previewQuery = new Query();
        previewQuery.fields()
                .include("previewData.contentRaw")
                .include("previewData.pathProtected");
        for (Document doc : mongo().find(previewQuery, Document.class, MongoCollections.DATABASE_RESOURCES)) {
            if (doc == null) {
                continue;
            }
            ObjectId id = objectId(doc.get("_id"));
            if (id == null) {
                continue;
            }
            Document preview = nested(doc, "previewData");
            byte[] bytes = decodeRaw(preview);
            if (bytes == null) {
                bytes = cachedBytes(pathProtected(preview));
            }
            if (bytes != null) {
                byId.put(id, bytes);
            }
        }
        Query images = Query.query(new Criteria().andOperator(
                Criteria.where("resourceData.type").is(ResourceType.IMAGE),
                Criteria.where("previewData.contentRaw").exists(false)));
        images.fields()
                .include("resourceData.contentRaw")
                .include("resourceData.pathProtected")
                .include("resourceData.largeFile")
                .include("previewData.pathProtected");
        for (Document doc : mongo().find(images, Document.class, MongoCollections.DATABASE_RESOURCES)) {
            if (doc == null) {
                continue;
            }
            ObjectId id = objectId(doc.get("_id"));
            if (id == null || byId.containsKey(id)) {
                continue;
            }
            Document resource = nested(doc, "resourceData");
            byte[] bytes = null;
            if (!isLarge(resource)) {
                bytes = decodeRaw(resource);
            }
            if (bytes == null) {
                bytes = cachedBytes(pathProtected(nested(doc, "previewData")));
            }
            if (bytes == null) {
                bytes = cachedBytes(pathProtected(resource));
            }
            if (bytes != null) {
                byId.put(id, bytes);
            }
        }
        List<ResourcePreviewSource> out = new ArrayList<>(byId.size());
        for (Map.Entry<ObjectId, byte[]> entry : byId.entrySet()) {
            out.add(new ResourcePreviewSource(entry.getKey(), entry.getValue()));
        }
        return out;
    }

    @Override
    public List<ResourceEntity> listForEditor(ObjectId groupId) {
        Criteria group = groupId == null
                ? new Criteria().orOperator(
                        Criteria.where("refResourceGroup").is(null),
                        Criteria.where("refResourceGroup").exists(false))
                : Criteria.where("refResourceGroup").is(groupId);
        Query query = Query.query(group);
        query.fields().exclude("resourceData.contentRaw").exclude("previewData.contentRaw");
        return new ArrayList<>(mongo().find(query, ResourceEntity.class));
    }

    private static ObjectId objectId(Object raw) {
        if (raw instanceof ObjectId id) {
            return id;
        }
        if (raw instanceof String hex && ObjectId.isValid(hex)) {
            return new ObjectId(hex);
        }
        return null;
    }

    private static String pathPublic(Document doc) {
        Object data = doc.get("resourceData");
        if (data instanceof Document payload && payload.get("pathPublic") instanceof String path) {
            return path;
        }
        return null;
    }

    private static Document nested(Document doc, String field) {
        Object data = doc == null ? null : doc.get(field);
        return data instanceof Document payload ? payload : null;
    }

    private static String pathProtected(Document payload) {
        if (payload != null && payload.get("pathProtected") instanceof String path) {
            return path;
        }
        return null;
    }

    private static boolean isLarge(Document payload) {
        return payload != null && Boolean.TRUE.equals(payload.get("largeFile"));
    }

    private static byte[] decodeRaw(Document payload) {
        if (payload == null || !(payload.get("contentRaw") instanceof String raw) || raw.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(raw);
            if (bytes.length == 0 || bytes.length > 8 * 1024 * 1024) {
                return null;
            }
            return bytes;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private byte[] cachedBytes(String protectedPath) {
        if (protectedPath == null || protectedPath.isBlank() || mediaCache == null) {
            return null;
        }
        File file = mediaCache.cachedMedia(protectedPath);
        if (file == null || !file.isFile()) {
            return null;
        }
        long size = file.length();
        if (size <= 0 || size > 8L * 1024 * 1024) {
            return null;
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            LOGGER.debug("Cannot read cached media {}", file.getAbsolutePath());
            return null;
        }
    }

    private static long sizeInBytes(Document doc) {
        Object data = doc.get("resourceData");
        if (!(data instanceof Document payload)) {
            return 0L;
        }
        Object size = payload.get("sizeInBytes");
        return size instanceof Number number ? number.longValue() : 0L;
    }

    private void deleteChunks(ResourceData data) {
        if (data == null || !data.isLargeFile() || data.getDataChunkIds() == null) {
            return;
        }
        data.getDataChunkIds().forEach(id -> {
            DataChunkEntity chunk = mongo().findById(id, DataChunkEntity.class);
            if (chunk != null) {
                mongo().remove(chunk);
                LOGGER.info("Deleted data chunk: " + id);
            }
        });
    }
}
