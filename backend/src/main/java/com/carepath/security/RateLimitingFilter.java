package com.carepath.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int authLimitPerMinute;
    private final int mlLimitPerMinute;
    private final int defaultLimitPerMinute;

    private final ConcurrentHashMap<String, WindowCounter> requestCounts = new ConcurrentHashMap<>();

    public RateLimitingFilter(
            ObjectMapper objectMapper,
            @Value("${app.security.rate-limiting.enabled:true}") boolean enabled,
            @Value("${app.security.rate-limiting.auth-limit-per-minute:30}") int authLimitPerMinute,
            @Value("${app.security.rate-limiting.ml-limit-per-minute:60}") int mlLimitPerMinute,
            @Value("${app.security.rate-limiting.default-limit-per-minute:120}") int defaultLimitPerMinute) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.authLimitPerMinute = authLimitPerMinute;
        this.mlLimitPerMinute = mlLimitPerMinute;
        this.defaultLimitPerMinute = defaultLimitPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();

        // Skip rate limiting on static resources or health checks
        if (uri.startsWith("/actuator") || uri.startsWith("/error")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        int limit = resolveLimitForUri(uri);
        String bucketKey = clientIp + ":" + getBucketCategory(uri);

        long currentMinute = Instant.now().getEpochSecond() / 60;

        WindowCounter counter = requestCounts.compute(bucketKey, (k, existing) -> {
            if (existing == null || existing.minute != currentMinute) {
                return new WindowCounter(currentMinute, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (counter.count.get() > limit) {
            log.warn("[RATE_LIMIT_EXCEEDED] Client IP {} exceeded limit of {} req/min for URI: {}", clientIp, limit, uri);
            writeRateLimitResponse(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int resolveLimitForUri(String uri) {
        if (uri.contains("/auth/login") || uri.contains("/auth/register") || uri.contains("/auth/refresh")) {
            return authLimitPerMinute;
        }
        if (uri.startsWith("/ml/") || uri.contains("/risk-assessments")) {
            return mlLimitPerMinute;
        }
        return defaultLimitPerMinute;
    }

    private String getBucketCategory(String uri) {
        if (uri.contains("/auth/")) return "AUTH";
        if (uri.startsWith("/ml/") || uri.contains("/risk-assessments")) return "ML";
        return "GENERAL";
    }

    private void writeRateLimitResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");

        Map<String, Object> errorBody = Map.of(
                "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                "error", "TOO_MANY_REQUESTS",
                "message", "Rate limit exceeded. Please retry after some time.",
                "path", request.getRequestURI(),
                "traceId", UUID.randomUUID().toString()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    public void clearCache() {
        requestCounts.clear();
    }

    private static class WindowCounter {
        final long minute;
        final AtomicInteger count;

        WindowCounter(long minute, AtomicInteger count) {
            this.minute = minute;
            this.count = count;
        }
    }
}
