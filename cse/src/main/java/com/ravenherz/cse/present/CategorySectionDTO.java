package com.ravenherz.cse.present;

import com.ravenherz.cse.dal.dto.basic.CategoryData;
import com.ravenherz.cse.dal.dto.events.PageEvent;
import com.ravenherz.cse.dal.dto.CategoryEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CategorySectionDTO {

    private String slug;
    private String itemName;
    private String title;
    private String description;
    private String kind;
    private List<PageEvent> pages = new ArrayList<>();
    private boolean visible;
    private boolean hasMore;
    private int totalCount;

    public static Comparator<CategoryEntity> displayOrder() {
        return Comparator
                .comparingInt((CategoryEntity category) -> {
                    CategoryData data = category.getCategoryData();
                    return data == null ? 0 : data.getDisplayPriority();
                })
                .reversed()
                .thenComparing(category -> {
                    CategoryData data = category.getCategoryData();
                    String name = data == null ? "" : data.getItemName();
                    return name == null ? "" : name;
                }, String.CASE_INSENSITIVE_ORDER);
    }

    public static CategorySectionDTO from(CategoryEntity category, List<PageEvent> pages) {
        return from(category, pages, false);
    }

    public static CategorySectionDTO from(CategoryEntity category, List<PageEvent> pages,
            boolean limitForHome) {
        CategoryData data = category.getCategoryData();
        String itemName = data != null && data.getItemName() != null ? data.getItemName() : "section";
        String title = data != null && data.getNavigationTitle() != null
                && !data.getNavigationTitle().isBlank()
                ? data.getNavigationTitle()
                : itemName;
        String description = data != null ? data.getNavigationDescription() : null;
        int displayCount = data != null ? data.getDisplayCount() : 0;
        List<PageEvent> allPages = pages == null ? new ArrayList<>() : pages;
        int total = allPages.size();
        boolean hasMore = limitForHome && displayCount > 0 && total > displayCount;
        CategorySectionDTO dto = new CategorySectionDTO();
        dto.slug = slugify(itemName);
        dto.itemName = itemName;
        dto.title = title;
        dto.description = description;
        dto.pages = hasMore ? new ArrayList<>(allPages.subList(0, displayCount)) : allPages;
        dto.kind = inferKind(itemName + " " + title);
        dto.visible = data != null && data.isVisible();
        dto.hasMore = hasMore;
        dto.totalCount = total;
        return dto;
    }

    public static CategorySectionDTO untitled(String title, List<PageEvent> pages) {
        CategorySectionDTO dto = new CategorySectionDTO();
        dto.slug = slugify(title);
        dto.itemName = title;
        dto.title = title;
        dto.description = null;
        dto.pages = pages == null ? new ArrayList<>() : pages;
        dto.kind = inferKind(title);
        dto.visible = true;
        dto.hasMore = false;
        dto.totalCount = dto.pages.size();
        return dto;
    }

    public static String slugify(String raw) {
        if (raw == null || raw.isBlank()) {
            return "section";
        }
        String slug = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "section" : slug;
    }

    static String inferKind(String haystack) {
        String hay = haystack == null ? "" : haystack.toLowerCase(Locale.ROOT);
        if (hay.contains("music") || hay.contains("release") || hay.contains("audio")
                || hay.contains("album") || hay.contains("track")) {
            return "music";
        }
        if (hay.contains("photo") || hay.contains("image") || hay.contains("visual")
                || hay.contains("gallery")) {
            return "photo";
        }
        if (hay.contains("software") || hay.contains("product") || hay.contains("app")
                || hay.contains("tool")) {
            return "software";
        }
        if (hay.contains("about") || hay.contains("cv") || hay.contains("bio")
                || hay.contains("resume")) {
            return "about";
        }
        return "default";
    }

    public boolean isFeatured() {
        return !hasMore && ("about".equals(kind) || (pages != null && pages.size() == 1));
    }

    public PageEvent getFeature() {
        return pages == null || pages.isEmpty() ? null : pages.get(0);
    }

    public String getSlug() {
        return slug;
    }

    public String getItemName() {
        return itemName;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getKind() {
        return kind;
    }

    public List<PageEvent> getPages() {
        return pages;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public boolean getHasMore() {
        return hasMore;
    }

    public int getTotalCount() {
        return totalCount;
    }
}
