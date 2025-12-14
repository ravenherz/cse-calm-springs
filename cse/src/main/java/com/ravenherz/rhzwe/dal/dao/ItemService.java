package com.ravenherz.rhzwe.dal.dao;

import com.ravenherz.rhzwe.dal.EntityUtils;
import com.ravenherz.rhzwe.dal.dto.AccountEntity;
import com.ravenherz.rhzwe.dal.dto.CategoryEntity;
import com.ravenherz.rhzwe.dal.dto.ItemEntity;
import com.ravenherz.rhzwe.dal.dto.basic.enums.AccessType;

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
        return getAllByCategory(categoryEntity).stream().filter(pageEntity -> EntityUtils
                .isAccessible(pageEntity, AccessType.ACCESS_READ, accountEntity))
                .collect(Collectors.toList());
    }

    List<ItemEntity> getAllByCategory(CategoryEntity categoryEntity);

    List<ItemEntity> getAllByTag(String tag);

    ItemEntity getByName(String name);

}
