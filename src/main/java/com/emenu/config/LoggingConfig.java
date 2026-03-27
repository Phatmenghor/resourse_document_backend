package com.emenu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Logging configuration for the application.
 * - Enables detailed HTTP request/response logging
 * - Adds request tracing with correlation IDs
 */
@Configuration
@Slf4j
public class LoggingConfig {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_LOG_VAR = "correlationId";

    /**
     * Request logging filter for HTTP requests
     */
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        loggingFilter.setMaxPayloadLength(10000);
        loggingFilter.setIncludeHeaders(true);
        loggingFilter.setAfterMessagePrefix("REQUEST DATA : ");
        return loggingFilter;
    }

    /**
     * Correlation ID filter for request tracing
     */
    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    /**
     * Filter that adds correlation ID to all requests for distributed tracing
     */
    @Slf4j
    public static class CorrelationIdFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                       FilterChain filterChain) throws ServletException, IOException {
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);

            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }

            org.slf4j.MDC.put(CORRELATION_ID_LOG_VAR, correlationId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            try {
                log.debug("[CORRELATION-ID] Request: {} {} - Correlation: {}",
                    request.getMethod(), request.getRequestURI(), correlationId);
                filterChain.doFilter(request, response);
            } finally {
                org.slf4j.MDC.remove(CORRELATION_ID_LOG_VAR);
            }
        }
    }
}
