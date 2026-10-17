package com.utown.utownbackend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestCorrelationFilterTest {

    private RequestCorrelationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RequestCorrelationFilter();
        MDC.clear();
    }

    @Test
    void shouldGenerateRequestIdWhenMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        String requestId = response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);
        assertThat(requestId).isNotNull();
        assertThat(UUID.fromString(requestId)).isNotNull(); // Validates UUID format
        assertThat(MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID_KEY)).isNull(); // Cleared after chain
    }

    @Test
    void shouldPreserveValidIncomingRequestId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String validUuid = UUID.randomUUID().toString();
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, validUuid);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        String requestId = response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);
        assertThat(requestId).isEqualTo(validUuid);
    }

    @Test
    void shouldGenerateNewIdWhenIncomingIsInvalidUuid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "invalid-uuid-format");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        String requestId = response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);
        assertThat(requestId).isNotNull();
        assertThat(requestId).isNotEqualTo("invalid-uuid-format");
        assertThat(UUID.fromString(requestId)).isNotNull();
    }

    @Test
    void shouldMakeIdAvailableInMdcDuringFilterChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] mdcValueDuringChain = new String[1];

        FilterChain filterChain = (req, res) -> {
            mdcValueDuringChain[0] = MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID_KEY);
        };

        filter.doFilter(request, response, filterChain);

        assertThat(mdcValueDuringChain[0]).isNotNull();
        assertThat(mdcValueDuringChain[0]).isEqualTo(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER));
        assertThat(MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID_KEY)).isNull();
    }

    @Test
    void shouldCleanupMdcAfterException() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (req, res) -> {
            throw new RuntimeException("Test exception");
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test exception");

        assertThat(MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID_KEY)).isNull();
    }
}
