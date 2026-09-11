package com.ravenherz.cse.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ConcurrentModel;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EditorChromeAdviceTest {

    @Test
    void catalogStaysActiveOnContentRoutes() {
        EditorChromeAdvice advice = new EditorChromeAdvice();

        ConcurrentModel page = new ConcurrentModel();
        advice.editorNav(request("/editor/edit"), page);
        assertEquals(Boolean.TRUE, page.getAttribute("navResources"));
        assertEquals(Boolean.FALSE, page.getAttribute("navAccounts"));

        ConcurrentModel category = new ConcurrentModel();
        advice.editorNav(request("/editor/category/edit"), category);
        assertEquals(Boolean.TRUE, category.getAttribute("navResources"));

        ConcurrentModel create = new ConcurrentModel();
        advice.editorNav(request("/editor/create"), create);
        assertEquals(Boolean.TRUE, create.getAttribute("navResources"));

        ConcurrentModel playlist = new ConcurrentModel();
        advice.editorNav(request("/editor/playlist/create"), playlist);
        assertEquals(Boolean.TRUE, playlist.getAttribute("navResources"));

        ConcurrentModel leftoverPages = new ConcurrentModel();
        advice.editorNav(request("/editor/pages"), leftoverPages);
        assertEquals(Boolean.TRUE, leftoverPages.getAttribute("navResources"));

        ConcurrentModel leftoverCategories = new ConcurrentModel();
        advice.editorNav(request("/editor/categories"), leftoverCategories);
        assertEquals(Boolean.TRUE, leftoverCategories.getAttribute("navResources"));

        ConcurrentModel settings = new ConcurrentModel();
        advice.editorNav(request("/editor/settings"), settings);
        assertEquals(Boolean.FALSE, settings.getAttribute("navResources"));
        assertEquals(Boolean.TRUE, settings.getAttribute("navSettings"));

        ConcurrentModel transcode = new ConcurrentModel();
        advice.editorNav(request("/editor/transcode"), transcode);
        assertEquals(Boolean.FALSE, transcode.getAttribute("navResources"));
        assertEquals(Boolean.TRUE, transcode.getAttribute("navTranscode"));
        assertEquals(Boolean.FALSE, transcode.getAttribute("navInstance"));

        ConcurrentModel instance = new ConcurrentModel();
        advice.editorNav(request("/editor/instance"), instance);
        assertEquals(Boolean.FALSE, instance.getAttribute("navResources"));
        assertEquals(Boolean.TRUE, instance.getAttribute("navInstance"));
        assertEquals(Boolean.FALSE, instance.getAttribute("navLogs"));
        assertEquals(Boolean.FALSE, instance.getAttribute("navTranscode"));
    }

    private static MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }
}
