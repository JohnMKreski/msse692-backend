package com.arkvalleyevents.msse692_backend.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that ensures every request has a correlation ID.
 * - Reuses client-provided X-Request-ID if present (sanitized)
 * - Otherwise generates a UUID
 * - Adds the ID to MDC and the response header X-Request-ID
 * - Also sets MDC 'user' from authenticated principal name when available
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Request-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String requestId = sanitize(request.getHeader(HEADER));
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }
            MDC.put("requestId", requestId);
            if (request.getUserPrincipal() != null) {
                MDC.put("user", request.getUserPrincipal().getName());
            }
            // Always echo back the correlation ID
            response.setHeader(HEADER, requestId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    // Keep IDs simple and bounded; return null if not acceptable
    private String sanitize(String incoming) {
        if (incoming == null) return null;
        String trimmed = incoming.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() > 128) {
            return null; // too long
        }
        // Allow common safe characters
        // Hyphen placed at end so it doesn't define a range; escape sequence corrected.
        if (!trimmed.matches("[A-Za-z0-9._-]+")) {
            return null;
        }
        return trimmed;
    }
}
