package com.taskflow.task_management_api.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    private final RateLimitFilter filter = new RateLimitFilter();

    private static MockHttpServletRequest registerRequest(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        request.setRemoteAddr(ip);
        return request;
    }

    @Test
    void blocksRegisterAfterFiveAttemptsFromSameClient() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockFilterChain chain = new MockFilterChain();
            filter.doFilter(registerRequest("10.0.0.1"), new MockHttpServletResponse(), chain);
            assertNotNull(chain.getRequest(), "la requête doit atteindre la suite de la chaîne");
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(registerRequest("10.0.0.1"), blocked, new MockFilterChain());

        assertEquals(429, blocked.getStatus());
        assertTrue(blocked.getContentAsString().contains("Trop de tentatives"));
    }

    @Test
    void countersArePerClient() throws Exception {
        for (int i = 0; i < 5; i++) {
            filter.doFilter(registerRequest("10.0.0.1"), new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletResponse other = new MockHttpServletResponse();
        filter.doFilter(registerRequest("10.0.0.2"), other, new MockFilterChain());

        assertEquals(200, other.getStatus());
    }

    @Test
    void otherRoutesAreNotLimited() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/workspaces");
        request.setRemoteAddr("10.0.0.1");

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest());
    }
}
