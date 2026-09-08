package com.ravenherz.cse.dal.dao;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.EntityAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public interface ItemService extends Service {

    default List<ItemEntity> getAllByCategoriesAndAccessibility(List<CategoryEntity> categoryEntities,
                                                                AccountEntity accountEntity) {
        List<ItemEntity> output = new ArrayList<>();
        for (CategoryEntity categoryEntity: categoryEntities) {
            output.addAll(getAllByCategoryAndAccessibility(categoryEntity, accountEntity));
        }
        return output;
    }

    default List<ItemEntity> getAllByCategoryAndAccessibility(CategoryEntity categoryEntity,
            AccountEntity accountEntity) {
        return getAllByCategory(categoryEntity).stream().filter(pageEntity -> EntityAccess
                .isAccessible(pageEntity, AccessType.ACCESS_READ, accountEntity))
                .collect(Collectors.toList());
    }

    List<ItemEntity> getAllByCategory(CategoryEntity categoryEntity);

    List<ItemEntity> getAllByTag(String tag);

    List<ItemEntity> getAllByRefImage(ResourceEntity resource);

    ItemEntity getByName(String name);

    void delete(ItemEntity item);

}
