package com.ravenherz.cse.dal.dao.impl;

import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.dao.BasicService;
import com.ravenherz.cse.dal.dao.ItemService;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.AlbumData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository(value = "itemService")
public class ItemServiceImpl extends BasicService implements ItemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemServiceImpl.class);

    public ItemServiceImpl(DataProvider dataProvider) {
        super(dataProvider);
    }

    @Override
    public List<BasicEntity> getAll() {
        List<BasicEntity> items = new ArrayList<>();
        int consecutiveFailures = 0;
        for (ItemEntity item : mongo().findAll(ItemEntity.class)) {
            try {
                items.add(item);
                consecutiveFailures = 0;
            } catch (Exception e) {
                consecutiveFailures++;
                LOGGER.error("Skipping item Mongo could not fetch: {}", e.getMessage(), e);
                if (consecutiveFailures > 5) {
                    LOGGER.error("Too many consecutive item fetch failures; stopping the scan");
                    break;
                }
            }
        }
        return items;
    }

    @Override
    public List<ItemEntity> getAllByCategory(CategoryEntity categoryEntity) {
        if (categoryEntity == null || categoryEntity.getId() == null) {
            return List.of();
        }
        return mongo().find(Query.query(Criteria.where("refCategoryId").is(categoryEntity.getId())),
                ItemEntity.class);
    }

    @Override
    public ItemEntity getByName(String name) {
        return mongo().findOne(Query.query(Criteria.where("uniqueUriName").is(name)), ItemEntity.class);
    }

    @Override
    public List<ItemEntity> getAllByTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return List.of();
        }
        String needle = tag.trim();
        List<ItemEntity> tagged = new ArrayList<>();
        for (ItemEntity item : mongo().findAll(ItemEntity.class)) {
            try {
                if (itemHasTag(item, needle)) {
                    tagged.add(item);
                }
            } catch (Exception e) {
                LOGGER.error("Skipping item while scanning tags: {}", e.getMessage(), e);
            }
        }
        return tagged;
    }

    private static boolean itemHasTag(ItemEntity item, String tag) {
        if (item == null) {
            return false;
        }
        if (item.isAlbum()) {
            AlbumData albumData = item.getAlbumData();
            return albumData != null && containsTag(albumData.getTags(), tag);
        }
        PageData pageData = item.getPageData();
        return pageData != null && containsTag(pageData.getTags(), tag);
    }

    private static boolean containsTag(List<String> tags, String tag) {
        if (tags == null || tags.isEmpty()) {
            return false;
        }
        for (String candidate : tags) {
            if (candidate != null && tag.equals(candidate.trim())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void delete(ItemEntity item) {
        if (item != null) {
            mongo().remove(item);
        }
    }

    @Override
    public List<ItemEntity> getAllByRefImage(ResourceEntity resource) {
        if (resource == null || resource.getId() == null) {
            return List.of();
        }
        return mongo().find(Query.query(Criteria.where("pageData.refImageId").is(resource.getId())),
                ItemEntity.class);
    }
}
