package com.carepath.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Should preserve valid client-supplied X-Request-ID header")
    void testPreserveClientCorrelationId() throws ServletException, IOException {
        String customId = "req-" + UUID.randomUUID();
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(customId);

        filter.doFilter(request, response, filterChain);

        verify(response).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, customId);
        verify(filterChain).doFilter(request, response);
        // MDC must be cleaned up in finally block
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Should generate UUID correlation ID when header is missing")
    void testGenerateCorrelationIdWhenMissing() throws ServletException, IOException {
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(null);
        when(request.getHeader(CorrelationIdFilter.ALT_CORRELATION_ID_HEADER)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), captor.capture());
        assertThat(captor.getValue()).isNotBlank();
        // Should parse as valid UUID
        assertThat(UUID.fromString(captor.getValue())).isNotNull();
        verify(filterChain).doFilter(request, response);
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Should accept alternate X-Correlation-ID header if X-Request-ID is absent")
    void testAcceptAlternateHeader() throws ServletException, IOException {
        String altId = "alt-" + UUID.randomUUID();
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(null);
        when(request.getHeader(CorrelationIdFilter.ALT_CORRELATION_ID_HEADER)).thenReturn(altId);

        filter.doFilter(request, response, filterChain);

        verify(response).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, altId);
        verify(filterChain).doFilter(request, response);
    }
}
