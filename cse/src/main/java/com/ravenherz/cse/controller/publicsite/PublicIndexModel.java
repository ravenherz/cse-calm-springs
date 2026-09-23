package com.ravenherz.cse.controller.publicsite;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.constants.Strings;
import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.dto.events.PageEvent;
import com.ravenherz.cse.dal.dto.events.PageEvent.PageEventConverter;
import com.ravenherz.cse.present.CategorySectionDTO;
import com.ravenherz.cse.engine.util.Settings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PublicIndexModel {

    public enum PagesAttribute {

        LEFT("pagesLeft"),
        RIGHT("pagesRight");

        private static final Map<Integer, PagesAttribute> valuesMap;
        static {
            valuesMap = Arrays.stream(PagesAttribute.values())
                    .collect(Collectors.toUnmodifiableMap(
                            Enum::ordinal,
                            (v) -> v)
                    );
        }

        private final String title;

        PagesAttribute(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public static PagesAttribute getByOrdinal(int ordinal) {
            return valuesMap.getOrDefault(ordinal, null);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicIndexModel.class);

    private static final Comparator<PageEvent> PAGE_EVENT_COMPARATOR = Comparator
            .comparingLong((PageEvent event) -> event.getCreated().toEpochSecond(ZoneOffset.UTC))
            .reversed();

    private final ServiceProvider serviceProvider;
    private final Settings settings;

    public PublicIndexModel(ServiceProvider serviceProvider, Settings settings) {
        this.serviceProvider = serviceProvider;
        this.settings = settings;
    }

    public void addPageContentToModel(Model model, HttpServletRequest request,
            HttpServletResponse response, String page, String album, String tag, String category,
            AccountEntity accessor, ErrorRedirect errorRedirect)
            throws IOException {
        List<List<PageEvent>> events = new ArrayList<>();
        List<CategorySectionDTO> sections = new ArrayList<>();
        List<CategoryEntity> activeCategories = serviceProvider.getCategoryService().getAllActive();
        activeCategories.sort(CategorySectionDTO.displayOrder());
        List<CategorySectionDTO> navCategories = new ArrayList<>();
        for (CategoryEntity navCategory : activeCategories) {
            if (navCategory.getCategoryData() != null && navCategory.getCategoryData().isVisible()) {
                navCategories.add(CategorySectionDTO.from(navCategory, Collections.emptyList()));
            }
        }
        model.addAttribute("navCategories", navCategories);

        String requestedName = firstNonBlank(page, album);
        if (requestedName != null) {
            ItemEntity itemEntity = serviceProvider.getItemService().getByName(requestedName);

            if (itemEntity == null) {
                errorRedirect.redirect(404, request, response);
                return;
            } else if (EntityAccess
                    .isAccessible(itemEntity, AccessType.ACCESS_READ, accessor)) {
                PageEvent pageEvent = toPublicEvent(itemEntity);
                events.add(Collections.singletonList(pageEvent));
                String sectionTitle = pageEvent.getCategoryItemName();
                if (itemEntity.getRefCategory() != null
                        && itemEntity.getRefCategory().getCategoryData() != null
                        && itemEntity.getRefCategory().getCategoryData().getNavigationTitle() != null
                        && !itemEntity.getRefCategory().getCategoryData().getNavigationTitle().isBlank()) {
                    sectionTitle = itemEntity.getRefCategory().getCategoryData().getNavigationTitle();
                }
                sections.add(CategorySectionDTO.untitled(
                        sectionTitle, Collections.singletonList(pageEvent)));
                model.addAttribute("readingPage", true);
                model.addAttribute("readingAlbum", itemEntity.isAlbum());
                String header = itemEntity.isAlbum()
                        ? (itemEntity.getAlbumData() == null ? null : itemEntity.getAlbumData().getHeader())
                        : (itemEntity.getPageData() == null ? null : itemEntity.getPageData().getHeader());
                String itemTitle = header != null && !header.isBlank() ? header : requestedName;
                model.addAttribute("htmlTitle", String.format("%s%s%s",
                        settings.getValue(SettingKeys.CONTEXT_DATASOURCE_PERSONAL,
                                SettingKeys.KEY_TAG_COMPANY_TITLE),
                        Strings.STR_DELIM_COLONDWWS,
                        itemTitle == null ? requestedName : itemTitle));
            } else {
                errorRedirect.redirect(403, request, response);
                return;
            }
        } else {
            if (tag != null && !tag.isBlank()) {
                String requestedTag = tag.trim();
                List<PageEvent> tagged = resolveTaggedEvents(requestedTag, accessor);
                events.add(tagged);
                sections.add(CategorySectionDTO.untitled(requestedTag, tagged));
                model.addAttribute("tagView", true);
                model.addAttribute("htmlTitle", String.format("%s%s by tag %s",
                        settings.getValue(SettingKeys.CONTEXT_DATASOURCE_PERSONAL,
                                SettingKeys.KEY_TAG_COMPANY_TITLE),
                        Strings.STR_DELIM_COLONDWWS,
                        requestedTag));
            } else if (category != null && category.trim().length() != 0) {
                CategoryEntity requested = findCategory(activeCategories, category.trim());
                if (requested == null) {
                    errorRedirect.redirect(404, request, response);
                    return;
                } else {
                    List<PageEvent> pageEvents = serviceProvider.getItemService()
                            .getAllByCategoryAndAccessibility(requested, accessor)
                            .stream()
                            .filter(PublicIndexModel::isPublicContent)
                            .map(this::toPublicEvent)
                            .sorted(PAGE_EVENT_COMPARATOR)
                            .collect(Collectors.toList());
                    events.add(pageEvents);
                    sections.add(CategorySectionDTO.from(requested, pageEvents));
                    model.addAttribute("categoryView", true);
                    model.addAttribute("htmlTitle", String.format("%s%s%s",
                            settings.getValue(SettingKeys.CONTEXT_DATASOURCE_PERSONAL,
                                    SettingKeys.KEY_TAG_COMPANY_TITLE),
                            Strings.STR_DELIM_COLONDWWS,
                            sections.get(0).getTitle()));
                }
            } else {
                model.addAttribute("portfolioHome", true);
                if (activeCategories.size() != 2) {
                    events.add(new ArrayList<>(serviceProvider.getItemService()
                            .getAllByCategoriesAndAccessibility(activeCategories, accessor)
                            .stream()
                            .filter(PublicIndexModel::isPublicContent)
                            .map(this::toPublicEvent)
                            .sorted(PAGE_EVENT_COMPARATOR)
                            .collect(Collectors.toList())));
                } else {
                    for (int i = 0; i < activeCategories.size(); i++) {
                        if (PagesAttribute.getByOrdinal(i) != null) {
                            List<PageEvent> pageEvents = serviceProvider.getItemService()
                                    .getAllByCategoryAndAccessibility(activeCategories.get(i), accessor)
                                    .stream()
                                    .filter(PublicIndexModel::isPublicContent)
                                    .map(this::toPublicEvent)
                                    .sorted(PAGE_EVENT_COMPARATOR)
                                    .collect(Collectors.toList());
                            events.add(pageEvents);
                        }
                    }
                }
                for (CategoryEntity homeCategory : activeCategories) {
                    List<PageEvent> pageEvents = serviceProvider.getItemService()
                            .getAllByCategoryAndAccessibility(homeCategory, accessor)
                            .stream()
                            .filter(PublicIndexModel::isPublicContent)
                            .map(this::toPublicEvent)
                            .sorted(PAGE_EVENT_COMPARATOR)
                            .collect(Collectors.toList());
                    if (!pageEvents.isEmpty()) {
                        sections.add(CategorySectionDTO.from(homeCategory, pageEvents, true));
                    }
                }
                PageEvent introPage = resolveDefaultPage(accessor);
                if (introPage != null) {
                    model.addAttribute("introPage", introPage);
                }
            }
        }
        model.addAttribute("pages", events);
        model.addAttribute("sections", sections);
    }

    /**
     * Tag listing:
     * 1) load all items with the tag
     * 2) keep only items the actor can read
     * 3) collect those items' categories
     * 4) keep categories that are active and readable by the actor
     * 5) keep items whose category survived step 4
     */
    private List<PageEvent> resolveTaggedEvents(String tag, AccountEntity accessor) {
        List<ItemEntity> taggedItems = serviceProvider.getItemService().getAllByTag(tag).stream()
                .filter(PublicIndexModel::isPublicContent)
                .collect(Collectors.toList());

        taggedItems = taggedItems.stream()
                .filter(item -> EntityAccess.isAccessible(item, AccessType.ACCESS_READ, accessor))
                .collect(Collectors.toList());

        Map<EntityId, CategoryEntity> categoriesById = new LinkedHashMap<>();
        for (ItemEntity item : taggedItems) {
            CategoryEntity itemCategory = item.getRefCategory();
            if (itemCategory != null && itemCategory.getId() != null) {
                categoriesById.putIfAbsent(itemCategory.getId(), itemCategory);
            }
        }

        Set<EntityId> allowedCategoryIds = categoriesById.values().stream()
                .filter(itemCategory -> itemCategory.getCategoryData() != null
                        && itemCategory.getCategoryData().isActive())
                .filter(itemCategory -> EntityAccess.isAccessible(itemCategory, AccessType.ACCESS_READ, accessor))
                .map(CategoryEntity::getId)
                .collect(Collectors.toCollection(HashSet::new));

        return taggedItems.stream()
                .filter(item -> item.getRefCategory() != null
                        && item.getRefCategory().getId() != null
                        && allowedCategoryIds.contains(item.getRefCategory().getId()))
                .map(this::toPublicEvent)
                .sorted(PAGE_EVENT_COMPARATOR)
                .collect(Collectors.toList());
    }

    private PageEvent toPublicEvent(ItemEntity item) {
        if (item != null && item.isAlbum()) {
            EntityId groupId = item.getAlbumData() == null ? null : item.getAlbumData().getRefResourceGroupId();
            ResourceGroupEntity group = null;
            if (groupId != null) {
                BasicEntity found = serviceProvider.getResourceGroupService()
                        .getById(ResourceGroupEntity.class, groupId);
                group = found instanceof ResourceGroupEntity loaded ? loaded : null;
            }
            List<ResourceEntity> images = serviceProvider.getResourceService().getImagesByGroup(group);
            return PageEventConverter.toEvent(item, images);
        }
        EntityId imageId = item == null || item.getPageData() == null ? null : item.getPageData().getRefImageId();
        ResourceEntity featured = null;
        if (imageId != null) {
            BasicEntity found = serviceProvider.getResourceService().getById(ResourceEntity.class, imageId);
            featured = found instanceof ResourceEntity loaded ? loaded : null;
        }
        return PageEventConverter.toEvent(item, featured);
    }

    private static boolean isPublicContent(ItemEntity item) {
        return item != null && (item.getItemType() == null
                || item.getItemType() == ItemEntity.ItemType.PAGE
                || item.getItemType() == ItemEntity.ItemType.ALBUM);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private CategoryEntity findCategory(List<CategoryEntity> categories, String requested) {
        for (CategoryEntity candidate : categories) {
            if (candidate.getCategoryData() == null) {
                continue;
            }
            String itemName = candidate.getCategoryData().getItemName();
            if (requested.equals(itemName) || requested.equals(CategorySectionDTO.slugify(itemName))) {
                return candidate;
            }
        }
        return null;
    }

    private PageEvent resolveDefaultPage(AccountEntity accessor) {
        String name = settings.getValue(SettingKeys.CONTEXT_DATASOURCE_VIEW, SettingKeys.KEY_DEFAULT_PAGE);
        if (name == null || name.isBlank()) {
            return null;
        }
        ItemEntity item = serviceProvider.getItemService().getByName(name.trim());
        if (item == null || item.getPageData() == null) {
            LOGGER.warn("Default page not found: " + name);
            return null;
        }
        if (!ItemEntity.ItemType.PAGE.equals(item.getItemType())) {
            return null;
        }
        if (!EntityAccess.isAccessible(item, AccessType.ACCESS_READ, accessor)) {
            return null;
        }
        return toPublicEvent(item);
    }

    @FunctionalInterface
    public interface ErrorRedirect {
        void redirect(int code, HttpServletRequest request, HttpServletResponse response) throws IOException;
    }
}
