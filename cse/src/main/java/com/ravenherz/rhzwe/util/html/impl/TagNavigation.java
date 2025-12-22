package com.ravenherz.rhzwe.util.html.impl;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.dal.business.basic.Navigation;
import com.ravenherz.rhzwe.dal.business.basic.NavigationUnit;
import com.ravenherz.rhzwe.dal.business.basic.NavigationUnit.NavigationUnitType;
import com.ravenherz.rhzwe.dal.dto.AccountEntity;
import com.ravenherz.rhzwe.util.Settings;
import com.ravenherz.rhzwe.util.html.AccessToDealerProviderTag;
import com.ravenherz.rhzwe.util.html.CustomAccessHtmlTag;
import com.ravenherz.rhzwe.util.html.HTMLElement;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Component
public final class TagNavigation
        extends AccessToDealerProviderTag
        implements CustomAccessHtmlTag {

    private boolean columns = true;
    private final Map<String, String> typeToLinkTypeMapping = new HashMap<>();

    {
        typeToLinkTypeMapping.put(NavigationUnit.NavigationUnitType.PAGE.name(), "./?page=");
        typeToLinkTypeMapping.put(NavigationUnit.NavigationUnitType.ALBUM.name(), "./?album=");
    }

    public TagNavigation() {
        super();
    }

    public TagNavigation(Boolean vertical) {
        columns = vertical;
    }

    @Override
    public HTMLElement getHtmlElement(AccountEntity accountEntity) {

        int numOfCols = 0;
        try {
            numOfCols = Integer.parseInt(settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW,
                    Strings.KEY_NAV_FOOTER_NUM_OF_COLS));
            if (numOfCols > 6) {
                numOfCols = 6;
            }
            if (numOfCols < 2) {
                numOfCols = 2;
            }
        } catch (NumberFormatException ex) {
            numOfCols = Settings.DEFAULT_NUMBER_OF_COLUMS_NAV_FOOTER;
        }

        ArrayList<String> readyListsAsHtmlString = new ArrayList<>();
        StringBuilder categoriesString = new StringBuilder(Strings.STR_EMPTY);
        Navigation navigation = dealerProvider.getNavigationDealer().getNavigation(accountEntity);
        List<NavigationUnit> tree = navigation.getTree();
        if (tree.size() == 0) {
            return new HTMLElement();
        }
        if (!columns) {
            readyListsAsHtmlString.add(makePageHtml(
                    new NavigationUnit(
                            NavigationUnitType.PAGE,
                            null, "Products overview", "Products overview", new LinkedList<>(), LocalDateTime.now())
            ).getCode());
        }

        tree.stream()
                .sorted(Comparator.comparingLong(o -> o.getDateTimeCreated().toEpochSecond(ZoneOffset.UTC)))
                .forEachOrdered(e -> {
                    switch (e.getType()) {
                        case CATEGORY -> readyListsAsHtmlString.add(makeCategoryHtml(e).getCode());
                        case PAGE -> readyListsAsHtmlString.add(makePageHtml(e).getCode());
                        default -> {
                        }
                    }
                });

        if (columns) {
            ArrayList<String> cols = new ArrayList<>();
            for (int i = 0; i < Math.min(readyListsAsHtmlString.size(), numOfCols); i++) {
                cols.add("");
            }
            for (int i = 0; i < readyListsAsHtmlString.size(); i++) {
                cols.set(i % numOfCols,
                        cols.get(i % numOfCols) == null ? readyListsAsHtmlString.get(i)
                                : cols.get(i % numOfCols) + readyListsAsHtmlString.get(i));
            }
            for (String col : cols
            ) {
                HTMLElement colElem = new HTMLElement(Strings.ELEM_NAME_DIV, col);
                switch (numOfCols) {
                    case 2 -> colElem.addAttribute(Strings.ATTR_NAME_CLASS,
                            Strings.ATTR_VALUE_CLASS_BOOTSTRAP_COL_SM_6);
                    case 3 -> colElem.addAttribute(Strings.ATTR_NAME_CLASS,
                            Strings.ATTR_VALUE_CLASS_BOOTSTRAP_COL_SM_4);
                    case 4 -> colElem.addAttribute(Strings.ATTR_NAME_CLASS,
                            Strings.ATTR_VALUE_CLASS_BOOTSTRAP_COL_SM_3);
                    default -> colElem.addAttribute(Strings.ATTR_NAME_CLASS,
                            Strings.ATTR_VALUE_CLASS_BOOTSTRAP_COL_SM_2);
                }

                categoriesString.append(colElem.getCode());
            }
        } else {
            StringBuilder singleColumn = new StringBuilder(Strings.STR_EMPTY);
            for (String item : readyListsAsHtmlString
            ) {

                singleColumn.append(item);
            }
            HTMLElement colElem = new HTMLElement(Strings.ELEM_NAME_DIV, singleColumn.toString());
            colElem.addAttribute(Strings.ATTR_NAME_CLASS,
                    Strings.ATTR_VALUE_CLASS_BOOTSTRAP_COL_SM_12);
            categoriesString.append(colElem.getCode());
        }
        HTMLElement div = new HTMLElement(Strings.ELEM_NAME_DIV, categoriesString.toString());
        div.addAttribute(Strings.ATTR_NAME_CLASS, Strings.ATTR_VALUE_CLASS_NAVIGATION);
        return div;
    }

    private HTMLElement makePageHtml(NavigationUnit navigationUnit) {
        HTMLElement ref = new HTMLElement(Strings.ELEM_NAME_A, navigationUnit.getTitle());
        ref.addAttribute(Strings.ATTR_NAME_CLASS, Strings.ATTR_VALUE_CLASS_TRANSITION);

        if (navigationUnit.getName() != null) {
            ref.addAttribute(Strings.ATTR_NAME_HREF, typeToLinkTypeMapping.get(
                    navigationUnit.getType().name()) + navigationUnit.getName());
            ref.addAttribute(Strings.ATTR_NAME_TITLE, navigationUnit.getDescription());
            return new HTMLElement(Strings.ELEM_NAME_LI, ref.getCode());
        } else {
            ref.addAttribute(Strings.ATTR_NAME_HREF, "./");
            ref.addAttribute(Strings.ATTR_NAME_TITLE, navigationUnit.getDescription());
            return new HTMLElement(Strings.ELEM_NAME_A, ref.getCode() + "<br><br>");
        }

    }

    private HTMLElement makeCategoryHtml(NavigationUnit navigationUnit) {
        LinkedList<String> pages = new LinkedList<>();
        for (NavigationUnit page : navigationUnit.getChildren()) {
            pages.add(makePageHtml(page).getCode());
        }
        HTMLElement category = new HTMLElement(Strings.ELEM_NAME_DIV,
                navigationUnit.getTitle() + new HTMLElement(Strings.ELEM_NAME_UL,
                        String.join(Strings.STR_EMPTY, pages)).getCode());
        category.addAttribute(Strings.ATTR_NAME_TITLE, navigationUnit.getDescription());

        return category;
    }
}
