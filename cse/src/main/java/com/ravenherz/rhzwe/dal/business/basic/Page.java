package com.ravenherz.rhzwe.dal.business.basic;

import com.ravenherz.rhzwe.dal.dto.basic.CategoryData;
import com.ravenherz.rhzwe.dal.dto.basic.PageData;
import com.ravenherz.rhzwe.dal.dto.basic.ResourceData;

@Deprecated
public class Page {

    private ResourceData resourceData;
    private CategoryData categoryData;
    private PageData pageData;

    public Page(ResourceData resourceData, CategoryData categoryData, PageData pageData) {
        this.resourceData = resourceData;
        this.categoryData = categoryData;
        this.pageData = pageData;
    }

    public ResourceData getResourceData() {
        return resourceData;
    }

    public CategoryData getCategoryData() {
        return categoryData;
    }

    public PageData getPageData() {
        return pageData;
    }
}
