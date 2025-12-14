package com.ravenherz.rhzwe.dal.business.impl;

import com.ravenherz.rhzwe.dal.EntityUtils;
import com.ravenherz.rhzwe.dal.business.Dealer;
import com.ravenherz.rhzwe.dal.business.NavigationDealer;
import com.ravenherz.rhzwe.dal.business.basic.Navigation;
import com.ravenherz.rhzwe.dal.business.basic.NavigationUnit;
import com.ravenherz.rhzwe.dal.business.basic.NavigationUnit.NavigationUnitType;
import com.ravenherz.rhzwe.dal.dto.AccountEntity;
import com.ravenherz.rhzwe.dal.dto.CategoryEntity;
import com.ravenherz.rhzwe.dal.dto.ItemEntity;
import com.ravenherz.rhzwe.dal.dto.basic.Event;
import com.ravenherz.rhzwe.dal.dto.basic.enums.AccessType;
import com.ravenherz.rhzwe.dal.dto.basic.enums.EventType;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Lazy
@Repository(value = "navigationDealer")
@Scope(value = "singleton")
@DependsOn("serviceProvider")
public class NavigationDealerImpl extends Dealer implements NavigationDealer {

    @Override
    public Navigation getNavigation(AccountEntity accessor) {
        Navigation nav = new Navigation();
        List<CategoryEntity> categoryEntities = serviceProvider.getCategoryService()
                .getAllByVisibility(true).stream()
                .filter(entity -> EntityUtils.isAccessible(
                        entity,
                        AccessType.ACCESS_READ,
                        accessor
                ))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(categoryEntities)) {
            for (CategoryEntity categoryEntity : categoryEntities
            ) {
                NavigationUnit category = new NavigationUnit(
                        NavigationUnitType.CATEGORY, null,
                        categoryEntity.getCategoryData().getNavigationTitle(),
                        categoryEntity.getCategoryData().getNavigationDescription(), null,
                            Arrays.stream(categoryEntity.getHistoryData().getEvents())
                                    .filter(event -> EventType.ENTITY_CREATED.equals(event.getEventType()))
                                    .map(Event::getLocalDateTime)
                                    .findFirst().orElse(LocalDateTime.now()));
                List<ItemEntity> itemEntities = serviceProvider.getItemService()
                        .getAllByCategory(categoryEntity).stream()
                        .filter(e -> EntityUtils.isAccessible(e, AccessType.ACCESS_READ, accessor))
                        .collect(Collectors.toList());
                if (itemEntities.size() > 0) {
                    for (ItemEntity itemEntity : itemEntities) {
                        NavigationUnitType type;
                        switch (itemEntity.getItemType()) {
                            case PAGE:
                                category.addChild(new NavigationUnit(
                                        NavigationUnitType.PAGE,
                                        itemEntity.getUniqueUriName(),
                                        itemEntity.getPageData().getTitle(),
                                        itemEntity.getPageData().getSubHeader(),
                                        null,
                                        Arrays.stream(categoryEntity.getHistoryData().getEvents())
                                                .filter(event -> EventType.ENTITY_CREATED.equals(event.getEventType()))
                                                .map(Event::getLocalDateTime)
                                                .findFirst().orElse(LocalDateTime.now())
                                ));
                                break;
                            case ALBUM:
                                category.addChild(new NavigationUnit(
                                        NavigationUnitType.ALBUM,
                                        itemEntity.getUniqueUriName(),
                                        itemEntity.getAlbumData().getTitle(),
                                        itemEntity.getAlbumData().getSubHeader(),
                                        null,
                                        Arrays.stream(categoryEntity.getHistoryData().getEvents())
                                                .filter(event -> EventType.ENTITY_CREATED.equals(event.getEventType()))
                                                .map(Event::getLocalDateTime)
                                                .findFirst().orElse(LocalDateTime.now())
                                ));
                                break;
                            default:
                                type = null;
                        }

                    }
                    nav.addItem(category);
                }
            }
        }
        return nav;
    }
}
