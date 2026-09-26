package com.ravenherz.cse.web;

import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.redirect.ResourceRedirectEntity;
import com.ravenherz.cse.redirect.ResourceRedirectService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedirectFilterTest {

    private ResourceRedirectService store;
    private RedirectFilter filter;

    @BeforeEach
    void setup() {
        store = mock(ResourceRedirectService.class);
        ServiceProvider services = mock(ServiceProvider.class);
        when(services.getResourceRedirectService()).thenReturn(store);
        filter = new RedirectFilter(services);
    }

    @Test
    void hitSetsPermanentLocation() throws Exception {
        when(store.getAll()).thenReturn(List.of(row("/old-post", "/?page=new-post", 301, true)));
        MockHttpServletRequest request = request("", "/old-post");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(301, response.getStatus());
        assertEquals("/?page=new-post", response.getHeader("Location"));
        verify(chain, org.mockito.Mockito.never()).doFilter(request, response);
    }

    @Test
    void temporaryHitKeepsContextAndQuery() throws Exception {
        when(store.getAll()).thenReturn(List.of(row("/old", "/next", 302, true)));
        MockHttpServletRequest request = request("/other-context", "/other-context/old");
        request.setQueryString("utm=1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertEquals(302, response.getStatus());
        assertEquals("/other-context/next?utm=1", response.getHeader("Location"));
    }

    @Test
    void queryOnTheTargetUsesAmpersand() throws Exception {
        when(store.getAll()).thenReturn(List.of(row("/old", "/?page=new-post", 301, true)));
        MockHttpServletRequest request = request("", "/old");
        request.setQueryString("utm=1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertEquals("/?page=new-post&utm=1", response.getHeader("Location"));
    }

    @Test
    void pageQueryRedirectsBeforeTheIndex() throws Exception {
        when(store.getAll()).thenReturn(List.of(
                new ResourceRedirectEntity("/?page=rhz-we", "/?page=cse", null, true, 301, false, null)));
        MockHttpServletRequest request = request("", "/");
        request.setQueryString("page=rhz-we");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(301, response.getStatus());
        assertEquals("/?page=cse", response.getHeader("Location"));
        verify(chain, org.mockito.Mockito.never()).doFilter(request, response);
    }

    @Test
    void missFallsThrough() throws Exception {
        when(store.getAll()).thenReturn(List.of());
        MockHttpServletRequest request = request("", "/unknown");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertNull(response.getHeader("Location"));
        verify(chain).doFilter(request, response);
    }

    @Test
    void storedPrefixesHit() throws Exception {
        when(store.getAll()).thenReturn(List.of(
                row("/rhz-we/*", "/*", 301, true),
                row("/static-pages/*", "/apps/*", 301, true)));
        MockHttpServletRequest legacy = request("", "/rhz-we/editor");
        MockHttpServletResponse legacyResponse = new MockHttpServletResponse();
        filter.doFilter(legacy, legacyResponse, mock(FilterChain.class));
        assertEquals("/editor", legacyResponse.getHeader("Location"));

        MockHttpServletRequest pages = request("", "/static-pages/login/");
        MockHttpServletResponse pagesResponse = new MockHttpServletResponse();
        filter.doFilter(pages, pagesResponse, mock(FilterChain.class));
        assertEquals("/apps/login", pagesResponse.getHeader("Location"));
    }

    @Test
    void reservedFallsThrough() throws Exception {
        when(store.getAll()).thenReturn(List.of(row("/editor/roles", "/?page=x", 301, true)));
        MockHttpServletRequest request = request("", "/editor/roles");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertNull(response.getHeader("Location"));
        verify(chain).doFilter(request, response);
    }

    private static MockHttpServletRequest request(String context, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath(context);
        request.setRequestURI(uri);
        return request;
    }

    private static BasicEntity row(String from, String target, int status, boolean enabled) {
        return new ResourceRedirectEntity(from, target, null, enabled, status, true, null);
    }
}
