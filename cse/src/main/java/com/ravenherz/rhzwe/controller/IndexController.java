package com.ravenherz.rhzwe.controller;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.dal.EntityUtils;
import com.ravenherz.rhzwe.dal.dto.AccountEntity;
import com.ravenherz.rhzwe.dal.dto.*;
import com.ravenherz.rhzwe.dal.dto.basic.enums.AccessType;
import com.ravenherz.rhzwe.dal.dto.events.PageEvent;
import com.ravenherz.rhzwe.dal.dto.events.PageEvent.PageEventConverter;
import com.ravenherz.rhzwe.util.html.impl.TagNavigation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class IndexController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexController.class);

    public enum PagesAttribute {

        LEFT("pagesLeft"),
        RIGHT("pagesRight");

        private static final Map<Integer, PagesAttribute> valuesMap;
        static {
            valuesMap = Arrays.stream(PagesAttribute.values())
                    .collect(Collectors.toUnmodifiableMap(
                            Enum::ordinal,
                            (v) -> {return v;})
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


    private static Comparator<PageEvent> pageEventComparator;

    static {
        pageEventComparator = Comparator.comparingLong(
                event -> event.getCreated().toEpochSecond(ZoneOffset.UTC));
        pageEventComparator = pageEventComparator.reversed();
    }

    @RequestMapping (value = "/", method = RequestMethod.GET)
    public String getPage(
            Model model,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "page", required = false) String page,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "error", required = false) String error
    ) throws IOException {
        try {
            tags.forEach(tagProducer -> {
                if (tagProducer != null && tagProducer.getHtmlElement(null) != null) {
                    model.addAttribute(tagProducer.getClass().getSimpleName(),
                            tagProducer.getHtmlElement(null).getCode());
                }
            });

            Arrays.asList(Strings.KEY_STYLES_THEME, Strings.KEY_STYLES_SCHEMA)
                    .forEach(key -> model.addAttribute(Strings.jsonKeyToCamelCase(key),
                            settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW, key)));

            if (settings.isConfigured()) {

                AccountEntity accessor = getAccessor(request, response);
                if (accessor != null) {
                    model.addAttribute("username", accessor.getAccountData().getLogin());
                }
                model.addAttribute("authenticated", accessor != null);

                TagNavigation tagNavigation = new TagNavigation(true);
                String horizontalNavigation = tagNavigation.getHtmlElement(accessor).getCode();
                if (horizontalNavigation.length() > 0) {
                    model.addAttribute(tagNavigation.getClass().getSimpleName() + "Horizontal",
                            horizontalNavigation);
                    tagNavigation = new TagNavigation(false);
                    model.addAttribute(tagNavigation.getClass().getSimpleName() + "Vertical",
                            tagNavigation.getHtmlElement(accessor).getCode());
                    model.addAttribute("hasNavigation", true);
                } else {
                    model.addAttribute("hasNavigation", false);
                }

                addPageContentToModel(model, request, response, page, tag, accessor);

            }
            if (!model.containsAttribute("htmlTitle")) {
                model.addAttribute("htmlTitle",
                        settings.getValue(Strings.CONTEXT_DATASOURCE_PERSONAL,
                                Strings.KEY_TAG_COMPANY_TITLE));
            }
        } catch (Exception ex) {
            LOGGER.error("Exception", ex);
            ex.printStackTrace();
            error(500, request, response);
        }

        model.addAttribute("configured", settings.isConfigured());
        model.addAttribute("loginPanel", true);
        model.addAttribute("isNotError", request.getParameter("error") == null);
        model.addAttribute("selectedTag", request.getParameter("tag"));
        model.addAttribute("request", request);

        return String.format("/%s/index",
                settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW, Strings.KEY_STYLES_THEME));
    }

    private void addPageContentToModel(Model model, HttpServletRequest request,
            HttpServletResponse response, String page, String tag, AccountEntity accessor)
            throws IOException {
        List<List<PageEvent>> events = new ArrayList<>();
        if (page != null && page.trim().length() > 0) {
            ItemEntity itemEntity = serviceProvider.getItemService().getByName(page);

            if (itemEntity == null) {
                error(404, request, response);
            } else if (EntityUtils
                    .isAccessible(itemEntity, AccessType.ACCESS_READ, accessor)) {
                events.add(Collections.singletonList(PageEventConverter.toEvent(itemEntity)));
                model.addAttribute("htmlTitle", String.format("%s%s%s",
                        settings.getValue(Strings.CONTEXT_DATASOURCE_PERSONAL,
                                Strings.KEY_TAG_COMPANY_TITLE),
                        Strings.STR_DELIM_COLONDWWS,
                        itemEntity.getPageData().getTitle()));
            } else {
                error(403, request, response);
            }
        } else {
            if (tag != null && tag.trim().length() != 0) {
                events.add(serviceProvider.getItemService().getAllByTag(tag).stream()
                        .filter(entity -> EntityUtils
                                .isAccessible(entity, AccessType.ACCESS_READ, accessor))
                        .map(PageEventConverter::toEvent).sorted(pageEventComparator)
                        .collect(Collectors.toList()));
                model.addAttribute("htmlTitle", String.format("%s%s by tag %s",
                        settings.getValue(Strings.CONTEXT_DATASOURCE_PERSONAL,
                                Strings.KEY_TAG_COMPANY_TITLE),
                        Strings.STR_DELIM_COLONDWWS,
                        tag.trim()));
            } else {
                List<CategoryEntity> activeCategories = serviceProvider.getCategoryService()
                        .getAllActive();
                if (activeCategories.size() != 2) {
                    events.add(new ArrayList<>(serviceProvider.getItemService()
                            .getAllByCategoriesAndAccessibility(activeCategories, accessor)
                            .stream()
                            .map(PageEventConverter::toEvent)
                            .sorted(pageEventComparator)
                            .collect(Collectors.toList())));
                } else {
                    for (int i = 0; i < activeCategories.size(); i++) {
                        if (PagesAttribute.getByOrdinal(i) != null) {
                            List<PageEvent> pageEvents = serviceProvider.getItemService()
                                    .getAllByCategoryAndAccessibility(activeCategories.get(i), accessor)
                                    .stream()
                                    .filter(itemEntity -> ItemEntity.ItemType.PAGE.equals(itemEntity.getItemType()))
                                    .map(PageEventConverter::toEvent)
                                    .sorted(pageEventComparator)
                                    .collect(Collectors.toList());
                            events.add(pageEvents);
                        }
                    }
                }
            }
        }
        model.addAttribute("pages", events);


    }

    //@GetMapping(value = "/*/**")
    //public void redirectToIndex(HttpServletRequest request, HttpServletResponse response)
    //        throws IOException {
    //    LOGGER.error("REDIRECT");
    //    error(404, request, response); // cause redirects after tomcat10 migration
    //}
}
