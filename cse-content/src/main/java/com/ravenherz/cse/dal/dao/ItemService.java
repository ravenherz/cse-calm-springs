package com.ravenherz.cse.dal.dao;

import com.ravenherz.cse.dal.AccessActor;
import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public interface ItemService extends Store {

    default List<ItemEntity> getAllByCategoriesAndAccessibility(List<CategoryEntity> categoryEntities,
                                                                AccessActor actor) {
        List<ItemEntity> output = new ArrayList<>();
        for (CategoryEntity categoryEntity: categoryEntities) {
            output.addAll(getAllByCategoryAndAccessibility(categoryEntity, actor));
        }
        return output;
    }

    default List<ItemEntity> getAllByCategoryAndAccessibility(CategoryEntity categoryEntity,
            AccessActor actor) {
        return getAllByCategory(categoryEntity).stream().filter(pageEntity -> EntityAccess
                .isAccessible(pageEntity, AccessType.ACCESS_READ, actor))
                .collect(Collectors.toList());
    }

    List<ItemEntity> getAllByCategory(CategoryEntity categoryEntity);

    List<ItemEntity> getAllByTag(String tag);

    List<ItemEntity> getAllByRefImage(EntityId imageId);

    ItemEntity getByName(String name);

    void delete(ItemEntity item);

}
