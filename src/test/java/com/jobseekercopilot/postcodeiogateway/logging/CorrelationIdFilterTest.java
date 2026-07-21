package com.jobseekercopilot.postcodeiogateway.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class CorrelationIdFilterTest {

    @Test
    void redactsPostcodePathValues() {
        assertEquals("/api/postcodes/{postcode}",
                CorrelationIdFilter.safePath("/api/postcodes/LS1%201UR"));
    }

    @Test
    void preservesNonSensitiveOperationalPaths() {
        assertEquals("/actuator/health", CorrelationIdFilter.safePath("/actuator/health"));
    }

    @Test
    void preservesSafeInboundCorrelationIds() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter("test-service");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "request_123:child-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertEquals("request_123:child-1", response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertEquals("request_123:child-1", request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE));
    }

    @Test
    void replacesUnsafeInboundCorrelationIds() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter("test-service");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "unsafe\nlog-entry");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        String generated = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertNotEquals("unsafe\nlog-entry", generated);
        assertTrue(generated.matches("[0-9a-f-]{36}"));
        assertEquals(generated, request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE));
    }
}
